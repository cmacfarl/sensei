package com.cmacfarl.sensei.data;

import com.cmacfarl.sensei.tutor.Conversation;

/** Keeps the thread for the life of the process; it's gone when Android kills the app. */
public final class InMemoryConversationStore implements ConversationStore {

    private Conversation current;

    @Override
    public Conversation current() {
        return current;
    }

    @Override
    public void setCurrent(Conversation conversation) {
        current = conversation;
    }

    @Override
    public void saved(Conversation conversation) {
        // Nothing to persist.
    }
}
