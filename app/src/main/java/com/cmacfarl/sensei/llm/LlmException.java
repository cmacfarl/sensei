package com.cmacfarl.sensei.llm;

/** A request failed; the message is meant to be shown to the user. */
public class LlmException extends Exception {

    private final boolean retryable;

    public LlmException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public LlmException(String message, boolean retryable, Throwable cause) {
        super(message, cause);
        this.retryable = retryable;
    }

    /** False for problems retrying won't fix, like a missing API key. */
    public boolean isRetryable() {
        return retryable;
    }
}
