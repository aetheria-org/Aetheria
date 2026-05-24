package io.hamlook.aetheria.features.misc.pet;

public class CachedPet {

    public String baseName = "";
    public String formattedName = "";
    public String textureValue = "";
    public String rarityColor = "";
    public String skinTag = "";
    public int level = 0;

    public void sanitize() {
        if (baseName == null) baseName = "";
        if (formattedName == null) formattedName = "";
        if (textureValue == null) textureValue = "";
        if (rarityColor == null) rarityColor = "";
        if (skinTag == null) skinTag = "";
        formattedName = formattedName.replace("Â§", "§");
        rarityColor = rarityColor.replace("Â§", "§");
        skinTag = skinTag.replace("Â§", "§");
    }

    public void rebuildFormattedName() {
        StringBuilder sb = new StringBuilder();
        if (level > 0) sb.append("§7[Lvl ").append(level).append("] ");
        sb.append(rarityColor.isEmpty() ? "§f" : rarityColor);
        sb.append(baseName);
        if (!skinTag.isEmpty()) sb.append(" ").append(skinTag);
        formattedName = sb.toString();
    }
}
