package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.command.brigadier.CommandCategory;
import io.hamlook.aetheria.command.brigadier.CommandRegistrationEvent;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.util.EnumChatFormatting;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * /asmfindnullconfig — walks the entire config object graph (ATHRConfig.feature)
 * and logs to console any object-typed field that is unexpectedly null. Useful
 * after config migrations/renames to catch a field that silently failed to load.
 */
@RegisterEvents
public class AsmFindNullConfigCommand {

    @HandleEvent
    public void onCommandRegistration(CommandRegistrationEvent event) {
        event.registerBrigadier("asmfindnullconfig", builder -> {
            builder.description = "Walks the config object graph and logs null fields";
            builder.setCategory(CommandCategory.DEVELOPER_DEBUG);

            builder.simpleCallback(() -> {
                if (ATHRConfig.feature == null) {
                    ChatUtils.sendMessage(EnumChatFormatting.RED + "ATHRConfig.feature itself is null!");
                    return;
                }

                Aetheria.logger.info("[ASM] start null finder");
                int[] nullCount = {0};
                findNull(ATHRConfig.feature, "config", new HashSet<>(), nullCount);
                Aetheria.logger.info("[ASM] stop null finder (" + nullCount[0] + " nulls found)");

                ChatUtils.sendMessage(nullCount[0] == 0
                    ? EnumChatFormatting.GREEN + "No null config fields found. See console for full log."
                    : EnumChatFormatting.YELLOW + "Found " + nullCount[0] + " null config field(s). See console/logs for details.");
            });
        });
    }

    private void findNull(Object obj, String path, Set<Object> seen, int[] nullCount) {
        if (obj == null || obj.getClass().isEnum()) return;
        if (!seen.add(obj)) return; // avoid infinite loops on cyclic refs

        Class<?> type = obj.getClass();
        // don't walk into JDK/library internals — just the mod's own config classes
        if (!type.getName().startsWith("io.hamlook.aetheria")) return;

        for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            if (field.getType().isPrimitive()) continue;

            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                String newPath = path + "." + field.getName();

                if (value == null) {
                    Aetheria.logger.warning("[ASM] config null at " + newPath);
                    nullCount[0]++;
                } else {
                    findNull(value, newPath, seen, nullCount);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
