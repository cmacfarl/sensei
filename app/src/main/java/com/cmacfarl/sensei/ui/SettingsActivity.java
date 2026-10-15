package com.cmacfarl.sensei.ui;

import android.os.Bundle;
import android.text.InputType;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.cmacfarl.sensei.R;
import com.cmacfarl.sensei.data.Settings;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
            setUpKey(Settings.GEMINI_KEY);
            setUpKey(Settings.CLAUDE_KEY);
        }

        /** Mask the key while editing and never show it in the summary. */
        private void setUpKey(String key) {
            EditTextPreference pref = findPreference(key);
            if (pref == null) return;
            pref.setOnBindEditTextListener(edit -> {
                edit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                edit.setSingleLine(true);
            });
            pref.setSummaryProvider(p -> {
                String v = ((EditTextPreference) p).getText();
                if (v != null && !v.trim().isEmpty()) return getString(R.string.key_set);
                return Settings.builtInKey(key).isEmpty()
                        ? getString(R.string.key_not_set)
                        : getString(R.string.key_built_in);
            });
        }
    }
}
