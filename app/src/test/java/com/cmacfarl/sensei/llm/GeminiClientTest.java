package com.cmacfarl.sensei.llm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.cmacfarl.sensei.tutor.Mode;
import com.cmacfarl.sensei.tutor.Schemas;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.Arrays;

public class GeminiClientTest {

    @Test
    public void skipsThoughtPartsAndKeepsSignatures() throws Exception {
        String json = "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"role\":\"model\",\"parts\":["
                + "{\"text\":\"thinking...\",\"thought\":true},"
                + "{\"text\":\"{\\\"answer\\\":\",\"thoughtSignature\":\"abc\"},"
                + "{\"text\":\"\\\"x\\\"}\"}]}}]}";
        LlmReply reply = GeminiClient.parseResponse(json);
        assertEquals("{\"answer\":\"x\"}", reply.text);
        assertTrue(reply.providerPayload.contains("abc"));
        assertFalse(reply.providerPayload.contains("thinking..."));
    }

    @Test
    public void blockedPromptIsNotRetryable() {
        try {
            GeminiClient.parseResponse("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
            fail();
        } catch (LlmException e) {
            assertFalse(e.isRetryable());
        }
    }

    @Test
    public void truncatedAnswerIsAnError() {
        try {
            GeminiClient.parseResponse("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\","
                    + "\"content\":{\"parts\":[{\"text\":\"{\"}]}}]}");
            fail();
        } catch (LlmException e) {
            assertTrue(e.getMessage().contains("MAX_TOKENS"));
        }
    }

    @Test
    public void requestReplaysModelPartsAndSendsSchema() {
        GeminiClient client = new GeminiClient("k", "gemini-test", "medium");
        JsonObject req = client.buildRequest("sys", Arrays.asList(
                LlmMessage.user("hi"),
                LlmMessage.model("{}", "[{\"text\":\"{}\",\"thoughtSignature\":\"sig\"}]"),
                LlmMessage.user("more")), Schemas.forMode(Mode.FOLLOW_UP));

        assertEquals("sys", req.getAsJsonObject("systemInstruction").getAsJsonArray("parts")
                .get(0).getAsJsonObject().get("text").getAsString());
        assertEquals(3, req.getAsJsonArray("contents").size());
        JsonObject model = req.getAsJsonArray("contents").get(1).getAsJsonObject();
        assertEquals("model", model.get("role").getAsString());
        assertEquals("sig", model.getAsJsonArray("parts").get(0).getAsJsonObject()
                .get("thoughtSignature").getAsString());
        JsonObject config = req.getAsJsonObject("generationConfig");
        assertEquals("application/json", config.get("responseMimeType").getAsString());
        assertEquals("object", config.getAsJsonObject("responseJsonSchema").get("type").getAsString());
        assertEquals("medium", config.getAsJsonObject("thinkingConfig").get("thinkingLevel").getAsString());
    }

    @Test
    public void thinkingLevelPerModelGeneration() {
        JsonObject g3 = new GeminiClient("k", "gemini-3.8-flash", "low").thinkingConfig();
        assertEquals("low", g3.get("thinkingLevel").getAsString());
        assertFalse(g3.has("thinkingBudget"));

        JsonObject g25 = new GeminiClient("k", "gemini-2.5-flash", "high").thinkingConfig();
        assertEquals(24576, g25.get("thinkingBudget").getAsInt());
        assertFalse(g25.has("thinkingLevel"));
    }
}
