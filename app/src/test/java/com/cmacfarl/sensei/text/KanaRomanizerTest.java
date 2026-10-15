package com.cmacfarl.sensei.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class KanaRomanizerTest {

    @Test
    public void romanizesHepburn() {
        assertEquals("taberu", KanaRomanizer.romanize("たべる"));
        assertEquals("gakkou", KanaRomanizer.romanize("がっこう"));
        assertEquals("kyou", KanaRomanizer.romanize("きょう"));
        assertEquals("matcha", KanaRomanizer.romanize("まっちゃ"));
        assertEquals("shinbun", KanaRomanizer.romanize("しんぶん"));
        assertEquals("koohii", KanaRomanizer.romanize("コーヒー"));
        assertEquals("fairu", KanaRomanizer.romanize("ファイル"));
    }

    @Test
    public void rejectsNonKana() {
        assertNull(KanaRomanizer.romanize("食べる"));
    }
}
