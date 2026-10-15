package com.cmacfarl.sensei.text;

/** Character classification helpers for Japanese text. */
public final class JapaneseChars {

    private JapaneseChars() {}

    public static boolean isHiragana(int c) {
        return c >= 0x3041 && c <= 0x309F;
    }

    public static boolean isKatakana(int c) {
        return (c >= 0x30A0 && c <= 0x30FF)    // includes ー
                || (c >= 0x31F0 && c <= 0x31FF) // phonetic extensions
                || (c >= 0xFF66 && c <= 0xFF9F); // half-width
    }

    public static boolean isKana(int c) {
        return isHiragana(c) || isKatakana(c);
    }

    /**
     * True for characters that can carry a furigana reading: CJK ideographs plus the
     * iteration mark 々 and the counters ヶ/ヵ/〆, which behave like kanji in compounds (一ヶ月).
     */
    public static boolean isKanjiLike(int c) {
        return isKanji(c) || c == '々' || c == '〆' || c == 'ヶ' || c == 'ヵ';
    }

    /** True only for real ideographs (the things a learner can "know"). */
    public static boolean isKanji(int c) {
        return (c >= 0x4E00 && c <= 0x9FFF)
                || (c >= 0x3400 && c <= 0x4DBF)
                || (c >= 0xF900 && c <= 0xFAFF)
                || (c >= 0x20000 && c <= 0x2FA1F);
    }

    public static boolean isJapanesePunctuation(int c) {
        return (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF01 && c <= 0xFF0F)
                || c == 0xFF1F || c == 0xFF1A || c == 0xFF1B;
    }

    public static boolean isJapanese(int c) {
        return isKana(c) || isKanjiLike(c) || isJapanesePunctuation(c);
    }

    /** True if the string contains any kana or kanji at all. */
    public static boolean containsJapanese(String s) {
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            if (isKana(c) || isKanjiLike(c)) return true;
            i += Character.charCount(c);
        }
        return false;
    }

    /** True if every character is kana (or the long-vowel mark), and the string is non-empty. */
    public static boolean isAllKana(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); ) {
            int c = s.codePointAt(i);
            if (!isKana(c)) return false;
            i += Character.charCount(c);
        }
        return true;
    }
}
