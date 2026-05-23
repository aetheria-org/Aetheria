package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatFilter;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.regex.Pattern;


@RegisterEvents
public class HideBlessing {

    private static final String KEY = "dungeons.blessingMessages";

    private static final Pattern BLESSING = Pattern.compile("§r§6§lDUNGEON BUFF! .*?found a §r§dBlessing of \\w+.*?§r§f!§r");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ClientChatReceivedEvent event) {
        if (ATHRConfig.feature == null) return;

        boolean enabled = ATHRConfig.feature.dungeons.hideBlessingMessages;

        if (enabled && !ChatFilter.isHiding(KEY)) {
            ChatFilter.hide(KEY, BLESSING);
        } else if (!enabled && ChatFilter.isHiding(KEY)) {
            ChatFilter.unhide(KEY);
        }
    }
}
