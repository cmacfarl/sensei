package com.cmacfarl.sensei.ui;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import com.cmacfarl.sensei.R;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.cmacfarl.sensei.text.JapaneseChars;

/**
 * A TextView for furigana markup. Long-pressing a kanji word reports it, so the student can
 * mark its kanji as known.
 */
public class RubyTextView extends AppCompatTextView {

    public interface OnKanjiWordLongPress {
        void onKanjiWordLongPress(String word);
    }

    private OnKanjiWordLongPress listener;
    private boolean copyOnTap;
    private final GestureDetector gestures;

    public RubyTextView(@NonNull Context context) {
        this(context, null);
    }

    public RubyTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gestures = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent e) {
                return listener != null || copyOnTap;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (!copyOnTap) return false;
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Japanese", getText().toString()));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+ shows its own clipboard confirmation.
                    Toast.makeText(getContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent e) {
                String word = kanjiWordAt(e.getX(), e.getY());
                if (word != null && listener != null) {
                    performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    listener.onKanjiWordLongPress(word);
                }
            }
        });
    }

    public void setMarkup(String markup, boolean japaneseOnly, RubyRenderer.Options options) {
        setText(RubyRenderer.render(markup, japaneseOnly, options, getTextSize()));
    }

    public void setOnKanjiWordLongPress(OnKanjiWordLongPress listener) {
        this.listener = listener;
    }

    /** Tap copies the plain Japanese (no readings) to the clipboard. */
    public void setCopyOnTap(boolean copyOnTap) {
        this.copyOnTap = copyOnTap;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = gestures.onTouchEvent(event);
        return super.onTouchEvent(event) || handled;
    }

    /** The ruby word, or run of kanji, under the given point; null if there's no kanji there. */
    private String kanjiWordAt(float x, float y) {
        CharSequence text = getText();
        if (getLayout() == null || text.length() == 0) return null;
        int offset = getOffsetForPosition(x, y);
        if (text instanceof Spanned) {
            Spanned spanned = (Spanned) text;
            for (int probe : new int[]{offset, offset - 1}) {
                if (probe < 0) continue;
                for (RubySpan span : spanned.getSpans(probe, probe + 1, RubySpan.class)) {
                    if (spanned.getSpanStart(span) <= probe && probe < spanned.getSpanEnd(span)) {
                        return span.base;
                    }
                }
            }
        }
        // A kanji with no reading (e.g. the model left one unannotated).
        String s = text.toString();
        for (int probe : new int[]{offset, offset - 1}) {
            if (probe < 0 || probe >= s.length() || !JapaneseChars.isKanjiLike(s.codePointAt(probe))) continue;
            int start = probe;
            int end = probe;
            while (start > 0 && JapaneseChars.isKanjiLike(s.codePointBefore(start))) {
                start -= Character.charCount(s.codePointBefore(start));
            }
            while (end < s.length() && JapaneseChars.isKanjiLike(s.codePointAt(end))) {
                end += Character.charCount(s.codePointAt(end));
            }
            return s.substring(start, end);
        }
        return null;
    }
}
