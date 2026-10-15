package com.cmacfarl.sensei.llm;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.JsonOutputFormat;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Claude via the official Anthropic Java SDK, using structured outputs for the reply schema. */
public final class ClaudeClient implements LlmClient {

    /** Models that accept server-side refusal fallbacks ({@code fallbacks: "default"}). */
    private static final Set<String> FALLBACK_MODELS = new HashSet<>(Arrays.asList(
            "claude-opus-5-5", "claude-opus-5", "claude-sonnet-5-5", "claude-fable-5-1"));

    private static AnthropicClient cachedClient;
    private static String cachedKey;

    private final String apiKey;
    private final String model;

    public ClaudeClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    private static synchronized AnthropicClient client(String apiKey) {
        if (cachedClient == null || !apiKey.equals(cachedKey)) {
            cachedClient = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            cachedKey = apiKey;
        }
        return cachedClient;
    }

    @Override
    public LlmReply send(String system, List<LlmMessage> messages, Map<String, Object> schema) throws LlmException {
        JsonOutputFormat.Schema.Builder schemaBuilder = JsonOutputFormat.Schema.builder();
        for (Map.Entry<String, Object> e : schema.entrySet()) {
            schemaBuilder.putAdditionalProperty(e.getKey(), JsonValue.from(e.getValue()));
        }

        MessageCreateParams.Builder params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(16000L)
                // The system prompt never changes, so cache it.
                .systemOfTextBlockParams(Collections.singletonList(TextBlockParam.builder()
                        .text(system)
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.MEDIUM)
                        .format(JsonOutputFormat.builder().schema(schemaBuilder.build()).build())
                        .build());

        if (FALLBACK_MODELS.contains(model)) {
            // If a safety classifier declines, let the API retry on a suitable model instead of failing.
            params.putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01");
            params.putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
        }

        // Prior assistant turns are replayed as their final text only; thinking blocks are not sent back.
        for (LlmMessage m : messages) {
            if (m.role == LlmMessage.Role.USER) params.addUserMessage(m.text);
            else params.addAssistantMessage(m.text);
        }

        Message response;
        try {
            response = client(apiKey).messages().create(params.build());
        } catch (UnauthorizedException e) {
            throw new LlmException("Claude rejected the API key. Check it in Settings.", false, e);
        } catch (RateLimitException e) {
            throw new LlmException("Claude rate limit reached. Try again shortly.", true, e);
        } catch (AnthropicServiceException e) {
            throw new LlmException("Claude error " + e.statusCode() + ": " + e.getMessage(),
                    e.statusCode() >= 500, e);
        } catch (RuntimeException e) {
            throw new LlmException("Network error: " + e.getMessage(), true, e);
        }

        StopReason stop = response.stopReason().orElse(StopReason.END_TURN);
        if (StopReason.REFUSAL.equals(stop)) {
            throw new LlmException("Claude declined to answer this one.", false);
        }
        if (StopReason.MAX_TOKENS.equals(stop)) {
            throw new LlmException("Claude's answer was cut off (too long).", true);
        }

        StringBuilder text = new StringBuilder();
        for (ContentBlock block : response.content()) {
            block.text().ifPresent(t -> text.append(t.text()));
        }
        if (text.length() == 0) {
            throw new LlmException("Claude returned an empty answer.", true);
        }
        return new LlmReply(text.toString(), null);
    }
}
