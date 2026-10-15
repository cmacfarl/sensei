package com.cmacfarl.sensei.tutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON Schemas for each {@link Mode}'s reply, as plain maps so each provider can serialize them
 * its own way. Kept to the subset both Gemini and Claude accept: objects, arrays, strings and
 * string enums, every property required, no additional properties.
 */
public final class Schemas {

    private static final String JA = " Japanese script only, with furigana markup on every kanji, e.g. 食[た]べる.";
    private static final String EN = " English. Any Japanese inside it is in Japanese script with furigana markup; never romaji.";

    private Schemas() {}

    public static Map<String, Object> forMode(Mode mode) {
        switch (mode) {
            case TRANSLATE:
                return translate();
            case CHECK:
                return check();
            default:
                return followUp();
        }
    }

    static Map<String, Object> translate() {
        Map<String, Object> chunk = object(
                "japanese", string("A chunk of the polite sentence, exactly as it appears there." + JA),
                "role", string("Short English label for what the chunk does, e.g. \"topic\", \"reason clause\"."),
                "explanation", string("The grammar at work in this chunk." + EN));
        return object(
                "polite", string("Natural polite (です/ます) translation." + JA),
                "casual", string("Natural casual translation, the way friends really talk." + JA),
                "casual_note", string("How and why the casual version differs from the polite one." + EN),
                "chunks", array(chunk, "2 to 6 meaningful chunks of the polite sentence, in order."),
                "notes", string("Optional extra notes (nuance, alternatives). Empty string if none." + EN));
    }

    static Map<String, Object> check() {
        Map<String, Object> issue = object(
                "original", string("The problematic chunk from the student's sentence." + JA),
                "fix", string("The corrected chunk." + JA),
                "explanation", string("Why it was wrong or unnatural and how the fix works." + EN));
        Map<String, Object> verdict = string(
                "correct = grammatical and natural; unnatural = grammatical but not how a native would say it; "
                        + "incorrect = grammar or usage errors.");
        verdict.put("enum", Arrays.asList("correct", "unnatural", "incorrect"));
        return object(
                "verdict", verdict,
                "original", string("The student's text, unchanged except for added furigana markup." + JA),
                "meaning", string("What the sentence says in English, and what the student probably meant if different."),
                "issues", array(issue, "Each problem found. Empty if the verdict is correct."),
                "corrected", string("Minimal correction keeping the student's wording and register; identical to original if correct." + JA),
                "natural_polite", string("How a native speaker would naturally say the intended meaning, polite." + JA),
                "natural_casual", string("How a native speaker would naturally say the intended meaning, casual." + JA),
                "comment", string("A short, encouraging teacher's comment, including what the student did well." + EN));
    }

    static Map<String, Object> followUp() {
        Map<String, Object> example = object(
                "japanese", string("An example sentence." + JA),
                "english", string("Its English translation."));
        return object(
                "answer", string("The teacher's answer. Paragraphs separated by blank lines." + EN),
                "examples", array(example, "Example sentences when they help. Empty list otherwise."));
    }

    /** Field names whose values must be pure Japanese, per mode (dotted for nested arrays). */
    public static List<String> japaneseFields(Mode mode) {
        switch (mode) {
            case TRANSLATE:
                return Arrays.asList("polite", "casual", "chunks.japanese");
            case CHECK:
                return Arrays.asList("original", "issues.original", "issues.fix", "corrected",
                        "natural_polite", "natural_casual");
            default:
                return Arrays.asList("examples.japanese");
        }
    }

    private static Map<String, Object> string(String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "string");
        m.put("description", description);
        return m;
    }

    private static Map<String, Object> array(Map<String, Object> items, String description) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "array");
        m.put("description", description);
        m.put("items", items);
        return m;
    }

    private static Map<String, Object> object(Object... nameSchemaPairs) {
        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (int i = 0; i < nameSchemaPairs.length; i += 2) {
            props.put((String) nameSchemaPairs[i], nameSchemaPairs[i + 1]);
            required.add((String) nameSchemaPairs[i]);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "object");
        m.put("properties", props);
        m.put("required", required);
        m.put("additionalProperties", false);
        return m;
    }
}
