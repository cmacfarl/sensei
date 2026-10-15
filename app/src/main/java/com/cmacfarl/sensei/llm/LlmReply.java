package com.cmacfarl.sensei.llm;

/** The model's reply: the JSON text plus anything the provider needs replayed next turn. */
public final class LlmReply {

    public final String text;
    public final String providerPayload;

    public LlmReply(String text, String providerPayload) {
        this.text = text;
        this.providerPayload = providerPayload;
    }
}
