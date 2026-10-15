package com.cmacfarl.sensei.tutor;

import java.util.ArrayList;
import java.util.List;

/** One message in a {@link Conversation}. */
public final class Turn {

    public enum Role { STUDENT, TUTOR }

    public final Role role;
    public final Mode mode;
    /** Student: what they typed. Tutor: the reply JSON. */
    public final String text;
    /** Student: the message actually sent to the model (with its TASK line). */
    public final String requestText;
    /** Tutor: provider data to replay on later requests (see LlmMessage), or null. */
    public final String providerPayload;
    /** Tutor: romaji still present after the retry budget ran out. Normally empty. */
    public final List<String> unresolvedRomaji;

    /** Tutor: the parsed reply (a Responses.* object). Rebuilt from {@link #text}; not serialized. */
    public transient Object parsed;

    private Turn(Role role, Mode mode, String text, String requestText, String providerPayload,
                 List<String> unresolvedRomaji) {
        this.role = role;
        this.mode = mode;
        this.text = text;
        this.requestText = requestText;
        this.providerPayload = providerPayload;
        this.unresolvedRomaji = unresolvedRomaji;
    }

    public static Turn student(Mode mode, String typed, String requestText) {
        return new Turn(Role.STUDENT, mode, typed, requestText, null, new ArrayList<>());
    }

    public static Turn tutor(Mode mode, String json, String providerPayload, List<String> unresolvedRomaji,
                             Object parsed) {
        Turn t = new Turn(Role.TUTOR, mode, json, null, providerPayload, unresolvedRomaji);
        t.parsed = parsed;
        return t;
    }
}
