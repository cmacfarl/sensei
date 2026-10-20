package com.cmacfarl.sensei.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DictionaryLookupTest {

    /** Lookup text for the first occurrence of {@code kanji} in {@code sentence}. */
    private static String lookup(String sentence, String kanji) {
        int start = sentence.indexOf(kanji);
        return DictionaryLookup.forWord(sentence, start, start + kanji.length());
    }

    @Test
    public void stopsBeforeGrammarAfterTheVerb() {
        assertEquals("行った", lookup("風邪をひいていたら、コンサートに行ったほうがいいですか。", "行"));
        assertEquals("風邪", lookup("風邪をひいていたら、コンサートに行ったほうがいいですか。", "風邪"));
    }

    @Test
    public void particlesRightAfterANoun() {
        String s = "昨日、図書館へ勉強しに行きました。";
        assertEquals("昨日", lookup(s, "昨日"));
        assertEquals("図書館", lookup(s, "図書館"));
        assertEquals("勉強し", lookup(s, "勉強"));
        assertEquals("行きました", lookup(s, "行"));
        assertEquals("日本", lookup("日本が好きです。", "日本"));
        assertEquals("学校", lookup("学校に行く。", "学校"));
        assertEquals("駅", lookup("駅で待つ。", "駅"));
    }

    @Test
    public void keepsConjugations() {
        assertEquals("美味しくなかった", lookup("美味しくなかった。", "美味"));
        assertEquals("食べさせられませんでした", lookup("食べさせられませんでした。", "食"));
        assertEquals("行かなければならない", lookup("行かなければならない。", "行"));
        assertEquals("高かった", lookup("高かったです。", "高"));
    }

    @Test
    public void teFormIsNotTheParticleDe() {
        assertEquals("読んで", lookup("静かに読んでください。", "読"));
        assertEquals("泳いでいる", lookup("海で泳いでいる。", "泳"));
        assertEquals("書いておいて", lookup("書いておいてね。", "書"));
    }

    @Test
    public void okuriganaThatLooksLikeAParticle() {
        assertEquals("静か", lookup("静かに読んでください。", "静"));
        assertEquals("確か", lookup("確かに難しいですね。", "確"));
        assertEquals("難しい", lookup("確かに難しいですね。", "難"));
        assertEquals("上がった", lookup("値段が上がった。", "上"));
        assertEquals("行きます", lookup("行きますか。", "行"));
        assertEquals("食べたかった", lookup("食べたかったよ。", "食"));
    }

    @Test
    public void stopsAtNonHiragana() {
        assertEquals("新しい", lookup("新しいパン", "新"));
        assertEquals("猫", lookup("猫", "猫"));
        assertEquals("来た", lookup("一ヶ月前に日本へ来た。", "来"));
    }
}
