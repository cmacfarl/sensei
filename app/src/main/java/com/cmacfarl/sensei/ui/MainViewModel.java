package com.cmacfarl.sensei.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cmacfarl.sensei.data.ConversationStore;
import com.cmacfarl.sensei.data.InMemoryConversationStore;
import com.cmacfarl.sensei.data.Settings;
import com.cmacfarl.sensei.llm.LlmException;
import com.cmacfarl.sensei.tutor.Conversation;
import com.cmacfarl.sensei.tutor.Turn;
import com.cmacfarl.sensei.tutor.Tutor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Holds the current thread across configuration changes and runs requests off the main thread.
 * A student message only joins the conversation once the tutor has answered it, so a failed
 * request can be retried without leaving a dangling turn in the history.
 */
public final class MainViewModel extends AndroidViewModel {

    private final Settings settings;
    private final ConversationStore store = new InMemoryConversationStore();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<ChatItem>> items = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    private String pendingText;
    private LlmException lastError;

    public MainViewModel(@NonNull Application app) {
        super(app);
        settings = new Settings(app);
    }

    LiveData<List<ChatItem>> items() {
        return items;
    }

    LiveData<Boolean> busy() {
        return busy;
    }

    boolean hasThread() {
        return store.current() != null;
    }

    /** "provider · model" for the current thread, or for the next one if none is open. */
    String providerLabel() {
        Conversation c = store.current();
        String provider = c != null ? c.provider : settings.provider();
        String model = c != null ? c.model : settings.modelFor(provider);
        String name = Settings.PROVIDER_CLAUDE.equals(provider) ? "Claude" : "Gemini";
        return name + " · " + model;
    }

    void send(String text) {
        if (Boolean.TRUE.equals(busy.getValue()) || text.trim().isEmpty()) return;
        pendingText = text.trim();
        lastError = null;
        busy.setValue(true);
        publish();

        Conversation existing = store.current();
        String typed = pendingText;
        executor.execute(() -> {
            Conversation conversation = existing;
            if (conversation == null) {
                String provider = settings.provider();
                conversation = new Conversation(provider, settings.modelFor(provider));
            }
            try {
                Tutor tutor = new Tutor(settings.clientFor(conversation.provider, conversation.model));
                Tutor.Exchange exchange = tutor.ask(conversation, typed);
                Conversation done = conversation;
                runOnMain(() -> onAnswered(done, exchange));
            } catch (LlmException e) {
                runOnMain(() -> onFailed(e));
            } catch (RuntimeException e) {
                runOnMain(() -> onFailed(new LlmException("Something went wrong: " + e, true, e)));
            }
        });
    }

    void retry() {
        if (pendingText != null) send(pendingText);
    }

    /** Ends the current thread; the next message starts a fresh one. */
    void newSentence() {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        store.setCurrent(null);
        pendingText = null;
        lastError = null;
        publish();
    }

    /** Re-emit the list, e.g. after furigana or known-kanji changes. */
    void refresh() {
        publish();
    }

    private void onAnswered(Conversation conversation, Tutor.Exchange exchange) {
        conversation.append(exchange.student, exchange.tutor);
        store.setCurrent(conversation);
        store.saved(conversation);
        pendingText = null;
        busy.setValue(false);
        publish();
    }

    private void onFailed(LlmException e) {
        lastError = e;
        busy.setValue(false);
        publish();
    }

    private void publish() {
        List<ChatItem> list = new ArrayList<>();
        Conversation c = store.current();
        if (c != null) {
            for (Turn t : c.turns()) {
                list.add(t.role == Turn.Role.STUDENT ? ChatItem.student(t.text) : ChatItem.tutor(t));
            }
        }
        if (pendingText != null) list.add(ChatItem.student(pendingText));
        if (Boolean.TRUE.equals(busy.getValue())) list.add(ChatItem.loading());
        if (lastError != null) list.add(ChatItem.error(lastError.getMessage(), lastError.isRetryable()));
        items.setValue(list);
    }

    private static void runOnMain(Runnable r) {
        new android.os.Handler(android.os.Looper.getMainLooper()).post(r);
    }

    @Override
    protected void onCleared() {
        executor.shutdownNow();
    }
}
