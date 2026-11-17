package io.hamlook.aetheria.features.dungeons.rooms.secret;

import io.hamlook.aetheria.utils.compat.ItemCompatKt;
import io.hamlook.aetheria.utils.compat.SkullProfile;
import net.minecraft.item.ItemStack;

public final class EssenceSkull {

    private static final String PROFILE_ID = "3498edfb-f2ff-3774-aefe-fe8b0c84c128";

    private EssenceSkull() {
    }

    public static boolean matches(ItemStack stack) {
        return PROFILE_ID.equals(profileId(stack));
    }

    public static String profileId(ItemStack stack) {
        SkullProfile profile = ItemCompatKt.skullProfile(stack);
        return profile == null || profile.profileId.isEmpty() ? null : profile.profileId;
    }
}
