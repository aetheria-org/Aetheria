package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.TextCompat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * /asmtestmessage <text> [flags] — posts a fake chat message through the real
 * ClientChatReceivedEvent pipeline, so every @HandleEvent chat listener in
 * the mod fires exactly like it would for a real server message. Lets you test
 * chat-parsing features without needing the real trigger to happen in-game.
 *
 * Flags:
 *  -lines      split the message into multiple messages by newline (\n)
 *  -clipboard  read the message from your clipboard instead of the command args
 *  -s          don't print the "Testing message" confirmation line
 */
@RegisterCommand
public class AsmTestMessageCommand extends ASMCommand {

    @Override
    public String getName() {
        return "asmtestmessage";
    }

    @Override
    public String getUsage() {
        return "/asmtestmessage <text> [-lines] [-clipboard] [-s]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmtest");
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, net.minecraft.util.BlockPos pos) {
        return java.util.Arrays.asList("-lines", "-clipboard", "-s");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        if (args.length == 0) {
            ChatUtils.sendMultilineMessage(EnumChatFormatting.RED + "Specify a chat message to test!\n"
                + EnumChatFormatting.GRAY + "Syntax: /asmtestmessage <chat message> [flags]\n"
                + "  [-lines]: split the message into multiple by newlines\n"
                + "  [-clipboard]: read the message from the clipboard\n"
                + "  [-s]: hide the testing confirmation message");
            return;
        }

        List<String> mutArgs = new ArrayList<>(java.util.Arrays.asList(args));
        boolean multiLines = mutArgs.remove("-lines");
        boolean isClipboard = mutArgs.remove("-clipboard") || multiLines;
        boolean isSilent = mutArgs.remove("-s");

        String text;
        if (isClipboard) {
            String clip = GuiScreen.getClipboardString();
            if (clip == null || clip.isEmpty()) {
                ChatUtils.sendMessage(EnumChatFormatting.RED + "Clipboard does not contain a string!");
                return;
            }
            text = clip;
        } else {
            text = String.join(" ", mutArgs);
        }

        if (multiLines) {
            for (String line : text.split("\n")) {
                fire(line, isSilent);
            }
        } else {
            fire(text.replace("&", "\u00a7"), isSilent);
        }
    }

    private void fire(String text, boolean isSilent) {
        IChatComponent component = TextCompat.createText(text);

        if (!isSilent) {
            ChatUtils.sendMessage(EnumChatFormatting.GREEN + "Testing message: " + EnumChatFormatting.GRAY + text);
        }

        // type 0 = chat (matches a normal server chat message, not the action bar/system channel)
        MinecraftForge.EVENT_BUS.post(new ClientChatReceivedEvent((byte) 0, component));
    }
}
