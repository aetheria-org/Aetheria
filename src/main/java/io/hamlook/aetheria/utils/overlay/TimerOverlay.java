package io.hamlook.aetheria.utils.overlay;

import io.hamlook.aetheria.utils.time.TimeFormatter;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TimerOverlay extends Overlay {

    public TimerOverlay() {
        super(90, 14);
    }

    protected abstract String getHeaderText();

    protected abstract List<String> getActiveTimers();

    protected abstract ItemStack findItemStack(String id);

    protected abstract long getRemainingMs(String id);

    protected abstract boolean shouldShowWhenEmpty();

    protected abstract String getPreviewItemName();

    @Override
    public List<String> getLines(boolean preview) {
        List<String> lines = new ArrayList<>();
        clearLineIcons();

        if (preview) {
            lines.add(getHeaderText());
            lines.add(getPreviewItemName() + " §f30.0s");
            return lines;
        }

        List<String> active = getActiveTimers();
        if (active.isEmpty() && !shouldShowWhenEmpty()) {
            return Collections.emptyList();
        }

        lines.add(getHeaderText());

        for (String id : active) {
            ItemStack stack = findItemStack(id);
            if (stack == null) continue;
            String line = stack.getDisplayName() + " §f" + TimeFormatter.formatTime(getRemainingMs(id));
            lines.add(line);
            putLineIcon(line, stack);
        }

        return lines;
    }

    @Override
    protected int getIconSize() {
        return LINE_HEIGHT;
    }
}
