package com.cmacfarl.sensei.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parses the furigana markup the model is instructed to produce: a run of kanji immediately
 * followed by its reading in square brackets, e.g. {@code 食[た]べる}, {@code 今日[きょう]は}.
 * Okurigana stays outside the brackets, so the reading only ever covers kanji.
 *
 * <p>Text that isn't markup (English, brackets after non-kanji) passes through untouched.
 */
public final class FuriganaParser {

    /** A piece of text, optionally carrying a reading for its (kanji-only) base. */
    public static final class Token {
        public final String base;
        public final String reading; // null for plain text

        public Token(String base, String reading) {
            this.base = base;
            this.reading = reading;
        }

        public boolean isRuby() {
            return reading != null;
        }

        @Override
        public String toString() {
            return isRuby() ? base + "[" + reading + "]" : base;
        }
    }

    public static final class Result {
        public final List<Token> tokens;
        /** Bracketed readings attached to kanji that were not pure kana (e.g. romaji). */
        public final List<String> invalidReadings;

        Result(List<Token> tokens, List<String> invalidReadings) {
            this.tokens = Collections.unmodifiableList(tokens);
            this.invalidReadings = Collections.unmodifiableList(invalidReadings);
        }

        /** The text with all markup removed. */
        public String plainText() {
            StringBuilder sb = new StringBuilder();
            for (Token t : tokens) sb.append(t.base);
            return sb.toString();
        }
    }

    private FuriganaParser() {}

    /**
     * @param stripSpaces remove whitespace between Japanese characters. Use for fields that are
     *                    pure Japanese sentences; models sometimes insert Anki-style spaces.
     */
    public static Result parse(String input, boolean stripSpaces) {
        String s = input.replace('［', '[').replace('］', ']');
        if (stripSpaces) s = stripJapaneseSpaces(s);

        List<Token> tokens = new ArrayList<>();
        List<String> invalid = new ArrayList<>();
        StringBuilder plain = new StringBuilder();

        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            int close = ch == '[' ? s.indexOf(']', i + 1) : -1;
            if (close < 0) {
                plain.append(ch);
                i++;
                continue;
            }
            String reading = s.substring(i + 1, close).trim();
            int runStart = kanjiRunStart(plain);

            if (runStart < plain.length()) {
                if (JapaneseChars.isAllKana(reading)) {
                    if (runStart > 0) tokens.add(new Token(plain.substring(0, runStart), null));
                    tokens.add(new Token(plain.substring(runStart), reading));
                    plain.setLength(0);
                    i = close + 1;
                    continue;
                }
                if (!reading.isEmpty() && !JapaneseChars.containsJapanese(reading)) {
                    // Kanji followed by a non-kana "reading": almost certainly romaji.
                    invalid.add(reading);
                }
            } else if (plain.length() > 0
                    && JapaneseChars.isKana(plain.codePointBefore(plain.length()))
                    && JapaneseChars.isAllKana(reading)) {
                // A redundant reading on a kana word (ねこ[ねこ]); drop it.
                i = close + 1;
                continue;
            }
            plain.append(ch);
            i++;
        }
        if (plain.length() > 0) tokens.add(new Token(plain.toString(), null));
        return new Result(tokens, invalid);
    }

    /** Index in {@code sb} where the trailing run of kanji-like characters starts. */
    private static int kanjiRunStart(CharSequence sb) {
        int k = sb.length();
        while (k > 0) {
            int c = Character.codePointBefore(sb, k);
            if (!JapaneseChars.isKanjiLike(c)) break;
            k -= Character.charCount(c);
        }
        return k;
    }

    static String stripJapaneseSpaces(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            char ch = s.charAt(i);
            if (!Character.isWhitespace(ch) || ch == '\n') {
                out.append(ch);
                i++;
                continue;
            }
            int j = i;
            while (j < s.length() && Character.isWhitespace(s.charAt(j)) && s.charAt(j) != '\n') j++;
            boolean prevJa = out.length() > 0 && isJapaneseOrMarkup(out.codePointBefore(out.length()));
            boolean nextJa = j < s.length() && isJapaneseOrMarkup(s.codePointAt(j));
            if (!(prevJa && nextJa)) out.append(s, i, j);
            i = j;
        }
        return out.toString();
    }

    private static boolean isJapaneseOrMarkup(int c) {
        return JapaneseChars.isJapanese(c) || c == ']';
    }
}
