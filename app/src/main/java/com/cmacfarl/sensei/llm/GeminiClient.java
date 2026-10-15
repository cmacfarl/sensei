package com.cmacfarl.sensei.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Gemini via the REST generateContent endpoint. */
public final class GeminiClient implements LlmClient {

    private static final String ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .build();

    private final String apiKey;
    private final String model;
    private final String thinkingLevel;
    private final Gson gson = new Gson();

    /** @param thinkingLevel "low", "medium" or "high" */
    public GeminiClient(String apiKey, String model, String thinkingLevel) {
        this.apiKey = apiKey;
        this.model = model;
        this.thinkingLevel = thinkingLevel;
    }

    @Override
    public LlmReply send(String system, List<LlmMessage> messages, Map<String, Object> schema) throws LlmException {
        String body = gson.toJson(buildRequest(system, messages, schema));
        Request request = new Request.Builder()
                .url(String.format(ENDPOINT, model))
                .header("x-goog-api-key", apiKey)
                .post(RequestBody.create(body, JSON))
                .build();

        try (Response response = HTTP.newCall(request).execute()) {
            String text = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new LlmException("Gemini error " + response.code() + ": " + errorMessage(text),
                        response.code() == 429 || response.code() >= 500);
            }
            return parseResponse(text);
        } catch (IOException e) {
            throw new LlmException("Network error: " + e.getMessage(), true, e);
        }
    }

    JsonObject buildRequest(String system, List<LlmMessage> messages, Map<String, Object> schema) {
        JsonObject req = new JsonObject();

        JsonObject systemInstruction = new JsonObject();
        systemInstruction.add("parts", textParts(system));
        req.add("systemInstruction", systemInstruction);

        JsonArray contents = new JsonArray();
        for (LlmMessage m : messages) {
            JsonObject content = new JsonObject();
            content.addProperty("role", m.role == LlmMessage.Role.USER ? "user" : "model");
            if (m.providerPayload != null) {
                content.add("parts", JsonParser.parseString(m.providerPayload));
            } else {
                content.add("parts", textParts(m.text));
            }
            contents.add(content);
        }
        req.add("contents", contents);

        JsonObject config = new JsonObject();
        config.addProperty("responseMimeType", "application/json");
        config.add("responseJsonSchema", gson.toJsonTree(schema));
        config.add("thinkingConfig", thinkingConfig());
        req.add("generationConfig", config);
        return req;
    }

    /**
     * Gemini 3+ models take a thinking level. 2.5 models predate levels and take a token
     * budget, so the level is mapped to one.
     */
    JsonObject thinkingConfig() {
        JsonObject thinking = new JsonObject();
        if (model.startsWith("gemini-2.")) {
            int budget;
            switch (thinkingLevel) {
                case "low":
                    budget = 1024;
                    break;
                case "high":
                    budget = 24576;
                    break;
                default:
                    budget = 8192;
            }
            thinking.addProperty("thinkingBudget", budget);
        } else {
            thinking.addProperty("thinkingLevel", thinkingLevel);
        }
        return thinking;
    }

    /** Extracts the reply text, keeping the non-thought parts (with signatures) for replay. */
    static LlmReply parseResponse(String json) throws LlmException {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new LlmException("Gemini returned something that isn't JSON.", true, e);
        }

        JsonObject feedback = root.getAsJsonObject("promptFeedback");
        if (feedback != null && feedback.has("blockReason")) {
            throw new LlmException("Gemini blocked the request (" + feedback.get("blockReason").getAsString() + ").", false);
        }
        JsonArray candidates = root.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new LlmException("Gemini returned no answer.", true);
        }
        JsonObject candidate = candidates.get(0).getAsJsonObject();
        String finish = candidate.has("finishReason") ? candidate.get("finishReason").getAsString() : "STOP";

        StringBuilder text = new StringBuilder();
        JsonArray keptParts = new JsonArray();
        JsonObject content = candidate.getAsJsonObject("content");
        JsonArray parts = content != null ? content.getAsJsonArray("parts") : null;
        if (parts != null) {
            for (JsonElement p : parts) {
                JsonObject part = p.getAsJsonObject();
                if (part.has("thought") && part.get("thought").getAsBoolean()) continue;
                if (part.has("text")) text.append(part.get("text").getAsString());
                keptParts.add(part);
            }
        }

        if (!"STOP".equals(finish)) {
            throw new LlmException("Gemini stopped early (" + finish + ").", !"SAFETY".equals(finish));
        }
        if (text.length() == 0) {
            throw new LlmException("Gemini returned an empty answer.", true);
        }
        return new LlmReply(text.toString(), keptParts.toString());
    }

    private static JsonArray textParts(String text) {
        JsonObject part = new JsonObject();
        part.addProperty("text", text);
        JsonArray parts = new JsonArray();
        parts.add(part);
        return parts;
    }

    private static String errorMessage(String body) {
        try {
            JsonObject err = JsonParser.parseString(body).getAsJsonObject().getAsJsonObject("error");
            if (err != null && err.has("message")) return err.get("message").getAsString();
        } catch (RuntimeException ignored) {
            // Fall through to the raw body.
        }
        return body.length() > 300 ? body.substring(0, 300) : body;
    }
}
