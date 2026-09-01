package io.hamlook.aetheria.features.qol.timers;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ActionBarUpdateEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.repo.TimerRepo;
import io.hamlook.aetheria.repo.data.TimerData;
import io.hamlook.aetheria.utils.item.ItemStackFinder;
import io.hamlook.aetheria.utils.time.TimerManager;
import net.minecraft.item.ItemStack;
import io.hamlook.aetheria.api.event.HandleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import io.hamlook.aetheria.events.ASMChatEvent;

@RegisterEvents
public class ItemCooldowns {

    private static final TimerManager timerManager = new TimerManager(TimerRepo.getCooldownDurations());
    private static List<ChatTrigger> chatTriggers = new ArrayList<>();
    private static List<ChatTrigger> actionBarTriggers = new ArrayList<>();

    static {
        reloadFromRepo();
        RepoHandler.addListener(ATHRRepo.KEY_TIMERS, ItemCooldowns::reloadFromRepo);
    }

    private static void reloadFromRepo() {
        timerManager.updateDurations(TimerRepo.getCooldownDurations());

        TimerData data = TimerRepo.getTimerData();

        List<ChatTrigger> newChat = new ArrayList<>();
        if (data.chatTriggers != null) {
            for (TimerData.TriggerEntry entry : data.chatTriggers) {
                if (entry.pattern != null && entry.itemIds != null && !entry.itemIds.isEmpty()) {
                    newChat.add(new ChatTrigger(Pattern.compile(entry.pattern), entry.itemIds.toArray(new String[0])));
                }
            }
        }
        chatTriggers = newChat;

        List<ChatTrigger> newActionBar = new ArrayList<>();
        if (data.actionBarTriggers != null) {
            for (TimerData.TriggerEntry entry : data.actionBarTriggers) {
                if (entry.pattern != null && entry.itemIds != null && !entry.itemIds.isEmpty()) {
                    newActionBar.add(new ChatTrigger(Pattern.compile(entry.pattern), entry.itemIds.toArray(new String[0])));
                }
            }
        }
        actionBarTriggers = newActionBar;
    }

    public static long getRemainingMs(String itemId) {
        return timerManager.getRemainingMs(itemId);
    }

    public static boolean isOnCooldown(String itemId) {
        return timerManager.isActive(itemId);
    }

    public static void markUsed(String itemId) {
        timerManager.markActive(itemId);
    }

    public static void markUsed(String itemId, long durationMs) {
        timerManager.markActive(itemId, durationMs);
    }

    public static List<String> getActiveCooldowns() {
        return timerManager.getActiveTimers();
    }

    public static ItemStack findItemStack(String itemId) {
        return ItemStackFinder.findItemStack(itemId);
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        if (event.type == 2) return;
        String msg = event.message.getFormattedText();
        for (ChatTrigger t : chatTriggers) {
            if (t.pattern.matcher(msg).find()) {
                for (String id : t.itemIds) {
                    markUsed(id);
                    ItemInvincibilityTimers.markActive(id);
                }
            }
        }
    }

    @HandleEvent
    public void onActionBar(ActionBarUpdateEvent event) {
        String msg = event.getText();
        for (ChatTrigger t : actionBarTriggers) {
            if (t.pattern.matcher(msg).find()) {
                for (String id : t.itemIds) {
                    markUsed(id);
                    ItemAbilityTimers.markActive(id);
                }
            }
        }
    }

    private static class ChatTrigger {
        final Pattern pattern;
        final String[] itemIds;

        ChatTrigger(Pattern pattern, String... itemIds) {
            this.pattern = pattern;
            this.itemIds = itemIds;
        }
    }
}