package com.cmacfarl.sensei.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.cmacfarl.sensei.R;
import com.cmacfarl.sensei.data.KnownKanji;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Edit the known-kanji set as plain text: paste, delete, or type kanji freely. */
public class KnownKanjiActivity extends AppCompatActivity {

    private KnownKanji known;
    private EditText editor;
    private TextView count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_known_kanji);
        known = KnownKanji.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        editor = findViewById(R.id.editor);
        count = findViewById(R.id.count);
        if (savedInstanceState == null) editor.setText(known.asString());
        updateCount();

        findViewById(R.id.save).setOnClickListener(v -> {
            known.replaceAll(editor.getText().toString());
            editor.setText(known.asString());
            updateCount();
            finish();
        });
        findViewById(R.id.reset).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.reset_confirm)
                .setPositiveButton(R.string.reset, (d, w) -> {
                    known.resetToStarterList();
                    editor.setText(known.asString());
                    updateCount();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());
    }

    private void updateCount() {
        count.setText(getResources().getQuantityString(R.plurals.known_count, known.count(), known.count()));
    }
}
