package com.cmacfarl.sensei.llm;

/** One message of a conversation as sent to a provider. */
public final class LlmMessage {

    public enum Role { USER, MODEL }

    public final Role role;
    public final String text;
    /**
     * Provider-specific data to replay instead of {@link #text}, or null. Gemini uses it to send
     * back the exact response parts, thought signatures included.
     */
    public final String providerPayload;

    public LlmMessage(Role role, String text, String providerPayload) {
        this.role = role;
        this.text = text;
        this.providerPayload = providerPayload;
    }

    public static LlmMessage user(String text) {
        return new LlmMessage(Role.USER, text, null);
    }

    public static LlmMessage model(String text, String providerPayload) {
        return new LlmMessage(Role.MODEL, text, providerPayload);
    }
}
