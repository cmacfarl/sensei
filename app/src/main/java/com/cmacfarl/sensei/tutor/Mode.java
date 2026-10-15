package com.cmacfarl.sensei.tutor;

import com.cmacfarl.sensei.text.JapaneseChars;

/** What the tutor is being asked to do with a message. */
public enum Mode {
    /** English in, Japanese (polite + casual) with a grammar breakdown out. */
    TRANSLATE,
    /** The student's Japanese in, a teacher's verdict and corrections out. */
    CHECK,
    /** A question about the current thread. */
    FOLLOW_UP;

    /** Mode for the first message of a thread: any kana or kanji means the student wrote Japanese. */
    public static Mode forNewSentence(String text) {
        return JapaneseChars.containsJapanese(text) ? CHECK : TRANSLATE;
    }
}
