package io.hamlook.aetheria.events;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.List;

public class DebugReportEvent extends Event {

    private final List<String> lines = new ArrayList<>();
    private final String search;
    private String currentTitle = "";
    private boolean anyFlagged = false;

    public DebugReportEvent(String search) {
        this.search = search == null ? "" : search;
    }

    public String getSearch() {
        return search;
    }

    public boolean isSearchAll() {
        return search.equalsIgnoreCase("all");
    }

    public boolean matchesSearch(String title) {
        if (search.isEmpty()) return false;
        if (isSearchAll()) return true;
        return title.toLowerCase().contains(search.toLowerCase());
    }

    public void title(String title) {
        this.currentTitle = title;
        lines.add("");
        lines.add("== " + title + " ==");
    }

    public void addFlagged(String... content) {
        anyFlagged = true;
        for (String line : content) lines.add(line);
    }

    public void addFlagged(List<String> content) {
        anyFlagged = true;
        lines.addAll(content);
    }

    public void addNormal(String... content) {
        if (isSearchAll() || matchesSearch(currentTitle)) {
            for (String line : content) lines.add(line);
        } else {
            lines.add(" (normal, hidden — use /asmdebug all or /asmdebug " + currentTitle.toLowerCase() + " to view)");
        }
    }

    public void addNormal(List<String> content) {
        if (isSearchAll() || matchesSearch(currentTitle)) {
            lines.addAll(content);
        } else {
            lines.add(" (normal, hidden — use /asmdebug all or /asmdebug " + currentTitle.toLowerCase() + " to view)");
        }
    }

    public List<String> getLines() {
        return lines;
    }

    public boolean isAnyFlagged() {
        return anyFlagged;
    }
}
