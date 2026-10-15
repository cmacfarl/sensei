package com.cmacfarl.sensei.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class FuriganaParserTest {

    private static String tokens(String markup, boolean strip) {
        List<FuriganaParser.Token> t = FuriganaParser.parse(markup, strip).tokens;
        return t.stream().map(FuriganaParser.Token::toString).collect(Collectors.joining("|"));
    }

    @Test
    public void readingCoversOnlyKanjiAndOkuriganaStaysOutside() {
        assertEquals("食[た]|べる", tokens("食[た]べる", false));
        assertEquals("昨日[きのう]|、|図書館[としょかん]|へ|行[い]|きました。",
                tokens("昨日[きのう]、図書館[としょかん]へ行[い]きました。", false));
    }

    @Test
    public void specialReadingsAndIterationMark() {
        assertEquals("今日[きょう]|は", tokens("今日[きょう]は", false));
        assertEquals("時々[ときどき]", tokens("時々[ときどき]", false));
        assertEquals("一ヶ月[いっかげつ]", tokens("一ヶ月[いっかげつ]", false));
    }

    @Test
    public void plainTextHasNoMarkup() {
        FuriganaParser.Result r = FuriganaParser.parse("私[わたし]は学生[がくせい]です。", false);
        assertEquals("私は学生です。", r.plainText());
    }

    @Test
    public void englishWithBracketsIsUntouched() {
        assertEquals("Use [this] form", tokens("Use [this] form", false));
        assertEquals("The particle は marks the topic.", tokens("The particle は marks the topic.", false));
    }

    @Test
    public void markupInsideEnglish() {
        assertEquals("The て-form of |食[た]|べる is |食[た]|べて.",
                tokens("The て-form of 食[た]べる is 食[た]べて.", false));
    }

    @Test
    public void fullWidthBracketsAccepted() {
        assertEquals("食[た]|べる", tokens("食［た］べる", false));
    }

    @Test
    public void redundantReadingOnKanaIsDropped() {
        assertEquals("ねこが", tokens("ねこ[ねこ]が", false));
    }

    @Test
    public void romanReadingIsReportedAndLeftVisible() {
        FuriganaParser.Result r = FuriganaParser.parse("食[ta]べる", false);
        assertEquals(List.of("ta"), r.invalidReadings);
        assertTrue(r.plainText().contains("[ta]"));
    }

    @Test
    public void ankiStyleSpacesStrippedInJapaneseFields() {
        assertEquals("私[わたし]|は|学生[がくせい]|です", tokens("私[わたし]は 学生[がくせい]です", true));
        // ...but spaces in English are kept.
        assertEquals("go to |学校[がっこう]", tokens("go to 学校[がっこう]", true));
    }

    @Test
    public void surrogatePairKanji() {
        assertEquals("𠮟[しか]|る", tokens("𠮟[しか]る", false));
    }
}
