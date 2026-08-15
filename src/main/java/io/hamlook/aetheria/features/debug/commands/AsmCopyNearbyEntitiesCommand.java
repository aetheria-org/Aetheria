package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /asmcopynearbyentities [radius] — dumps every entity within range: class,
 * name, id, uuid, position, distance, and a rough type classification.
 * Default radius is 10 blocks.
 */
@RegisterCommand
public class AsmCopyNearbyEntitiesCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmcopynearbyentities";
    }

    @Override
    public String getUsage() {
        return "/asmcopynearbyentities [radius]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmcopyentities");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.thePlayer == null) {
            ChatUtils.sendMessage(EnumChatFormatting.RED + "Not in a world.");
            return;
        }

        double radius = 10;
        if (args.length == 1) {
            try {
                radius = Double.parseDouble(args[0]);
            } catch (NumberFormatException e) {
                ChatUtils.sendMessage(EnumChatFormatting.RED + "Radius must be a number.");
                return;
            }
        }

        List<Entity> loaded = new ArrayList<>(mc.theWorld.loadedEntityList);
        List<String> result = new ArrayList<>();
        int count = 0;

        for (Entity entity : loaded) {
            double distance = entity.getDistanceToEntity(mc.thePlayer);
            if (distance > radius) continue;
            if (entity == mc.thePlayer) continue;
            count++;

            result.add("entity: " + entity.getClass().getSimpleName());
            result.add("name: '" + nameOf(entity) + "'");
            result.add("displayName: '" + displayNameOf(entity) + "'");
            result.add("entityId: " + entity.getEntityId());
            result.add("uuid: " + entity.getUniqueID());
            result.add("type: " + classify(entity));
            result.add(String.format(Locale.ROOT, "position: %.2f, %.2f, %.2f (distance %.2f)", entity.posX, entity.posY, entity.posZ, distance));
            result.add(String.format(Locale.ROOT, "rotation: yaw=%.1f pitch=%.1f", entity.rotationYaw, entity.rotationPitch));
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                result.add("health: " + living.getHealth() + "/" + living.getMaxHealth());
            }
            result.add("---");
        }

        if (count == 0) {
            ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "No entities found within " + radius + " blocks.");
            return;
        }

        StringBuilder sb = new StringBuilder("=== NEARBY ENTITIES (" + count + " within " + radius + " blocks) ===\n");
        for (String line : result) sb.append(line).append("\n");

        GuiScreen.setClipboardString(sb.toString());
        ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Copied " + count + " nearby entities to clipboard.");
    }

    private static String classify(Entity entity) {
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            boolean isRealPlayer = player.getGameProfile() != null && player.getGameProfile().getId() != null;
            return isRealPlayer ? "player (real or NPC skin)" : "player-shaped entity";
        }
        if (entity instanceof EntityLivingBase) return "living entity / mob";
        return "misc entity";
    }

    private static String nameOf(Entity entity) {
        if (entity instanceof EntityLivingBase) return ((EntityLivingBase) entity).getName();
        return entity.toString();
    }

    private static String displayNameOf(Entity entity) {
        if (entity instanceof EntityLivingBase) return ((EntityLivingBase) entity).getDisplayName().getFormattedText();
        return entity.getClass().getSimpleName();
    }
}
