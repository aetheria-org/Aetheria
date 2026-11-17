package io.hamlook.aetheria.features.dungeons;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatFilter;

import java.util.regex.Pattern;

@RegisterEvents
public class HideBlessing {

    public static final Pattern BLESSING = Pattern.compile("§r§6§lDUNGEON BUFF! .*?found a §r§dBlessing of \\w+.*?§r§f!§r");

    public HideBlessing() {
        ChatFilter.hide("dungeons.blessingMessages", msg -> ATHRConfig.feature != null && ATHRConfig.feature.dungeons.hideBlessingMessages && BLESSING.matcher(msg).find());
    }
}
