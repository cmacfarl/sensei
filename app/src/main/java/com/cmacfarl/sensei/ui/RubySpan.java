package com.cmacfarl.sensei.ui;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;
import android.util.TypedValue;

/**
 * Draws a kanji word with its reading centred above it (ruby), the way furigana is printed.
 * The span covers the base text, so copying or measuring text never includes the reading.
 * Line height is reserved by {@link RubyLineHeightSpan}.
 */
public final class RubySpan extends ReplacementSpan {

    static final float RUBY_SCALE = 0.5f;
    /** Furigana is half the base size, but never smaller than this, so it stays legible on body text. */
    static final float MIN_RUBY_PX = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP, 9f, Resources.getSystem().getDisplayMetrics());

    static float rubyTextSize(float baseTextSize) {
        return Math.max(baseTextSize * RUBY_SCALE, MIN_RUBY_PX);
    }

    public final String base;
    public final String reading;
    private final boolean showReading;
    private final TextPaint rubyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    public RubySpan(String base, String reading, boolean showReading) {
        this.base = base;
        this.reading = reading;
        this.showReading = showReading;
    }

    public boolean isReadingShown() {
        return showReading;
    }

    private void syncRubyPaint(Paint paint) {
        rubyPaint.set(paint);
        rubyPaint.setTextSize(rubyTextSize(paint.getTextSize()));
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        if (fm != null) paint.getFontMetricsInt(fm);
        float width = paint.measureText(base);
        if (showReading) {
            syncRubyPaint(paint);
            width = Math.max(width, rubyPaint.measureText(reading));
        }
        return (int) Math.ceil(width);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y,
                     int bottom, Paint paint) {
        float baseWidth = paint.measureText(base);
        float width = baseWidth;
        if (showReading) {
            syncRubyPaint(paint);
            float rubyWidth = rubyPaint.measureText(reading);
            width = Math.max(baseWidth, rubyWidth);
            float rubyBaseline = y + paint.ascent() - rubyPaint.descent();
            canvas.drawText(reading, x + (width - rubyWidth) / 2f, rubyBaseline, rubyPaint);
        }
        canvas.drawText(base, x + (width - baseWidth) / 2f, y, paint);
    }
}
