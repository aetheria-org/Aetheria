package io.hamlook.aetheria.features.farming.visitors;

public final class VisitorBonus {

    public enum Type {COPPER, BITS, FARMING_XP, GARDEN_XP}

    public final Type type;
    public final double amount;
    /** Original color-stripped lore line, for verbatim display. */
    public final String rawLine;

    public VisitorBonus(Type type, double amount, String rawLine) {
        this.type = type;
        this.amount = amount;
        this.rawLine = rawLine;
    }
}
