package com.cmacfarl.sensei.ui;

import android.graphics.Paint;
import android.text.Spanned;
import android.text.style.LineHeightSpan;

/** Adds room above any line that shows furigana, so readings never collide with the line above. */
final class RubyLineHeightSpan implements LineHeightSpan {

    private final int extra;

    RubyLineHeightSpan(float textSizePx) {
        Paint p = new Paint();
        p.setTextSize(RubySpan.rubyTextSize(textSizePx));
        Paint.FontMetrics m = p.getFontMetrics();
        extra = (int) Math.ceil(m.descent - m.ascent);
    }

    @Override
    public void chooseHeight(CharSequence text, int start, int end, int spanstartv, int lineHeight,
                             Paint.FontMetricsInt fm) {
        if (!(text instanceof Spanned) || !lineShowsReading((Spanned) text, start, end)) return;
        fm.ascent -= extra;
        fm.top -= extra;
    }

    private static boolean lineShowsReading(Spanned text, int start, int end) {
        for (RubySpan span : text.getSpans(start, end, RubySpan.class)) {
            if (span.isReadingShown() && text.getSpanStart(span) < end && text.getSpanEnd(span) > start) {
                return true;
            }
        }
        return false;
    }
}
