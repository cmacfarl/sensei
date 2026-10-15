package com.cmacfarl.sensei.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.cmacfarl.sensei.R;
import com.cmacfarl.sensei.text.JapaneseChars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Kanji the student already knows. Furigana is hidden on any word whose kanji are all known.
 * Seeded from res/raw/starter_kanji.txt on first run; stored as one string in preferences.
 */
public final class KnownKanji {

    private static final String KEY = "known_kanji";

    private static KnownKanji instance;

    private final Context context;
    private final SharedPreferences prefs;
    private final Set<Integer> known = new LinkedHashSet<>();

    public static synchronized KnownKanji get(Context context) {
        if (instance == null) instance = new KnownKanji(context.getApplicationContext());
        return instance;
    }

    private KnownKanji(Context context) {
        this.context = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String stored = prefs.getString(KEY, null);
        if (stored == null) {
            replaceAll(readStarterList());
        } else {
            known.addAll(extract(stored));
        }
    }

    public synchronized boolean isKnown(int codePoint) {
        return known.contains(codePoint);
    }

    /** True if every real kanji in {@code word} is known (iteration marks etc. don't count). */
    public synchronized boolean allKnown(String word) {
        boolean any = false;
        for (int i = 0; i < word.length(); ) {
            int c = word.codePointAt(i);
            if (JapaneseChars.isKanji(c)) {
                any = true;
                if (!known.contains(c)) return false;
            }
            i += Character.charCount(c);
        }
        return any;
    }

    public synchronized void setKnown(int codePoint, boolean isKnown) {
        if (isKnown) known.add(codePoint);
        else known.remove(codePoint);
        save();
    }

    /** All known kanji as one string, in the order they were added. */
    public synchronized String asString() {
        StringBuilder sb = new StringBuilder();
        for (int c : known) sb.appendCodePoint(c);
        return sb.toString();
    }

    public synchronized int count() {
        return known.size();
    }

    /** Replace the whole set with the kanji found in {@code text} (anything else is ignored). */
    public synchronized void replaceAll(String text) {
        known.clear();
        known.addAll(extract(text));
        save();
    }

    public synchronized void resetToStarterList() {
        replaceAll(readStarterList());
    }

    private void save() {
        prefs.edit().putString(KEY, asString()).apply();
    }

    static Set<Integer> extract(String text) {
        Set<Integer> out = new LinkedHashSet<>();
        for (int i = 0; i < text.length(); ) {
            int c = text.codePointAt(i);
            if (JapaneseChars.isKanji(c)) out.add(c);
            i += Character.charCount(c);
        }
        return out;
    }

    private String readStarterList() {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                context.getResources().openRawResource(R.raw.starter_kanji), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.startsWith("#")) sb.append(line);
            }
        } catch (IOException e) {
            return "";
        }
        return sb.toString();
    }

    public Set<Integer> snapshot() {
        synchronized (this) {
            return Collections.unmodifiableSet(new LinkedHashSet<>(known));
        }
    }
}
