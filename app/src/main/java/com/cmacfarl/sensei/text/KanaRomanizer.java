package com.cmacfarl.sensei.text;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts kana to Hepburn romaji. This exists only so {@link RomajiDetector} can recognise
 * romanized Japanese in model output (e.g. "食べる (taberu)"); nothing romanized is ever shown.
 */
final class KanaRomanizer {

    private static final Map<String, String> TABLE = new HashMap<>();

    static {
        String[][] base = {
                {"あ", "a"}, {"い", "i"}, {"う", "u"}, {"え", "e"}, {"お", "o"},
                {"か", "ka"}, {"き", "ki"}, {"く", "ku"}, {"け", "ke"}, {"こ", "ko"},
                {"が", "ga"}, {"ぎ", "gi"}, {"ぐ", "gu"}, {"げ", "ge"}, {"ご", "go"},
                {"さ", "sa"}, {"し", "shi"}, {"す", "su"}, {"せ", "se"}, {"そ", "so"},
                {"ざ", "za"}, {"じ", "ji"}, {"ず", "zu"}, {"ぜ", "ze"}, {"ぞ", "zo"},
                {"た", "ta"}, {"ち", "chi"}, {"つ", "tsu"}, {"て", "te"}, {"と", "to"},
                {"だ", "da"}, {"ぢ", "ji"}, {"づ", "zu"}, {"で", "de"}, {"ど", "do"},
                {"な", "na"}, {"に", "ni"}, {"ぬ", "nu"}, {"ね", "ne"}, {"の", "no"},
                {"は", "ha"}, {"ひ", "hi"}, {"ふ", "fu"}, {"へ", "he"}, {"ほ", "ho"},
                {"ば", "ba"}, {"び", "bi"}, {"ぶ", "bu"}, {"べ", "be"}, {"ぼ", "bo"},
                {"ぱ", "pa"}, {"ぴ", "pi"}, {"ぷ", "pu"}, {"ぺ", "pe"}, {"ぽ", "po"},
                {"ま", "ma"}, {"み", "mi"}, {"む", "mu"}, {"め", "me"}, {"も", "mo"},
                {"や", "ya"}, {"ゆ", "yu"}, {"よ", "yo"},
                {"ら", "ra"}, {"り", "ri"}, {"る", "ru"}, {"れ", "re"}, {"ろ", "ro"},
                {"わ", "wa"}, {"ゐ", "i"}, {"ゑ", "e"}, {"を", "o"}, {"ん", "n"},
                {"ゔ", "vu"},
                {"ぁ", "a"}, {"ぃ", "i"}, {"ぅ", "u"}, {"ぇ", "e"}, {"ぉ", "o"},
                {"ゃ", "ya"}, {"ゅ", "yu"}, {"ょ", "yo"}, {"ゎ", "wa"},
        };
        for (String[] p : base) TABLE.put(p[0], p[1]);

        // Yōon: consonant + small ya/yu/yo.
        String[][] yoon = {
                {"き", "ky"}, {"ぎ", "gy"}, {"に", "ny"}, {"ひ", "hy"}, {"び", "by"},
                {"ぴ", "py"}, {"み", "my"}, {"り", "ry"},
                {"し", "sh"}, {"じ", "j"}, {"ち", "ch"}, {"ぢ", "j"},
        };
        String[][] smallY = {{"ゃ", "a"}, {"ゅ", "u"}, {"ょ", "o"}};
        for (String[] c : yoon) {
            for (String[] y : smallY) TABLE.put(c[0] + y[0], c[1] + y[1]);
        }
        // Common extended combinations (mostly loanwords).
        String[][] ext = {
                {"しぇ", "she"}, {"じぇ", "je"}, {"ちぇ", "che"},
                {"ふぁ", "fa"}, {"ふぃ", "fi"}, {"ふぇ", "fe"}, {"ふぉ", "fo"},
                {"てぃ", "ti"}, {"でぃ", "di"}, {"とぅ", "tu"}, {"どぅ", "du"},
                {"うぃ", "wi"}, {"うぇ", "we"}, {"うぉ", "wo"},
                {"ゔぁ", "va"}, {"ゔぃ", "vi"}, {"ゔぇ", "ve"}, {"ゔぉ", "vo"},
                {"つぁ", "tsa"}, {"いぇ", "ye"},
        };
        for (String[] p : ext) TABLE.put(p[0], p[1]);
    }

    private KanaRomanizer() {}

    /** Romanizes a kana string, or returns null if it contains anything that isn't kana. */
    static String romanize(String kana) {
        String s = toHiragana(kana);
        StringBuilder out = new StringBuilder();
        boolean geminate = false;
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 'っ') {
                geminate = true;
                i++;
                continue;
            }
            if (c == 'ー') {
                char lastVowel = lastVowel(out);
                if (lastVowel != 0) out.append(lastVowel);
                i++;
                continue;
            }
            String syllable = null;
            if (i + 1 < s.length()) {
                syllable = TABLE.get(s.substring(i, i + 2));
                if (syllable != null) i += 2;
            }
            if (syllable == null) {
                syllable = TABLE.get(String.valueOf(c));
                if (syllable == null) return null;
                i++;
            }
            if (geminate) {
                out.append(syllable.startsWith("ch") ? 't' : syllable.charAt(0));
                geminate = false;
            }
            out.append(syllable);
        }
        return out.toString();
    }

    private static char lastVowel(CharSequence sb) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if ("aiueo".indexOf(c) >= 0) return c;
        }
        return 0;
    }

    static String toHiragana(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x30A1 && c <= 0x30F6) sb.append((char) (c - 0x60));
            else sb.append(c);
        }
        return sb.toString();
    }
}
