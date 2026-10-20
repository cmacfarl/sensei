package com.cmacfarl.sensei.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.checkbox.MaterialCheckBox;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cmacfarl.sensei.R;
import com.cmacfarl.sensei.data.KnownKanji;
import com.cmacfarl.sensei.data.Settings;
import com.cmacfarl.sensei.text.JapaneseChars;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ChatAdapter.Actions {

    private MainViewModel viewModel;
    private Settings settings;
    private KnownKanji known;
    private ChatAdapter adapter;
    private LinearLayoutManager layoutManager;
    private List<ChatItem> shownItems;
    private MaterialToolbar toolbar;
    private RecyclerView list;
    private EditText input;
    private View send;
    private TextView empty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = new Settings(this);
        known = KnownKanji.get(this);
        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        list = findViewById(R.id.list);
        input = findViewById(R.id.input);
        send = findViewById(R.id.send);
        empty = findViewById(R.id.empty);

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });

        adapter = new ChatAdapter(() -> new RubyRenderer.Options(settings.showFurigana(), known, null, 0), this);
        layoutManager = new LinearLayoutManager(this);
        list.setLayoutManager(layoutManager);
        list.setAdapter(adapter);

        send.setOnClickListener(v -> submit());
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit();
                return true;
            }
            return false;
        });

        viewModel.items().observe(this, items -> {
            int previousLastType = lastType(shownItems);
            int previousCount = shownItems == null ? -1 : shownItems.size();
            shownItems = items;
            adapter.setItems(items);
            empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
            updateChrome();

            if (previousLastType == ChatItem.LOADING && lastType(items) == ChatItem.TUTOR) {
                showNewReply(items);
            } else if (previousCount >= 0 && items.size() > previousCount) {
                list.scrollToPosition(items.size() - 1);
            }
            // Otherwise it's a refresh (furigana toggle, known kanji, returning from Settings): stay put.
        });
        viewModel.busy().observe(this, busy -> {
            send.setEnabled(!busy);
            invalidateOptionsMenu();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Settings or known kanji may have changed while another screen was open.
        viewModel.refresh();
    }

    /** Hide the keyboard and put the student's last message at the top, so the reply reads from its start. */
    private void showNewReply(List<ChatItem> items) {
        input.clearFocus();
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), input);
        controller.hide(WindowInsetsCompat.Type.ime());

        int lastStudent = items.size() - 1;
        while (lastStudent > 0 && items.get(lastStudent).type != ChatItem.STUDENT) lastStudent--;
        layoutManager.scrollToPositionWithOffset(lastStudent, 0);
    }

    private static int lastType(List<ChatItem> items) {
        return items == null || items.isEmpty() ? -1 : items.get(items.size() - 1).type;
    }

    private void submit() {
        String text = input.getText().toString();
        if (text.trim().isEmpty() || Boolean.TRUE.equals(viewModel.busy().getValue())) return;
        viewModel.send(text);
        input.setText("");
    }

    private void updateChrome() {
        toolbar.setSubtitle(viewModel.providerLabel());
        input.setHint(viewModel.hasThread() ? R.string.hint_follow_up : R.string.hint_new_sentence);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean show = settings.showFurigana();
        MenuItem furigana = menu.findItem(R.id.action_furigana);
        furigana.setIcon(show ? R.drawable.ic_furigana_on : R.drawable.ic_furigana_off);
        furigana.setTitle(show ? R.string.action_furigana_hide : R.string.action_furigana_show);
        menu.findItem(R.id.action_new).setEnabled(!Boolean.TRUE.equals(viewModel.busy().getValue()));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            viewModel.newSentence();
            input.setText("");
            input.requestFocus();
            return true;
        } else if (id == R.id.action_furigana) {
            settings.setShowFurigana(!settings.showFurigana());
            invalidateOptionsMenu();
            viewModel.refresh();
            return true;
        } else if (id == R.id.action_known_kanji) {
            startActivity(new Intent(this, KnownKanjiActivity.class));
            return true;
        } else if (id == R.id.action_settings) {
            openSettings();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- ChatAdapter.Actions ---

    @Override
    public void retry() {
        viewModel.retry();
    }

    @Override
    public void openSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    /** Lets the student tick which kanji of a word they know, or look the word up on jisho.org. */
    @Override
    public void kanjiWordLongPressed(String word, String lookup) {
        List<Integer> kanji = new ArrayList<>();
        for (int i = 0; i < word.length(); ) {
            int c = word.codePointAt(i);
            if (JapaneseChars.isKanji(c) && !kanji.contains(c)) kanji.add(c);
            i += Character.charCount(c);
        }
        if (kanji.isEmpty()) return;

        View body = getLayoutInflater().inflate(R.layout.dialog_kanji_word, null);
        LinearLayout checks = body.findViewById(R.id.checks);
        List<MaterialCheckBox> boxes = new ArrayList<>();
        for (int c : kanji) {
            MaterialCheckBox box = new MaterialCheckBox(this);
            box.setText(new String(Character.toChars(c)));
            box.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            box.setChecked(known.isKnown(c));
            checks.addView(box);
            boxes.add(box);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.mark_known_title, word))
                .setView(body)
                .create();
        // Closes the dialog (like Cancel) so Back from the browser returns straight to the reply.
        body.findViewById(R.id.jisho).setOnClickListener(v -> {
            dialog.dismiss();
            openJisho(lookup);
        });
        body.findViewById(R.id.cancel).setOnClickListener(v -> dialog.dismiss());
        body.findViewById(R.id.ok).setOnClickListener(v -> {
            for (int i = 0; i < kanji.size(); i++) known.setKnown(kanji.get(i), boxes.get(i).isChecked());
            viewModel.refresh();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void openJisho(String query) {
        Uri uri = Uri.parse("https://jisho.org/search/" + Uri.encode(query));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_SHORT).show();
        }
    }
}
