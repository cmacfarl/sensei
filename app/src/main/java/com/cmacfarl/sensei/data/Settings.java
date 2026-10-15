package com.cmacfarl.sensei.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.cmacfarl.sensei.BuildConfig;
import com.cmacfarl.sensei.llm.ClaudeClient;
import com.cmacfarl.sensei.llm.DemoClient;
import com.cmacfarl.sensei.llm.GeminiClient;
import com.cmacfarl.sensei.llm.LlmClient;
import com.cmacfarl.sensei.llm.LlmException;

/**
 * App settings. Debug builds can carry API keys from local.properties (git-ignored); a key
 * entered on the Settings screen overrides it. Entered keys are stored in the app's private
 * preferences, which are excluded from backups.
 */
public final class Settings {

    public static final String PROVIDER = "provider";
    public static final String GEMINI_KEY = "gemini_api_key";
    public static final String GEMINI_MODEL = "gemini_model";
    public static final String GEMINI_THINKING = "gemini_thinking";
    public static final String CLAUDE_KEY = "claude_api_key";
    public static final String CLAUDE_MODEL = "claude_model";
    public static final String SHOW_FURIGANA = "show_furigana";

    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_CLAUDE = "claude";

    public static final String DEFAULT_GEMINI_MODEL = "gemini-3.8-flash";
    public static final String DEFAULT_CLAUDE_MODEL = "claude-opus-5-5";

    /** Debug builds only: an API key of "demo" returns canned replies, for trying the UI offline. */
    public static final String DEMO_KEY = "demo";

    private final SharedPreferences prefs;

    public Settings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public String provider() {
        return prefs.getString(PROVIDER, PROVIDER_GEMINI);
    }

    public String modelFor(String provider) {
        String model = PROVIDER_CLAUDE.equals(provider)
                ? prefs.getString(CLAUDE_MODEL, DEFAULT_CLAUDE_MODEL)
                : prefs.getString(GEMINI_MODEL, DEFAULT_GEMINI_MODEL);
        if (model == null || model.trim().isEmpty()) {
            return PROVIDER_CLAUDE.equals(provider) ? DEFAULT_CLAUDE_MODEL : DEFAULT_GEMINI_MODEL;
        }
        return model.trim();
    }

    /** Gemini thinking level: "low", "medium" or "high". */
    public String geminiThinkingLevel() {
        String level = prefs.getString(GEMINI_THINKING, "medium");
        return level == null || level.isEmpty() ? "medium" : level;
    }

    public boolean showFurigana() {
        return prefs.getBoolean(SHOW_FURIGANA, true);
    }

    public void setShowFurigana(boolean show) {
        prefs.edit().putBoolean(SHOW_FURIGANA, show).apply();
    }

    /** The key typed into Settings, or "" if none. */
    public String enteredKey(String prefKey) {
        String key = prefs.getString(prefKey, "");
        return key == null ? "" : key.trim();
    }

    /** The key built into debug builds from local.properties, or "" (always "" in release). */
    public static String builtInKey(String prefKey) {
        return CLAUDE_KEY.equals(prefKey) ? BuildConfig.CLAUDE_API_KEY : BuildConfig.GEMINI_API_KEY;
    }

    /** A key entered in Settings wins; otherwise the built-in debug key, if any. */
    public String effectiveKey(String prefKey) {
        String entered = enteredKey(prefKey);
        return entered.isEmpty() ? builtInKey(prefKey) : entered;
    }

    /** A client for the given provider and model, using the effective key. */
    public LlmClient clientFor(String provider, String model) throws LlmException {
        boolean claude = PROVIDER_CLAUDE.equals(provider);
        String key = effectiveKey(claude ? CLAUDE_KEY : GEMINI_KEY);
        if (key.isEmpty()) {
            throw new LlmException("No " + (claude ? "Claude" : "Gemini")
                    + " API key yet. Add one in Settings, or to local.properties for debug builds.", false);
        }
        if (BuildConfig.DEBUG && DEMO_KEY.equals(key)) return new DemoClient();
        return claude ? new ClaudeClient(key, model) : new GeminiClient(key, model, geminiThinkingLevel());
    }
}
