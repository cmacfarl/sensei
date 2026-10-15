package com.cmacfarl.sensei.ui;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

import com.cmacfarl.sensei.data.KnownKanji;
import com.cmacfarl.sensei.text.FuriganaParser;

import java.util.Collection;

/** Turns furigana markup into styled text with {@link RubySpan}s. */
public final class RubyRenderer {

    public static final class Options {
        final boolean showFurigana;
        final KnownKanji known;
        final Collection<String> highlights;
        final int highlightColor;

        public Options(boolean showFurigana, KnownKanji known, Collection<String> highlights, int highlightColor) {
            this.showFurigana = showFurigana;
            this.known = known;
            this.highlights = highlights;
            this.highlightColor = highlightColor;
        }
    }

    private RubyRenderer() {}

    /**
     * @param japaneseOnly the markup is a Japanese sentence (strip stray spaces)
     * @param textSizePx   the target TextView's text size, for reserving furigana line height
     */
    public static CharSequence render(String markup, boolean japaneseOnly, Options options, float textSizePx) {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        boolean anyReading = false;
        for (FuriganaParser.Token t : FuriganaParser.parse(markup == null ? "" : markup, japaneseOnly).tokens) {
            if (!t.isRuby()) {
                sb.append(t.base);
                continue;
            }
            boolean show = options.showFurigana && !options.known.allKnown(t.base);
            anyReading |= show;
            int start = sb.length();
            sb.append(t.base);
            sb.setSpan(new RubySpan(t.base, t.reading, show), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (options.highlights != null) {
            String plain = sb.toString();
            for (String h : options.highlights) {
                if (h.isEmpty()) continue;
                for (int i = plain.indexOf(h); i >= 0; i = plain.indexOf(h, i + h.length())) {
                    sb.setSpan(new BackgroundColorSpan(options.highlightColor), i, i + h.length(),
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }

        if (anyReading) {
            sb.setSpan(new RubyLineHeightSpan(textSizePx), 0, sb.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }
        return sb;
    }
}
