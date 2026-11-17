package io.hamlook.aetheria.features.storage.data;

import com.google.gson.annotations.SerializedName;
import io.hamlook.aetheria.utils.compat.NbtCompat;
import io.hamlook.aetheria.utils.item.ItemUtils;
import net.minecraft.item.ItemStack;

public class ItemData {

    @SerializedName("d")
    public String data;

    @SerializedName("n")
    public String displayName;

    private transient ItemStack cachedStack;

    public ItemData() {
    }

    public static ItemData fromItemStack(ItemStack stack) {
        //#if MC < 1.21
        if (stack == null) return null;
        //#else
        //$$ if (stack == null || stack.isEmpty()) return null;
        //#endif

        ItemData item = new ItemData();

        String encoded = NbtCompat.encodeStack(stack);
        if (encoded == null) return null;
        item.data = encoded;

        item.displayName = ItemUtils.getDisplayName(stack);
        item.cachedStack = stack;

        return item;
    }

    public ItemStack toItemStack() {
        if (cachedStack != null) return cachedStack;
        if (data == null || data.isEmpty()) return null;

        try {
            cachedStack = NbtCompat.decodeStack(data);
            return cachedStack;
        } catch (Exception e) {
            return null;
        }
    }
}
