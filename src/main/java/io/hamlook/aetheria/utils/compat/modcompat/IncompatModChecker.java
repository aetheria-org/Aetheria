package io.hamlook.aetheria.utils.compat.modcompat;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.ASMServerJoinEvent;
import io.hamlook.aetheria.events.ASMTickEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RegisterEvents
public class IncompatModChecker {

    private static final List<IncompatMod> REGISTRY = new ArrayList<>();
    private static boolean hasWarnedThisLaunch = false;
    private static List<IncompatMod> pending = null;

    static {
        REGISTRY.add(new IncompatMod(new String[]{"skyblockaddons"}, "SkyblockAddons", "breaks the enchant parser and enchant chroma", "This mod breaks the enchant parser and enchant chroma.\n" + "A mod for Hypixel, not Fakepixel.\n" + "Most features do not work."));
        REGISTRY.add(new IncompatMod(new String[]{"patcher", "polypatcher"}, "Patcher", "may break enchant chroma", "This mod may break enchant chroma.\n" + "It breaks only when the optimized font renderer is on.\n" + "Turn that setting off to keep chroma."));
    }

    private static List<IncompatMod> findIncompatible() {
        List<IncompatMod> found = new ArrayList<>();
        Set<String> dismissed = ATHRConfig.feature.misc.dismissedIncompatMods;
        for (IncompatMod mod : REGISTRY) {
            if (!mod.isLoaded()) continue;
            if (dismissed.contains(mod.key())) continue;
            found.add(mod);
        }
        return found;
    }

    private static void sendWarning(List<IncompatMod> found) {
        IChatComponent root = TextCompat.createText("");
        TextCompat.appendSibling(root, TextCompat.createText("§c[ATHR] §fDetected mods that may break Aetheria features:"));
        TextCompat.appendSibling(root, TextCompat.createText("\n§7Hover a mod name to see what it breaks. Click §e[Hide]§7 to stop that warning."));
        for (IncompatMod mod : found) {
            TextCompat.appendSibling(root, TextCompat.createText("\n§c - "));
            IChatComponent name = TextCompat.createText("§e" + mod.displayName);
            TextCompat.setHoverShowText(TextCompat.getChatStyle(name), "§e" + mod.displayName + "\n§7" + mod.hoverDetails);
            TextCompat.appendSibling(root, name);
            TextCompat.appendSibling(root, TextCompat.createText(" §7(" + mod.impact + ") "));
            IChatComponent hide = TextCompat.createText("§8[§cHide§8]");
            TextCompat.setClickRunCommand(TextCompat.getChatStyle(hide), "/athrignoreincompat ignore " + mod.key());
            TextCompat.setHoverShowText(TextCompat.getChatStyle(hide), "§cHide this warning for " + mod.displayName);
            TextCompat.appendSibling(root, hide);
        }
        Minecraft.getMinecraft().thePlayer.addChatMessage(root);
    }

    public static boolean dismiss(String key) {
        if (ATHRConfig.feature == null) return false;
        if (ATHRConfig.feature.misc.dismissedIncompatMods.add(key)) {
            ATHRConfig.saveConfig();
            return true;
        }
        return false;
    }

    public static boolean reset(String key) {
        if (ATHRConfig.feature == null) return false;
        if (ATHRConfig.feature.misc.dismissedIncompatMods.remove(key)) {
            ATHRConfig.saveConfig();
            return true;
        }
        return false;
    }

    public static Set<String> dismissed() {
        return ATHRConfig.feature != null ? ATHRConfig.feature.misc.dismissedIncompatMods : java.util.Collections.emptySet();
    }

    @HandleEvent
    public void onServerJoin(ASMServerJoinEvent event) {
        if (hasWarnedThisLaunch) return;
        if (ATHRConfig.feature == null) return;
        List<IncompatMod> found = findIncompatible();
        if (found.isEmpty()) return;
        hasWarnedThisLaunch = true;
        pending = found;
    }

    @HandleEvent
    public void onClientTick(ASMTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (pending == null) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;
        sendWarning(pending);
        pending = null;
    }

    private static class IncompatMod {
        private final String[] modIds;
        private final String displayName;
        private final String impact;
        private final String hoverDetails;

        private IncompatMod(String[] modIds, String displayName, String impact, String hoverDetails) {
            this.modIds = modIds;
            this.displayName = displayName;
            this.impact = impact;
            this.hoverDetails = hoverDetails;
        }

        private String key() {
            return modIds[0];
        }

        private boolean isLoaded() {
            for (String id : modIds) {
                if (Loader.isModLoaded(id)) return true;
            }
            return false;
        }
    }
}
