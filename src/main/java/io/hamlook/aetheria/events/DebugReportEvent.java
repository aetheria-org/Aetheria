package io.hamlook.aetheria.events;

import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DebugReportEvent extends Event {

    private final List<String> lines;
    private final String search;
    private String currentTitle = "";
    private boolean irrelevant = false;
    private boolean empty = true;

    public DebugReportEvent(List<String> lines, String search) {
        this.lines = lines;
        this.search = search == null ? "" : search;
    }

    public String getSearch() {
        return search;
    }

    public boolean isSearchAll() {
        return search.equalsIgnoreCase("all");
    }

    public void title(String title) {
        if (!currentTitle.isEmpty()) {
            System.err.println("[Aetheria] DebugReportEvent: duplicate title '" + title
                + "' with no data added after '" + currentTitle + "'");
        }
        this.currentTitle = title;
    }

    public void addIrrelevant(String... content) {
        addIrrelevant(Arrays.asList(content));
    }

    public void addIrrelevant(List<String> content) {
        irrelevant = true;
        addData(content);
    }

    public void addData(String... content) {
        addData(Arrays.asList(content));
    }

    public void addData(List<String> content) {
        if (currentTitle.isEmpty()) throw new IllegalStateException("Title not set");
        writeData(content);
        currentTitle = "";
        irrelevant = false;
    }

    private void writeData(List<String> text) {
        if (irrelevant && search.isEmpty()) return;
        if (!search.isEmpty() && !isSearchAll()) {
            if (!currentTitle.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) return;
        }

        empty = false;
        lines.add("");
        lines.add("== " + currentTitle + " ==");
        for (String line : text) lines.add(" " + line);
    }

    public boolean isEmpty() {
        return empty;
    }
}
