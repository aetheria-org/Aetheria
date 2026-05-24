package io.hamlook.aetheria.features.misc.pet;

public final class CurrentPetApi {

    private CurrentPetApi() {
    }

    public static boolean hasPet() {
        return !key().isEmpty();
    }

    public static String getBaseName() {
        CachedPet p = cached();
        return p != null ? p.baseName : "";
    }

    public static int getLevel() {
        CachedPet p = cached();
        return p != null ? p.level : 0;
    }

    public static String getRarityColor() {
        CachedPet p = cached();
        return p != null ? p.rarityColor : "";
    }

    public static String getSkinTag() {
        CachedPet p = cached();
        return p != null ? p.skinTag : "";
    }

    public static String getTextureValue() {
        CachedPet p = cached();
        return p != null ? p.textureValue : "";
    }

    public static String getDisplayName() {
        CachedPet p = cached();
        return p != null ? p.formattedName : "";
    }

    public static boolean hasSkin() {
        return !getSkinTag().isEmpty();
    }

    public static String getColoredName() {
        CachedPet p = cached();
        if (p == null) return "";
        return (p.rarityColor.isEmpty() ? "§f" : p.rarityColor) + p.baseName;
    }

    public static boolean isPet(String baseName) {
        return getBaseName().equalsIgnoreCase(baseName);
    }

    public static boolean isRarity(String rarityColor) {
        return rarityColor.equals(getRarityColor());
    }

    private static String key() {
        return CurrentPetTracker.getInstance().getCurrentBaseName();
    }

    private static CachedPet cached() {
        String k = key();
        return k.isEmpty() ? null : PetCache.getInstance().get(k);
    }
}
