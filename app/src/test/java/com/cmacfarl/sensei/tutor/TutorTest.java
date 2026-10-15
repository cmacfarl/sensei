package com.cmacfarl.sensei.tutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.cmacfarl.sensei.llm.LlmClient;
import com.cmacfarl.sensei.llm.LlmMessage;
import com.cmacfarl.sensei.llm.LlmReply;

import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

public class TutorTest {

    private static final String CLEAN_FOLLOW_UP =
            "{\"answer\":\"Use は for the topic.\",\"examples\":[]}";
    private static final String LEAKY_FOLLOW_UP =
            "{\"answer\":\"Use wa for the topic.\",\"examples\":[]}";
    private static final String TRANSLATION =
            "{\"polite\":\"食[た]べます。\",\"casual\":\"食[た]べる。\",\"casual_note\":\"Plain form.\","
                    + "\"chunks\":[{\"japanese\":\"食[た]べます\",\"role\":\"verb\",\"explanation\":\"Polite.\"}],"
                    + "\"notes\":\"\"}";

    /** Replays canned replies and records every request. */
    private static final class FakeClient implements LlmClient {
        final Deque<String> replies;
        final List<List<LlmMessage>> requests = new ArrayList<>();
        final List<Map<String, Object>> schemas = new ArrayList<>();

        FakeClient(String... replies) {
            this.replies = new ArrayDeque<>(Arrays.asList(replies));
        }

        @Override
        public LlmReply send(String system, List<LlmMessage> messages, Map<String, Object> schema) {
            requests.add(new ArrayList<>(messages));
            schemas.add(schema);
            return new LlmReply(replies.removeFirst(), null);
        }
    }

    @Test
    public void modeComesFromTheFirstMessage() {
        assertEquals(Mode.TRANSLATE, Mode.forNewSentence("I like cats."));
        assertEquals(Mode.CHECK, Mode.forNewSentence("ねこが好きです。"));
        assertEquals(Mode.CHECK, Mode.forNewSentence("I think 猫 is cute"));
    }

    @Test
    public void translatesEnglishAndParsesTheReply() throws Exception {
        FakeClient client = new FakeClient(TRANSLATION);
        Conversation c = new Conversation("gemini", "m");
        Tutor.Exchange ex = new Tutor(client).ask(c, "I eat.");

        assertEquals(Mode.TRANSLATE, ex.student.mode);
        assertEquals("TASK: TRANSLATE\nI eat.", client.requests.get(0).get(0).text);
        Responses.Translate r = (Responses.Translate) ex.tutor.parsed;
        assertEquals("食[た]べる。", r.casual);
        assertEquals("Plain form.", r.casualNote);
        assertEquals(1, r.chunks.size());
        assertTrue(ex.tutor.unresolvedRomaji.isEmpty());
        assertEquals(Schemas.translate(), client.schemas.get(0));
    }

    @Test
    public void followUpsCarryTheThread() throws Exception {
        Conversation c = new Conversation("gemini", "m");
        Tutor.Exchange first = new Tutor(new FakeClient(TRANSLATION)).ask(c, "I eat.");
        c.append(first.student, first.tutor);

        FakeClient client = new FakeClient(CLEAN_FOLLOW_UP);
        Tutor.Exchange second = new Tutor(client).ask(c, "Why ます?");

        assertEquals(Mode.FOLLOW_UP, second.student.mode);
        List<LlmMessage> sent = client.requests.get(0);
        assertEquals(3, sent.size());
        assertEquals(LlmMessage.Role.USER, sent.get(0).role);
        assertEquals(LlmMessage.Role.MODEL, sent.get(1).role);
        assertEquals(TRANSLATION, sent.get(1).text);
        assertEquals("TASK: FOLLOW_UP\nWhy ます?", sent.get(2).text);
    }

    @Test
    public void romajiTriggersARewrite() throws Exception {
        Conversation c = conversationWithOneExchange();
        FakeClient client = new FakeClient(LEAKY_FOLLOW_UP, CLEAN_FOLLOW_UP);
        Tutor.Exchange ex = new Tutor(client).ask(c, "Which particle?");

        assertEquals(2, client.requests.size());
        List<LlmMessage> retry = client.requests.get(1);
        assertEquals(LEAKY_FOLLOW_UP, retry.get(retry.size() - 2).text);
        assertTrue(retry.get(retry.size() - 1).text.contains("\"wa\""));
        assertEquals(CLEAN_FOLLOW_UP, ex.tutor.text);
        assertTrue(ex.tutor.unresolvedRomaji.isEmpty());

        // The rejected draft never enters the thread.
        c.append(ex.student, ex.tutor);
        for (Turn t : c.turns()) assertTrue(!t.text.equals(LEAKY_FOLLOW_UP));
    }

    @Test
    public void givesUpAfterTheRetryBudgetAndReportsTheLeak() throws Exception {
        Conversation c = conversationWithOneExchange();
        FakeClient client = new FakeClient(LEAKY_FOLLOW_UP, LEAKY_FOLLOW_UP, LEAKY_FOLLOW_UP);
        Tutor.Exchange ex = new Tutor(client).ask(c, "Which particle?");

        assertEquals(Tutor.MAX_ROMAJI_RETRIES + 1, client.requests.size());
        assertEquals(List.of("wa"), ex.tutor.unresolvedRomaji);
    }

    @Test
    public void checkModeFlagsRomajiInJapaneseFields() {
        String reply = "{\"verdict\":\"correct\",\"original\":\"watashi wa\",\"meaning\":\"I\",\"issues\":[],"
                + "\"corrected\":\"\",\"natural_polite\":\"\",\"natural_casual\":\"\",\"comment\":\"Good.\"}";
        List<String> hits = RomajiCheck.find(Mode.CHECK, com.google.gson.JsonParser.parseString(reply).getAsJsonObject());
        assertTrue(hits.contains("watashi"));
        assertTrue(hits.contains("wa"));
    }

    @Test
    public void verdictParsing() throws Exception {
        String reply = "{\"verdict\":\"unnatural\",\"original\":\"\",\"meaning\":\"\",\"issues\":[],"
                + "\"corrected\":\"\",\"natural_polite\":\"\",\"natural_casual\":\"\",\"comment\":\"\"}";
        Responses.Check r = (Responses.Check) Tutor.toResponse(Mode.CHECK, Tutor.parseJson(reply));
        assertEquals(Responses.Verdict.UNNATURAL, r.verdictValue());
    }

    @Test
    public void toleratesMarkdownFence() throws Exception {
        assertEquals("x", Tutor.parseJson("```json\n{\"answer\":\"x\"}\n```").get("answer").getAsString());
    }

    private static Conversation conversationWithOneExchange() throws Exception {
        Conversation c = new Conversation("gemini", "m");
        Tutor.Exchange first = new Tutor(new FakeClient(TRANSLATION)).ask(c, "I eat.");
        c.append(first.student, first.tutor);
        return c;
    }
}
