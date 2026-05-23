package io.hamlook.aetheria.utils.chat;

import io.hamlook.aetheria.init.RegisterEvents;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;


@RegisterEvents
public class ChatFilter {

    private static final ConcurrentHashMap<String, Predicate<String>> FILTERS = new ConcurrentHashMap<>();

    private ChatFilter() {
    }


    public static void hide(String key, Predicate<String> filter) {
        FILTERS.put(key, filter);
    }


    public static void hide(String key, Pattern pattern) {
        FILTERS.put(key, msg -> pattern.matcher(msg).find());
    }


    public static void unhide(String key) {
        FILTERS.remove(key);
    }


    public static boolean isHiding(String key) {
        return FILTERS.containsKey(key);
    }

    public static void clear() {
        FILTERS.clear();
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (FILTERS.isEmpty()) return;

        String raw = event.message.getFormattedText();
        for (Predicate<String> filter : FILTERS.values()) {
            if (filter.test(raw)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
