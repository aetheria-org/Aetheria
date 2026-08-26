package io.hamlook.aetheria.features.farming.visitors;

import net.minecraft.item.ItemStack;

public final class VisitorLine {

    public enum Kind {TEXT, ITEM, SEPARATOR}

    public final Kind kind;
    public final String text;
    public final ItemStack icon;
    public final String itemId;
    public final int amount;

    private VisitorLine(Kind kind, String text, ItemStack icon, String itemId, int amount) {
        this.kind = kind;
        this.text = text;
        this.icon = icon;
        this.itemId = itemId;
        this.amount = amount;
    }

    public static VisitorLine text(String text) {
        return new VisitorLine(Kind.TEXT, text, null, null, 0);
    }

    public static VisitorLine item(String text, ItemStack icon, String itemId, int amount) {
        return new VisitorLine(Kind.ITEM, text, icon, itemId, amount);
    }

    public static VisitorLine separator() {
        return new VisitorLine(Kind.SEPARATOR, "", null, null, 0);
    }
}
