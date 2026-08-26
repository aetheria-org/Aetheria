package io.hamlook.aetheria.features.farming.visitors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VisitorOffer {

    public final Map<String, Integer> needs = new LinkedHashMap<>();
    public final Map<String, Integer> rewards = new LinkedHashMap<>();
    public final List<VisitorBonus> bonuses = new ArrayList<>();
    public final List<String> unresolvedNames = new ArrayList<>();

    public Integer copperAmount() {
        for (VisitorBonus bonus : bonuses) {
            if (bonus.type == VisitorBonus.Type.COPPER) return (int) bonus.amount;
        }
        return null;
    }
}
