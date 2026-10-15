package com.cmacfarl.sensei.data;

import com.cmacfarl.sensei.tutor.Conversation;

/**
 * Where the current thread lives. Today it's memory only; a Room- or file-backed implementation
 * can be swapped in to add history without touching the UI.
 */
public interface ConversationStore {

    /** The current thread, or null if none has been started. */
    Conversation current();

    void setCurrent(Conversation conversation);

    /** Called after a thread changes, so persistent stores can save it. */
    void saved(Conversation conversation);
}
