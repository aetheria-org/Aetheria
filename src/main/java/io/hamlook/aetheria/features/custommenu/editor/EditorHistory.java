package io.hamlook.aetheria.features.custommenu.editor;

import com.google.gson.Gson;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Bounded command history for reversible preset edits. */
public final class EditorHistory {
    private final Gson gson;
    private final Deque<String> undo = new ArrayDeque<>();
    private final Deque<String> redo = new ArrayDeque<>();
    private final List<String> labels = new ArrayList<>();
    private final int limit;

    public EditorHistory(Gson gson, int limit) { this.gson = gson; this.limit = limit; }
    public void checkpoint(CustomMMConfig config, String label) {
        undo.push(gson.toJson(config)); redo.clear(); labels.add(0, label);
        while (undo.size() > limit) { undo.removeLast(); if (labels.size() > limit) labels.remove(labels.size() - 1); }
    }
    public CustomMMConfig undo(CustomMMConfig current) {
        if (undo.isEmpty()) return current;
        redo.push(gson.toJson(current)); if (!labels.isEmpty()) labels.remove(0);
        return gson.fromJson(undo.pop(), CustomMMConfig.class);
    }
    public CustomMMConfig redo(CustomMMConfig current) {
        if (redo.isEmpty()) return current;
        undo.push(gson.toJson(current)); labels.add(0, "Redo");
        return gson.fromJson(redo.pop(), CustomMMConfig.class);
    }
    public List<String> labels() { return labels; }
}
