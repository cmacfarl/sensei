package com.cmacfarl.sensei.text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds romanized Japanese in model output. Explanations are in English, so Latin text is
 * expected; the rules here aim at high-signal leaks and deliberately avoid flagging short
 * romaji that are also English words (no, to, de, mo...).
 */
public final class RomajiDetector {

    private static final String LATIN = "A-Za-z\\u00C0-\\u017F";
    private static final Pattern WORD = Pattern.compile("[" + LATIN + "]+(?:['’][" + LATIN + "]+)?");

    /** Grammar terms that should be written with kana: te-form → て-form, i-adjective → い-adjective. */
    private static final Pattern GRAMMAR_TERM = Pattern.compile(
            "\\b(te|de|ta|da|masu|nai|naide|ru|u|i|na|tai|ba|eba|tara|ou|you|reru|rareru|seru|saseru)"
                    + "(-|\\s)(form|forms|stem|stems|verb|verbs|adjective|adjectives|adj|conjugation|"
                    + "ending|endings|base|group|type)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Romanized words that are not English, so a whole-word hit is a leak. */
    private static final Set<String> BLOCKLIST = new HashSet<>(Arrays.asList(
            "wa", "ga", "wo", "ka", "ni", "ne", "yo", "kara", "yori", "dake", "shika", "nado",
            "desu", "deshita", "deshou", "desho", "darou", "masu", "mashita", "masen", "mashou",
            "imasu", "arimasu", "gozaimasu", "kudasai", "onegai", "onegaishimasu", "sumimasen",
            "arigatou", "arigato", "konnichiwa", "ohayou", "ohayo", "watashi", "boku", "anata",
            "kore", "dore", "kono", "sono", "nani", "doko", "itsu", "suru", "shimasu", "shita",
            "shite", "kuru", "kimasu", "iku", "ikimasu", "taberu", "tabemasu", "nomu", "nomimasu",
            "miru", "mimasu", "iru", "aru", "nai", "janai", "jyanai", "dewa", "dakara", "keredo",
            "kedo", "iie", "soudesu", "naru", "sugoi", "kawaii", "oishii", "nihongo", "gakusei",
            "teiru", "teimasu", "nakatta", "ikanai", "tabenai"));

    /** Inflectional endings that only occur on romanized Japanese words. */
    private static final String[] JAPANESE_ENDINGS = {
            "masu", "mashita", "masen", "mashou", "desu", "deshita", "nakatta", "teiru", "teimasu",
            "nai", "tai", "reru", "rareru", "saseru", "kunai", "kute", "tara"};

    /** English words borrowed from Japanese; never flag these. */
    private static final Set<String> ALLOWLIST = new HashSet<>(Arrays.asList(
            "kanji", "kana", "hiragana", "katakana", "furigana", "okurigana", "romaji", "keigo",
            "jlpt", "anime", "manga", "sushi", "tsunami", "karaoke", "kimono", "samurai", "ninja",
            "emoji", "ramen", "tofu", "sake", "sensei", "kunyomi", "onyomi", "kun'yomi", "on'yomi",
            "kun’yomi", "on’yomi", "kaomoji", "bonsai", "origami", "haiku", "judo", "karate",
            "tempura", "wasabi", "senpai", "kohai", "otaku", "kawaii", "ichidan", "godan"));

    private static final Set<String> SYLLABLES = new HashSet<>();

    static {
        String[] onsets = {"", "k", "g", "s", "z", "t", "d", "n", "h", "b", "p", "m", "y", "r", "w"};
        for (String c : onsets) for (char v : "aiueo".toCharArray()) SYLLABLES.add(c + v);
        SYLLABLES.addAll(Arrays.asList("shi", "chi", "tsu", "fu", "ji", "n"));
        for (String c : new String[]{"ky", "gy", "ny", "hy", "by", "py", "my", "ry", "sh", "ch", "j"}) {
            for (char v : "auo".toCharArray()) SYLLABLES.add(c + v);
        }
        SYLLABLES.addAll(Arrays.asList("she", "che", "je", "fa", "fi", "fe", "fo"));
        // Not Hepburn: these make plenty of English words fail the check, which is what we want.
        SYLLABLES.removeAll(Arrays.asList("si", "ti", "tu", "hu", "zi", "di", "du", "yi", "ye", "wu", "wi", "we"));
    }

    private RomajiDetector() {}

    /** Check text whose prose is English (explanations, notes). */
    public static List<String> findInEnglish(String text) {
        return find(text, false);
    }

    /** Check a field that should be Japanese only (sentences, corrections). */
    public static List<String> findInJapanese(String text) {
        return find(text, true);
    }

    private static List<String> find(String text, boolean japaneseOnly) {
        Set<String> hits = new LinkedHashSet<>();

        hits.addAll(FuriganaParser.parse(text, false).invalidReadings);

        Matcher g = GRAMMAR_TERM.matcher(text);
        while (g.find()) hits.add(g.group());

        Matcher m = WORD.matcher(text);
        while (m.find()) {
            String word = m.group();
            String lower = word.toLowerCase(Locale.ROOT);
            if (ALLOWLIST.contains(lower)) continue;
            if (hasMacron(lower)) {
                hits.add(word);
            } else if (BLOCKLIST.contains(lower)) {
                hits.add(word);
            } else if (lower.length() >= 5 && endsWithJapaneseInflection(lower) && isHepburn(lower)) {
                hits.add(word);
            } else if (japaneseOnly && word.equals(lower) && lower.length() >= 2 && isHepburn(lower)) {
                hits.add(word);
            }
        }

        hits.addAll(findReadingEchoes(text));
        return new ArrayList<>(hits);
    }

    // --- Romaji placed next to the Japanese it spells: 食べる (taberu), taberu (食べる) ---

    private static final Pattern JAPANESE_RUN = Pattern.compile(
            "[\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}々〆ヶ]+"
                    + "(?:\\[[^\\]\\s]*\\][\\p{InHiragana}\\p{InKatakana}\\p{InCJKUnifiedIdeographs}々〆ヶ]*)*");
    private static final String SEP = "[\\s()（）\"'“”‘’:=/→\\-–—]*";
    private static final Pattern LATIN_PHRASE = Pattern.compile(
            "[" + LATIN + "']+(?:[\\s\\-][" + LATIN + "']+){0,5}");

    private static List<String> findReadingEchoes(String text) {
        List<String> hits = new ArrayList<>();
        Matcher run = JAPANESE_RUN.matcher(text);
        while (run.find()) {
            String reading = readingOf(run.group());
            if (reading == null) continue;
            Set<String> targets = new HashSet<>();
            String romanized = KanaRomanizer.romanize(reading);
            if (romanized != null) targets.add(normalize(romanized));
            // Particles are romanized by pronunciation, not spelling.
            if (reading.equals("は")) targets.add("wa");
            if (reading.equals("へ")) targets.add("e");
            if (reading.equals("を")) targets.add("o");
            targets.remove("");
            if (targets.isEmpty()) continue;

            // Latin words right after the run.
            Matcher after = Pattern.compile("^(" + SEP + ")(" + LATIN_PHRASE.pattern() + ")")
                    .matcher(text.substring(run.end()));
            if (after.find()) checkPrefixes(after.group(2), after.group(1), targets, true, hits);

            // Latin words right before the run.
            Matcher before = Pattern.compile("(" + LATIN_PHRASE.pattern() + ")(" + SEP + ")$")
                    .matcher(text.substring(0, run.start()));
            if (before.find()) checkPrefixes(before.group(1), before.group(2), targets, false, hits);
        }
        return hits;
    }

    /** Try word sequences growing away from the Japanese run (first words after, last words before). */
    private static void checkPrefixes(String phrase, String separator, Set<String> targets,
                                      boolean fromStart, List<String> hits) {
        // Short romaji (to, no, de) collide with English ("と to connect"), so only count them
        // when set off as a gloss: と (to), "no".
        boolean glossed = separator.matches(".*[(（\"“‘'].*");
        String[] words = phrase.trim().split("[\\s\\-]+");
        for (int n = 1; n <= words.length; n++) {
            String[] slice = fromStart
                    ? Arrays.copyOfRange(words, 0, n)
                    : Arrays.copyOfRange(words, words.length - n, words.length);
            String joined = String.join(" ", slice);
            String norm = normalize(joined);
            if (!norm.isEmpty() && (norm.length() > 2 || glossed) && targets.contains(norm)
                    && !ALLOWLIST.contains(joined.toLowerCase(Locale.ROOT))) {
                hits.add(joined);
                return;
            }
        }
    }

    /** Kana reading of a Japanese run with furigana markup, or null if some kanji has no reading. */
    static String readingOf(String run) {
        StringBuilder sb = new StringBuilder();
        for (FuriganaParser.Token t : FuriganaParser.parse(run, false).tokens) {
            if (t.isRuby()) {
                sb.append(t.reading);
            } else {
                for (int i = 0; i < t.base.length(); ) {
                    int c = t.base.codePointAt(i);
                    if (JapaneseChars.isKanjiLike(c)) return null;
                    if (JapaneseChars.isKana(c)) sb.appendCodePoint(c);
                    i += Character.charCount(c);
                }
            }
        }
        return sb.toString();
    }

    /** Collapses spelling variants (long vowels, macrons, n/m before b/p) so romaji compares loosely. */
    static String normalize(String s) {
        String t = s.toLowerCase(Locale.ROOT)
                .replace('ā', 'a').replace('ī', 'i').replace('ū', 'u').replace('ē', 'e').replace('ō', 'o')
                .replace('â', 'a').replace('î', 'i').replace('û', 'u').replace('ê', 'e').replace('ô', 'o')
                .replaceAll("[^a-z]", "")
                .replace("cch", "tch")
                .replaceAll("m(?=[bp])", "n")
                .replace("nn", "n");
        String prev;
        do {
            prev = t;
            t = t.replace("ou", "o").replace("oo", "o").replace("uu", "u").replace("aa", "a")
                    .replace("ii", "i").replace("ee", "e").replace("ei", "e");
        } while (!t.equals(prev));
        return t;
    }

    private static boolean hasMacron(String s) {
        for (char c : s.toCharArray()) {
            if ("āīūēōâîûêô".indexOf(c) >= 0) return true;
        }
        return false;
    }

    private static boolean endsWithJapaneseInflection(String s) {
        for (String e : JAPANESE_ENDINGS) {
            if (s.endsWith(e) && s.length() > e.length()) return true;
        }
        return false;
    }

    /** True if the word splits completely into Hepburn syllables (with geminate consonants). */
    static boolean isHepburn(String word) {
        String w = word.replace("'", "").replace("’", "");
        boolean[] ok = new boolean[w.length() + 1];
        ok[0] = true;
        for (int i = 0; i < w.length(); i++) {
            if (!ok[i]) continue;
            // Geminate: a doubled consonant (or "tch") means a small っ.
            if (i + 1 < w.length() && w.charAt(i) == w.charAt(i + 1) && "kgsztdbpmhrjfc".indexOf(w.charAt(i)) >= 0) {
                ok[i + 1] = true;
            }
            if (w.startsWith("tch", i)) ok[i + 1] = true;
            for (int len = 1; len <= 3 && i + len <= w.length(); len++) {
                String syl = w.substring(i, i + len);
                if (!SYLLABLES.contains(syl)) continue;
                // Syllabic n may not be followed by a vowel or y (that would be な-row).
                if (syl.equals("n") && i + 1 < w.length() && "aiueoy".indexOf(w.charAt(i + 1)) >= 0) continue;
                ok[i + len] = true;
            }
        }
        return ok[w.length()];
    }
}
