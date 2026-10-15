package com.cmacfarl.sensei.tutor;

import java.util.ArrayList;
import java.util.List;

/**
 * The structured replies the model returns, one class per {@link Mode}. Japanese strings carry
 * furigana markup ({@code 食[た]べる}); see {@link com.cmacfarl.sensei.text.FuriganaParser}.
 * Field names map to snake_case JSON.
 */
public final class Responses {

    private Responses() {}

    public static final class Translate {
        public String polite = "";
        public String casual = "";
        public String casualNote = "";
        public List<Chunk> chunks = new ArrayList<>();
        public String notes = "";
    }

    public static final class Chunk {
        public String japanese = "";
        public String role = "";
        public String explanation = "";
    }

    public static final class Check {
        public String verdict = "";
        public String original = "";
        public String meaning = "";
        public List<Issue> issues = new ArrayList<>();
        public String corrected = "";
        public String naturalPolite = "";
        public String naturalCasual = "";
        public String comment = "";

        public Verdict verdictValue() {
            return Verdict.parse(verdict);
        }
    }

    public enum Verdict {
        CORRECT, UNNATURAL, INCORRECT;

        static Verdict parse(String s) {
            if (s == null) return INCORRECT;
            switch (s.trim().toLowerCase(java.util.Locale.ROOT)) {
                case "correct":
                    return CORRECT;
                case "unnatural":
                    return UNNATURAL;
                default:
                    return INCORRECT;
            }
        }
    }

    public static final class Issue {
        public String original = "";
        public String fix = "";
        public String explanation = "";
    }

    public static final class FollowUp {
        public String answer = "";
        public List<Example> examples = new ArrayList<>();
    }

    public static final class Example {
        public String japanese = "";
        public String english = "";
    }
}
