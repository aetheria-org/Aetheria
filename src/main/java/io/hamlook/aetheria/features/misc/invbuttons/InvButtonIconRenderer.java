package io.hamlook.aetheria.features.misc.invbuttons;

import io.hamlook.aetheria.features.misc.itemList.ItemRegistry;
import io.hamlook.aetheria.features.misc.itemList.SkyblockItem;
import io.hamlook.aetheria.utils.Utils;
import io.hamlook.aetheria.utils.compat.GlStateManagerCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.render.ItemRenderUtils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;

import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

public class InvButtonIconRenderer {

    private static final HashMap<String, ItemStack> skullMap = new HashMap<>();

    private InvButtonIconRenderer() {
    }

    public static void renderIcon(String icon, int x, int y) {
        if (icon == null || icon.isEmpty()) return;

        if (icon.startsWith("extra:")) {
            String name = icon.substring("extra:".length());
            ResourceLocation loc = new ResourceLocation("aetheria", "invbuttons/extraicons/" + name + ".png");
            MinecraftCompat.getMinecraft().getTextureManager().bindTexture(loc);
            GlStateManagerCompat.color(1, 1, 1, 1);
            Utils.drawTexturedRect(x, y, 16, 16);
        } else {
            ItemStack stack = getStack(icon);
            if (stack == null) return;

            float scale = icon.startsWith("skull:") ? 1.2f : 1f;

            GlStateManagerCompat.pushMatrix();
            GlStateManagerCompat.translate(x + 8, y + 8, 0);
            GlStateManagerCompat.scale(scale, scale, 1);
            GlStateManagerCompat.translate(-8, -8, 0);
            drawItemStack(stack, 0, 0);
            GlStateManagerCompat.popMatrix();
        }
    }

    public static ItemStack getStack(String icon) {
        if (icon == null || icon.isEmpty()) return null;
        if (icon.startsWith("extra:")) return null;

        if (icon.startsWith("skull:")) {
            String link = icon.substring("skull:".length());
            if (skullMap.containsKey(link)) return skullMap.get(link);
            ItemStack stack = buildSkullStack(link);
            skullMap.put(link, stack);
            return stack;
        }

        SkyblockItem sbItem = ItemRegistry.getItem(icon);
        if (sbItem != null) {
            ItemStack stack = sbItem.getStack();
            if (stack != null) return stack;
        }

        return null;
    }

    private static ItemStack buildSkullStack(String hash) {
        ItemStack render = new ItemStack(Items.skull, 1, 3);
        NBTTagCompound nbt = new NBTTagCompound();
        NBTTagCompound owner = new NBTTagCompound();
        NBTTagCompound props = new NBTTagCompound();
        NBTTagList texs = new NBTTagList();
        NBTTagCompound tex0 = new NBTTagCompound();

        String uuid = UUID.nameUUIDFromBytes(hash.getBytes()).toString();
        owner.setString("Id", uuid);
        owner.setString("Name", uuid);

        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}";
        tex0.setString("Value", Base64.getEncoder().encodeToString(json.getBytes()));
        texs.appendTag(tex0);
        props.setTag("textures", texs);
        owner.setTag("Properties", props);
        nbt.setTag("SkullOwner", owner);
        render.setTagCompound(nbt);
        return render;
    }

    public static void drawItemStack(ItemStack stack, int x, int y) {
        ItemRenderUtils.drawItemStack(stack, x, y);
    }

}