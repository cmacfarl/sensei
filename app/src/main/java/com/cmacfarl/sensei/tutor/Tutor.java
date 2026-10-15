package com.cmacfarl.sensei.tutor;

import com.cmacfarl.sensei.llm.LlmClient;
import com.cmacfarl.sensei.llm.LlmException;
import com.cmacfarl.sensei.llm.LlmMessage;
import com.cmacfarl.sensei.llm.LlmReply;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs one exchange: sends the student's message with the thread so far, then checks the reply
 * for romaji and asks the model to rewrite it if any slipped through.
 */
public final class Tutor {

    /** Rewrites requested after the first reply before giving up and showing what we have. */
    public static final int MAX_ROMAJI_RETRIES = 2;

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    private final LlmClient client;

    public Tutor(LlmClient client) {
        this.client = client;
    }

    public static final class Exchange {
        public final Turn student;
        public final Turn tutor;

        Exchange(Turn student, Turn tutor) {
            this.student = student;
            this.tutor = tutor;
        }
    }

    /** Blocking. The conversation is not modified; append the returned exchange on success. */
    public Exchange ask(Conversation conversation, String typed) throws LlmException {
        Mode mode = conversation.isEmpty() ? Mode.forNewSentence(typed) : Mode.FOLLOW_UP;
        String request = Prompts.userMessage(mode, typed);

        List<LlmMessage> messages = history(conversation);
        messages.add(LlmMessage.user(request));

        LlmReply reply = null;
        JsonObject json = null;
        List<String> romaji = new ArrayList<>();
        for (int attempt = 0; attempt <= MAX_ROMAJI_RETRIES; attempt++) {
            reply = client.send(Prompts.SYSTEM, messages, Schemas.forMode(mode));
            json = parseJson(reply.text);
            romaji = RomajiCheck.find(mode, json);
            if (romaji.isEmpty() || attempt == MAX_ROMAJI_RETRIES) break;
            messages.add(LlmMessage.model(reply.text, reply.providerPayload));
            messages.add(LlmMessage.user(Prompts.romajiCorrection(romaji)));
        }

        Turn student = Turn.student(mode, typed, request);
        Turn tutor = Turn.tutor(mode, reply.text, reply.providerPayload, romaji, toResponse(mode, json));
        return new Exchange(student, tutor);
    }

    /** The thread as provider messages. Only accepted replies are in it, never rejected drafts. */
    static List<LlmMessage> history(Conversation conversation) {
        List<LlmMessage> out = new ArrayList<>();
        for (Turn t : conversation.turns()) {
            out.add(t.role == Turn.Role.STUDENT
                    ? LlmMessage.user(t.requestText)
                    : LlmMessage.model(t.text, t.providerPayload));
        }
        return out;
    }

    static JsonObject parseJson(String text) throws LlmException {
        String s = text.trim();
        // Tolerate a markdown fence in case a provider adds one.
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            int end = s.lastIndexOf("```");
            if (start >= 0 && end > start) s = s.substring(start + 1, end).trim();
        }
        try {
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new LlmException("The tutor's reply wasn't in the expected format. Try again.", true, e);
        }
    }

    /** For re-rendering a stored reply; throws an unchecked exception if it can't be parsed. */
    public static JsonObject parseJsonForDisplay(String text) {
        try {
            return parseJson(text);
        } catch (LlmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Object toResponse(Mode mode, JsonObject json) {
        switch (mode) {
            case TRANSLATE:
                return GSON.fromJson(json, Responses.Translate.class);
            case CHECK:
                return GSON.fromJson(json, Responses.Check.class);
            default:
                return GSON.fromJson(json, Responses.FollowUp.class);
        }
    }
}
