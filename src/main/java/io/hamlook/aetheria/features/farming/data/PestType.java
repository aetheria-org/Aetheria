package io.hamlook.aetheria.features.farming.data;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public enum PestType {

    FLY("Fly", "Dung", "Pretty Fly Vinyl", 5, "WHEAT"),
    CRICKET("Cricket", "Honey Jar", "Cricket Choir Vinyl", -1, "CARROT_ITEM"),
    LOCUST("Locust", "Plant Matter", "Cicada Symphony Vinyl", -1, "POTATO_ITEM"),
    RAT("Rat", "Tasty Cheese", "Rodent Revolution Vinyl", -1, "PUMPKIN"),
    MOSQUITO("Mosquito", "Compost", "Buzzin' Beats Vinyl", -1, "SUGAR_CANE"),
    EARTHWORM("Earthworm", null, "Earthworm Ensemble Vinyl", 6, "MELON"),
    MITE("Mite", "Tasty Cheese", "DynaMITES Vinyl", 7, "CACTUS"),
    MOTH("Moth", "Honey Jar", "Wings of Harmony Vinyl", 8, "INK_SACK__3"),
    SLUG("Slug", "Plant Matter", "Slow and Groovy Vinyl", 9, "RED_MUSHROOM", "BROWN_MUSHROOM"),
    BEETLE("Beetle", "Dung", "Not Just a Pest Vinyl", 10, "NETHER_STALK"),
    FIREFLY("Firefly", "Jelly", "Firefly in the Hole Vinyl", 11, "MOONFLOWER"),
    DRAGONFLY("Dragonfly", null, "Imagine Dragonflies Vinyl", -1, "DOUBLE_PLANT"),
    PRAYING_MANTIS("Praying Mantis", null, "Pray For Me Vinyl", 12, "WILD_ROSE"),
    FIELD_MOUSE("Field Mouse", null, null, 5),
    LUNAR_MOTH("Lunar Moth", null, null, -1);

    private final String chatName;
    private final String sprayonator;
    private final String vinyl;
    private final int gardenLevel;
    private final List<Crop> crops;

    PestType(String chatName, String sprayonator, String vinyl, int gardenLevel, String... cropRawIds) {
        this.chatName = chatName;
        this.sprayonator = sprayonator;
        this.vinyl = vinyl;
        this.gardenLevel = gardenLevel;
        List<Crop> list = new ArrayList<>();
        for (String rawId : cropRawIds) {
            Crop crop = Crop.findByRawId(rawId);
            if (crop != null) list.add(crop);
        }
        this.crops = list;
    }

    public static PestType fromChatName(String chatName) {
        if (chatName == null) return null;
        for (PestType type : values()) {
            if (type.chatName.equalsIgnoreCase(chatName.trim())) return type;
        }
        return null;
    }

}