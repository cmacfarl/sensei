package com.cmacfarl.sensei.text;

/**
 * Picks the text to look up in a dictionary when the student long-presses a kanji word: the
 * kanji plus its okurigana and conjugation (行った, 食べさせられません), but not the particles
 * and grammar that follow (行ったほうがいいです → 行った). Jisho deconjugates what's left.
 *
 * <p>A heuristic: hiragana after a kanji is either part of the word or the start of the next
 * one, and there's no dictionary here to tell which. It stops at the first thing that can't be
 * okurigana or a conjugation ending.
 */
public final class DictionaryLookup {

    /** Grammar written in kana that always starts a new word. Checked before single particles. */
    private static final String[] BOUNDARY_WORDS = {
            "ほう", "こと", "もの", "とき", "ところ", "です", "でした", "でしょう", "だろう",
            "から", "まで", "より", "けど", "けれど", "ので", "のに", "ながら", "くらい", "ぐらい",
            "だけ", "しか", "など", "ください"};

    /** Particles that end the word wherever they appear, even right after the kanji (図書館へ). */
    private static final String ALWAYS_PARTICLES = "をはへのねよとも";

    private DictionaryLookup() {}

    /** The lookup text for the kanji word at {@code text[start, end)}. */
    public static String forWord(String text, int start, int end) {
        int stop = end;
        while (stop < text.length()) {
            int c = text.codePointAt(stop);
            if (!JapaneseChars.isHiragana(c) || endsWordAt(text, end, stop)) break;
            stop += Character.charCount(c);
        }
        return text.substring(start, stop);
    }

    /** Whether the hiragana at {@code i} starts something after the word (kanji end at {@code wordEnd}). */
    private static boolean endsWordAt(String text, int wordEnd, int i) {
        boolean first = i == wordEnd;
        char c = text.charAt(i);
        char prev = first ? 0 : text.charAt(i - 1);
        char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;

        for (String w : BOUNDARY_WORDS) {
            if (!text.startsWith(w, i)) continue;
            // ...ませんでした / ませんでしょう are one conjugation, not ません + copula.
            if (w.startsWith("でし") && prev == 'ん') continue;
            return true;
        }
        if (ALWAYS_PARTICLES.indexOf(c) >= 0) return true;
        switch (c) {
            case 'が':
                // 上がる, 曲がった: が is okurigana when a verb ending follows.
                return !(first && "るりっられろ".indexOf(next) >= 0);
            case 'に':
                return true;
            case 'で':
                // 読んで, 泳いで: て-form, not the particle.
                return prev != 'ん' && prev != 'い';
            case 'か':
                // 行かない, 静か: か right after the kanji is okurigana; なかった, たかった: past tense.
                return !first && next != 'っ';
            default:
                return false;
        }
    }
}
