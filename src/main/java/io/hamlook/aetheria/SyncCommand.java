package io.hamlook.aetheria;

import io.hamlook.aetheria.command.SimpleCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.repo.CapeAPI;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.IChatComponent;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Base64;

@RegisterCommand
public class SyncCommand extends SimpleCommand {

    private static final String MOD_SECRET = "a7c0e73c-3b0b-4789-8c80-741dd09ba1bc";
    private static String SYNC_CODE = "";
    private static long lastUse = 0;

    @Override
    public String getName() {
        return "sync";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (!(sender instanceof EntityPlayer)) return;

        if (!SkyblockData.isOnSkyblock()) {
            ChatUtils.sendMessage("§cPlease Join SkyBlock in order to sync, this is to prove that you are not using the username of someone else.");
            return;
        }

        if(System.currentTimeMillis() - lastUse < 240000 && !SYNC_CODE.isEmpty()) {
            IChatComponent text = new ChatComponentText("§a[SkyAtlas] Your sync code is: §e§l" + SYNC_CODE);
            text.setChatStyle(new ChatStyle().setChatClickEvent(
                    new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, SYNC_CODE)
            ).setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new ChatComponentText("§aClick to show in chat"))));
            Minecraft.getMinecraft().addScheduledTask(() -> {
                        Minecraft.getMinecraft().thePlayer.addChatMessage(text);
                        ChatUtils.sendMessage(
                                "§r§aPlease paste this code in the §9#sync§a channel on Discord within 5 minutes!");
                    }
            );
            return;
        }

        String playerName = sender.getName();
        String syncCode = generateSyncCode();
        new Thread(() -> {
            try {
                URL url = new URL(CapeAPI.getAPIUrl("pending-sync"));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("x-playername", playerName);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("x-code", syncCode);
                conn.setRequestProperty("x-mod-secret", MOD_SECRET);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(new byte[0]);
                    os.flush();
                }

                int responseCode = conn.getResponseCode();

                if (responseCode >= 200 && responseCode < 300) {
                    IChatComponent text = new ChatComponentText("§a[SkyAtlas] Your sync code is: §e§l" + syncCode);
                    text.setChatStyle(new ChatStyle().setChatClickEvent(
                            new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, syncCode)
                    ).setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,new ChatComponentText("§aClick to show in chat"))));
                    Minecraft.getMinecraft().addScheduledTask(() -> {
                                Minecraft.getMinecraft().thePlayer.addChatMessage(text);
                                ChatUtils.sendMessage(
                                        "§r§aPlease paste this code in the §9#sync§a channel on Discord within 5 minutes!");
                            }
                        );
                    lastUse = System.currentTimeMillis();
                    SYNC_CODE = syncCode;
                } else {
                    Minecraft.getMinecraft().addScheduledTask(() -> ChatUtils.sendMessage("§c[SkyAtlas] Failed to generate sync code. API returned status " + responseCode));
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();

                Minecraft.getMinecraft().addScheduledTask(() -> ChatUtils.sendMessage("§c[SkyAtlas] An error occurred while contacting the sync server."));
            }
        }).start();
    }

    private String generateSyncCode() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[6];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}