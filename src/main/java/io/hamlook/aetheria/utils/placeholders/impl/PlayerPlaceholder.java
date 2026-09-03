package io.hamlook.aetheria.utils.placeholders.impl;

import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.placeholders.Placeholder;
import net.minecraft.client.Minecraft;

public class PlayerPlaceholder extends Placeholder {

    @Override
    public String replace(String initial) {
        Minecraft mc = MinecraftCompat.getMinecraft();
        String str = initial;
        str = str.replace("%player%",mc.getSession().getUsername());

        return str;
    }
}
