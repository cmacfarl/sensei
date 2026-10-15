package com.cmacfarl.sensei.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cmacfarl.sensei.R;
import com.cmacfarl.sensei.tutor.Responses;
import com.cmacfarl.sensei.tutor.Turn;
import com.cmacfarl.sensei.tutor.Tutor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Renders the thread. Tutor cards are built in code from the structured reply. */
final class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface Actions {
        void retry();

        void openSettings();

        void kanjiWordLongPressed(String word);
    }

    private final List<ChatItem> items = new ArrayList<>();
    private final Supplier<RubyRenderer.Options> options;
    private final Actions actions;

    ChatAdapter(Supplier<RubyRenderer.Options> options, Actions actions) {
        this.options = options;
        this.actions = actions;
    }

    @android.annotation.SuppressLint("NotifyDataSetChanged")
    void setItems(List<ChatItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        int layout;
        switch (viewType) {
            case ChatItem.STUDENT:
                layout = R.layout.item_student;
                break;
            case ChatItem.LOADING:
                layout = R.layout.item_loading;
                break;
            case ChatItem.ERROR:
                layout = R.layout.item_error;
                break;
            default:
                layout = R.layout.item_tutor;
        }
        return new RecyclerView.ViewHolder(inf.inflate(layout, parent, false)) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatItem item = items.get(position);
        View v = holder.itemView;
        switch (item.type) {
            case ChatItem.STUDENT:
                ((TextView) v.findViewById(R.id.text)).setText(item.text);
                break;
            case ChatItem.ERROR:
                ((TextView) v.findViewById(R.id.text)).setText(item.text);
                v.findViewById(R.id.retry).setOnClickListener(x -> actions.retry());
                v.findViewById(R.id.settings).setOnClickListener(x -> actions.openSettings());
                break;
            case ChatItem.TUTOR:
                bindTutor(v.findViewById(R.id.content), item.turn);
                break;
            default:
                break;
        }
    }

    // --- Tutor cards ---

    private void bindTutor(LinearLayout content, Turn turn) {
        content.removeAllViews();
        CardBuilder b = new CardBuilder(content, turn.unresolvedRomaji);
        if (!turn.unresolvedRomaji.isEmpty()) b.warning();

        Object parsed = turn.parsed != null ? turn.parsed : reparse(turn);
        if (parsed instanceof Responses.Translate) bindTranslate(b, (Responses.Translate) parsed);
        else if (parsed instanceof Responses.Check) bindCheck(b, (Responses.Check) parsed);
        else if (parsed instanceof Responses.FollowUp) bindFollowUp(b, (Responses.FollowUp) parsed);
    }

    private static Object reparse(Turn turn) {
        try {
            turn.parsed = Tutor.toResponse(turn.mode, Tutor.parseJsonForDisplay(turn.text));
        } catch (RuntimeException e) {
            turn.parsed = null;
        }
        return turn.parsed;
    }

    private void bindTranslate(CardBuilder b, Responses.Translate r) {
        b.label(R.string.label_polite);
        b.japanese(r.polite);
        b.label(R.string.label_casual);
        b.japanese(r.casual);
        b.body(r.casualNote);
        if (r.chunks != null && !r.chunks.isEmpty()) {
            b.heading(R.string.label_grammar);
            for (Responses.Chunk c : r.chunks) {
                b.chunk(c.japanese, c.role, c.explanation);
            }
        }
        if (notEmpty(r.notes)) {
            b.heading(R.string.label_notes);
            b.body(r.notes);
        }
    }

    private void bindCheck(CardBuilder b, Responses.Check r) {
        b.verdict(r.verdictValue());
        b.label(R.string.label_your_sentence);
        b.japanese(r.original);
        b.body(r.meaning);
        if (r.issues != null && !r.issues.isEmpty()) {
            b.heading(R.string.label_issues);
            for (Responses.Issue i : r.issues) b.issue(i.original, i.fix, i.explanation);
        }
        if (r.verdictValue() != Responses.Verdict.CORRECT) {
            b.label(R.string.label_corrected);
            b.japanese(r.corrected);
        }
        b.heading(R.string.label_natural);
        b.label(R.string.label_polite);
        b.japanese(r.naturalPolite);
        b.label(R.string.label_casual);
        b.japanese(r.naturalCasual);
        if (notEmpty(r.comment)) {
            b.divider();
            b.body(r.comment);
        }
    }

    private void bindFollowUp(CardBuilder b, Responses.FollowUp r) {
        b.body(r.answer);
        if (r.examples != null && !r.examples.isEmpty()) {
            b.heading(R.string.label_examples);
            for (Responses.Example e : r.examples) {
                b.japanese(e.japanese);
                b.secondary(e.english);
            }
        }
    }

    private static boolean notEmpty(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /** Appends styled rows to a card. */
    private final class CardBuilder {
        private final LinearLayout parent;
        private final Context ctx;
        private final RubyRenderer.Options opts;

        CardBuilder(LinearLayout parent, List<String> highlights) {
            this.parent = parent;
            this.ctx = parent.getContext();
            RubyRenderer.Options base = options.get();
            this.opts = new RubyRenderer.Options(base.showFurigana, base.known, highlights,
                    ContextCompat.getColor(ctx, R.color.romaji_highlight));
        }

        void label(int res) {
            TextView t = text(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium, 10, 2);
            t.setText(res);
            t.setTextColor(ContextCompat.getColor(ctx, R.color.label_text));
        }

        void heading(int res) {
            TextView t = text(com.google.android.material.R.style.TextAppearance_Material3_TitleSmall, 18, 4);
            t.setText(res);
        }

        void japanese(String markup) {
            if (!notEmpty(markup)) return;
            RubyTextView t = ruby(R.style.TextAppearance_Sensei_Japanese, 2, 4);
            t.setCopyOnTap(true);
            t.setMarkup(markup, true, opts);
        }

        void body(String markup) {
            if (!notEmpty(markup)) return;
            RubyTextView t = ruby(R.style.TextAppearance_Sensei_Note, 4, 4);
            t.setMarkup(markup, false, opts);
        }

        void secondary(String text) {
            if (!notEmpty(text)) return;
            RubyTextView t = ruby(com.google.android.material.R.style.TextAppearance_Material3_BodySmall, 0, 8);
            t.setTextColor(ContextCompat.getColor(ctx, R.color.label_text));
            t.setMarkup(text, false, opts);
        }

        void chunk(String japanese, String role, String explanation) {
            LinearLayout box = box(R.drawable.chunk_background);
            RubyTextView ja = ruby(box, R.style.TextAppearance_Sensei_JapaneseChunk, 0, 2);
            ja.setCopyOnTap(true);
            ja.setMarkup(japanese, true, opts);
            if (notEmpty(role)) {
                TextView r = text(box, com.google.android.material.R.style.TextAppearance_Material3_LabelMedium, 0, 2);
                r.setText(role);
                r.setTextColor(ContextCompat.getColor(ctx, R.color.accent_text));
            }
            RubyTextView ex = ruby(box, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, 0, 0);
            ex.setMarkup(explanation, false, opts);
        }

        void issue(String original, String fix, String explanation) {
            LinearLayout box = box(R.drawable.chunk_background);
            RubyTextView from = ruby(box, R.style.TextAppearance_Sensei_JapaneseChunk, 0, 0);
            from.setMarkup(original, true, opts);
            from.setPaintFlags(from.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            from.setTextColor(ContextCompat.getColor(ctx, R.color.verdict_incorrect));
            RubyTextView to = ruby(box, R.style.TextAppearance_Sensei_JapaneseChunk, 2, 4);
            to.setCopyOnTap(true);
            to.setMarkup("→ " + fix, true, opts);
            to.setTextColor(ContextCompat.getColor(ctx, R.color.verdict_correct));
            RubyTextView ex = ruby(box, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium, 0, 0);
            ex.setMarkup(explanation, false, opts);
        }

        void verdict(Responses.Verdict v) {
            TextView t = text(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium, 0, 8);
            int color;
            int label;
            switch (v) {
                case CORRECT:
                    color = R.color.verdict_correct;
                    label = R.string.verdict_correct;
                    break;
                case UNNATURAL:
                    color = R.color.verdict_unnatural;
                    label = R.string.verdict_unnatural;
                    break;
                default:
                    color = R.color.verdict_incorrect;
                    label = R.string.verdict_incorrect;
            }
            t.setText(label);
            t.setTextColor(ContextCompat.getColor(ctx, color));
        }

        void warning() {
            TextView t = text(com.google.android.material.R.style.TextAppearance_Material3_BodySmall, 0, 8);
            t.setText(ctx.getString(R.string.romaji_warning, Tutor.MAX_ROMAJI_RETRIES));
            t.setBackgroundColor(ContextCompat.getColor(ctx, R.color.romaji_highlight));
            int pad = dp(8);
            t.setPadding(pad, pad, pad, pad);
        }

        void divider() {
            View d = new View(ctx);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2));
            lp.topMargin = dp(12);
            lp.bottomMargin = dp(8);
            d.setLayoutParams(lp);
            d.setBackgroundColor(ContextCompat.getColor(ctx, R.color.divider));
            parent.addView(d);
        }

        private LinearLayout box(int background) {
            LinearLayout box = new LinearLayout(ctx);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setBackgroundResource(background);
            int pad = dp(10);
            box.setPadding(pad, pad, pad, pad);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(6);
            box.setLayoutParams(lp);
            parent.addView(box);
            return box;
        }

        private TextView text(int appearance, int topDp, int bottomDp) {
            return text(parent, appearance, topDp, bottomDp);
        }

        private TextView text(LinearLayout into, int appearance, int topDp, int bottomDp) {
            TextView t = new TextView(ctx);
            TextViewCompat.setTextAppearance(t, appearance);
            into.addView(t, params(topDp, bottomDp));
            return t;
        }

        private RubyTextView ruby(int appearance, int topDp, int bottomDp) {
            return ruby(parent, appearance, topDp, bottomDp);
        }

        private RubyTextView ruby(LinearLayout into, int appearance, int topDp, int bottomDp) {
            RubyTextView t = new RubyTextView(ctx);
            TextViewCompat.setTextAppearance(t, appearance);
            t.setOnKanjiWordLongPress(actions::kanjiWordLongPressed);
            into.addView(t, params(topDp, bottomDp));
            return t;
        }

        private LinearLayout.LayoutParams params(int topDp, int bottomDp) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(topDp);
            lp.bottomMargin = dp(bottomDp);
            return lp;
        }

        private int dp(int v) {
            return Math.round(v * ctx.getResources().getDisplayMetrics().density);
        }
    }
}
