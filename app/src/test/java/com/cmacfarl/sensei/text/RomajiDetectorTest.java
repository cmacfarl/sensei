package com.cmacfarl.sensei.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class RomajiDetectorTest {

    private static void leaks(String text, String expected) {
        List<String> hits = RomajiDetector.findInEnglish(text);
        assertTrue("expected \"" + expected + "\" in " + hits + " for: " + text,
                hits.stream().anyMatch(h -> h.equalsIgnoreCase(expected)));
    }

    private static void clean(String text) {
        List<String> hits = RomajiDetector.findInEnglish(text);
        assertTrue("false positive " + hits + " for: " + text, hits.isEmpty());
    }

    // --- Things that must be caught ---

    @Test
    public void romanizedGrammarTerms() {
        leaks("Use the te-form to link actions.", "te-form");
        leaks("This is an i-adjective.", "i-adjective");
        leaks("Take the masu stem and add たい.", "masu stem");
        leaks("It's a ru-verb, so drop る.", "ru-verb");
        leaks("Conjugate it as a u verb.", "u verb");
    }

    @Test
    public void romanizedParticlesAndWords() {
        leaks("The particle wa marks the topic.", "wa");
        leaks("End with desu for politeness.", "desu");
        leaks("Watashi is the neutral word for I.", "Watashi");
        leaks("ga marks the subject", "ga");
    }

    @Test
    public void romanizedInflections() {
        leaks("You could also say tabemashita here.", "tabemashita");
        leaks("The negative is ikanakatta.", "ikanakatta");
        leaks("Then you get yomitai.", "yomitai");
    }

    @Test
    public void macrons() {
        leaks("He went to Tōkyō last week.", "Tōkyō");
        leaks("Say kōhī.", "kōhī");
    }

    @Test
    public void readingNextToTheJapaneseItSpells() {
        leaks("食[た]べる (taberu) means to eat.", "taberu");
        leaks("taberu (食[た]べる) means to eat.", "taberu");
        leaks("学校[がっこう] gakkou is school.", "gakkou");
        leaks("コーヒー (koohii)", "koohii");
        leaks("東京[とうきょう] (Tokyo)", "Tokyo");
        leaks("は (wa) marks the topic.", "wa");
        leaks("と (to) means and.", "to");
        leaks("を (o) marks the object.", "o");
        leaks("抹茶[まっちゃ] (matcha) is green tea.", "matcha");
    }

    @Test
    public void romanReadingInsideFuriganaBrackets() {
        leaks("食[ta]べる", "ta");
    }

    // --- Normal English explanations must pass ---

    @Test
    public void ordinaryEnglishIsClean() {
        clean("Use と to connect nouns, like 'and'.");
        clean("No, this is not quite right. Do you see why?");
        clean("The ます-stem plus に before a movement verb shows purpose.");
        clean("In casual speech, people often drop the particle を.");
        clean("Tanaka went to Tokyo with his friend.");
        clean("This one is a name, the same as before; make sure to take time.");
        clean("I made a demo of the node at home.");
        clean("The kanji 日 has many readings; furigana shows the one used here.");
        clean("い-adjectives conjugate on their own, な-adjectives need だ.");
        clean("Keigo is not needed here. A sushi chef might say it differently.");
        clean("It sounds natural and polite. Nice work!");
        clean("に marks the destination; へ marks direction.");
        clean("The casual version uses 行[い]った instead of 行[い]きました.");
        clean("Remove the ね at the end if you don't need agreement.");
        clean("Use で to mark where an action happens, e.g. 図書館[としょかん]で.");
        clean("Native speakers would usually put the time first.");
    }

    @Test
    public void japaneseFieldsFlagAnyRomanizedWord() {
        assertFalse(RomajiDetector.findInJapanese("watashi wa 学生[がくせい]です").isEmpty());
        assertTrue(RomajiDetector.findInJapanese("Tシャツを買[か]いました。").isEmpty());
        assertTrue(RomajiDetector.findInJapanese("CDを聞[き]く。").isEmpty());
        assertTrue(RomajiDetector.findInJapanese("昨日[きのう]、図書館[としょかん]に行[い]った。").isEmpty());
    }

    @Test
    public void hepburnSegmentation() {
        assertTrue(RomajiDetector.isHepburn("taberu"));
        assertTrue(RomajiDetector.isHepburn("gakkou"));
        assertTrue(RomajiDetector.isHepburn("shinbun"));
        assertTrue(RomajiDetector.isHepburn("matcha"));
        assertTrue(RomajiDetector.isHepburn("kon'nichiwa"));
        assertFalse(RomajiDetector.isHepburn("street"));
        assertFalse(RomajiDetector.isHepburn("polite"));
        assertFalse(RomajiDetector.isHepburn("casual"));
    }

    @Test
    public void normalizationIgnoresLongVowelSpelling() {
        assertEquals(RomajiDetector.normalize("toukyou"), RomajiDetector.normalize("Tōkyō"));
        assertEquals(RomajiDetector.normalize("shinbun"), RomajiDetector.normalize("shimbun"));
    }
}
