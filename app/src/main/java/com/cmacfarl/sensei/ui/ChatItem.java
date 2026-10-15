package com.cmacfarl.sensei.ui;

import com.cmacfarl.sensei.tutor.Turn;

/** One row in the chat list. */
final class ChatItem {

    static final int STUDENT = 0;
    static final int TUTOR = 1;
    static final int LOADING = 2;
    static final int ERROR = 3;

    final int type;
    final String text;     // STUDENT: what they typed; ERROR: the message
    final Turn turn;       // TUTOR
    final boolean retryable;

    private ChatItem(int type, String text, Turn turn, boolean retryable) {
        this.type = type;
        this.text = text;
        this.turn = turn;
        this.retryable = retryable;
    }

    static ChatItem student(String text) {
        return new ChatItem(STUDENT, text, null, false);
    }

    static ChatItem tutor(Turn turn) {
        return new ChatItem(TUTOR, null, turn, false);
    }

    static ChatItem loading() {
        return new ChatItem(LOADING, null, null, false);
    }

    static ChatItem error(String message, boolean retryable) {
        return new ChatItem(ERROR, message, null, retryable);
    }
}
