package com.cmacfarl.sensei.tutor;

import com.cmacfarl.sensei.text.RomajiDetector;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Walks a reply's JSON and collects every romaji leak, checking each field by its language. */
public final class RomajiCheck {

    private RomajiCheck() {}

    public static List<String> find(Mode mode, JsonObject reply) {
        Set<String> hits = new LinkedHashSet<>();
        walk(reply, "", Schemas.japaneseFields(mode), hits);
        return new ArrayList<>(hits);
    }

    private static void walk(JsonElement e, String path, List<String> japaneseFields, Set<String> hits) {
        if (e == null || e.isJsonNull()) return;
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : ((JsonObject) e).entrySet()) {
                String child = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                walk(entry.getValue(), child, japaneseFields, hits);
            }
        } else if (e.isJsonArray()) {
            for (JsonElement item : (JsonArray) e) walk(item, path, japaneseFields, hits);
        } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            if (path.equals("verdict")) return;
            String s = e.getAsString();
            hits.addAll(japaneseFields.contains(path)
                    ? RomajiDetector.findInJapanese(s)
                    : RomajiDetector.findInEnglish(s));
        }
    }
}
