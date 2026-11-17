package io.hamlook.aetheria.features.storage.utils;

public class StorageUtils {

    public static final int STORAGE_HEADER_SLOTS = 9;

    public static final int MAX_STORAGE_SLOT_COUNT = 45;

    public static final int DEFAULT_SLOT_COUNT = MAX_STORAGE_SLOT_COUNT;

    public static int getSlotCountFromRenderHeight(int renderH) {
        switch (renderH) {
            case 70:
                return 9;
            case 100:
                return 18;
            case 140:
                return 27;
            case 170:
                return 36;
            case 230:
                return 54;
            case 200:
            default:
                return 45;
        }
    }

    public static int calculateRenderHeight(int rows) {
        switch (rows) {
            case 1:
                return 70;
            case 2:
                return 100;
            case 3:
                return 140;
            case 4:
                return 170;
            case 6:
                return 230;
            case 5:
            default:
                return 200;
        }
    }

    public static int backpackRenderHeightOrZero(String sizeType) {
        if (sizeType == null) return 0;
        switch (sizeType.toLowerCase()) {
            case "small":
                return 70;
            case "medium":
                return 100;
            case "large":
                return 140;
            case "greater":
                return 170;
            case "jumbo":
                return 200;
            default:
                return 0;
        }
    }

    public static int contentSlots(int windowSize) {
        if (windowSize <= 0 || windowSize % 9 != 0) return 0;
        int slots = windowSize - STORAGE_HEADER_SLOTS;
        return slots > 0 && slots <= MAX_STORAGE_SLOT_COUNT ? slots : 0;
    }

    public static int renderHeightForSlotCount(int slotCount) {
        return calculateRenderHeight((int) Math.ceil(slotCount / 9.0));
    }

    public static int initialSlotCount(Type type, Iterable<SContainer> containers) {
        for (SContainer container : containers) {
            if (container.type == type && container.slotCount > 0) {
                return container.slotCount;
            }
        }
        return DEFAULT_SLOT_COUNT;
    }
}
