package com.cmacfarl.sensei.llm;

import java.util.List;
import java.util.Map;

/**
 * Canned replies for trying the UI without an API key (debug builds, key "demo"). The first
 * translation deliberately leaks romaji so the rewrite loop can be seen working.
 */
public final class DemoClient implements LlmClient {

    private static final String TRANSLATE_LEAKY = """
            {"polite":"昨日[きのう]、図書館[としょかん]へ勉強[べんきょう]しに行[い]きました。",
             "casual":"昨日[きのう]、図書館[としょかん]に勉強[べんきょう]しに行[い]った。",
             "casual_note":"Casual speech uses the plain past 行[い]った instead of 行[い]きました, and に is more common than へ in conversation.",
             "chunks":[
              {"japanese":"昨日[きのう]、","role":"time","explanation":"Relative time words like 昨日[きのう] take no particle."},
              {"japanese":"図書館[としょかん]へ","role":"destination","explanation":"へ marks the direction you move toward. に also works here."},
              {"japanese":"勉強[べんきょう]しに","role":"purpose","explanation":"masu-stem + に before a movement verb gives the purpose of going."},
              {"japanese":"行[い]きました","role":"main verb","explanation":"Polite past of 行[い]く."}],
             "notes":""}
            """;

    private static final String TRANSLATE_CLEAN = TRANSLATE_LEAKY.replace("masu-stem", "ます-stem");

    private static final String CHECK = """
            {"verdict":"incorrect",
             "original":"私[わたし]は昨日[きのう]学校[がっこう]に行[い]きます。",
             "meaning":"\\"I will go to school yesterday.\\" You probably meant \\"I went to school yesterday.\\"",
             "issues":[{"original":"行[い]きます","fix":"行[い]きました","explanation":"昨日[きのう] puts the action in the past, so the verb needs the past form ました."}],
             "corrected":"私[わたし]は昨日[きのう]学校[がっこう]に行[い]きました。",
             "natural_polite":"昨日[きのう]は学校[がっこう]に行[い]きました。",
             "natural_casual":"昨日[きのう]、学校[がっこう]行[い]った。",
             "comment":"Good use of は and に. Just watch the tense when you use a time word like 昨日[きのう]."}
            """;

    private static final String FOLLOW_UP = """
            {"answer":"Both に and へ can mark a destination. へ focuses on the direction of travel, while に focuses on arriving at the place.\\n\\nIn everyday speech に is far more common, which is why the casual version uses it.",
             "examples":[
              {"japanese":"駅[えき]に着[つ]いた。","english":"I arrived at the station."},
              {"japanese":"東[ひがし]へ向[む]かう。","english":"Head east."}]}
            """;

    @Override
    public LlmReply send(String system, List<LlmMessage> messages, Map<String, Object> schema) throws LlmException {
        try {
            Thread.sleep(600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String last = messages.get(messages.size() - 1).text;
        if (last.startsWith("TASK: TRANSLATE")) return new LlmReply(TRANSLATE_LEAKY, null);
        if (last.startsWith("TASK: CHECK")) return new LlmReply(CHECK, null);
        if (last.startsWith("TASK: FOLLOW_UP")) return new LlmReply(FOLLOW_UP, null);
        return new LlmReply(TRANSLATE_CLEAN, null); // the romaji rewrite request
    }
}
