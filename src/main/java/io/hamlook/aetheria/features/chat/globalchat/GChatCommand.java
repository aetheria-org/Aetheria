package io.hamlook.aetheria.features.chat.globalchat;

import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.chat.globalchat.ui.ChatUI;
import io.hamlook.aetheria.features.chat.globalchat.vars.*;
import io.hamlook.aetheria.init.RegisterCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@RegisterCommand
public class GChatCommand extends ASMCommand {

    @Override
    public String getName() {
        return "globalchat";
    }

    @Override
    public String getUsage() {
        return "/" + getName();
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("gchat","g-chat");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        ATHRConfig.screenToOpen = new ChatUI();
    }

    private void sendTestMessageToAPI() {
        IEmoji emoji = GlobalChat.usableEmojis.get("shocked");
        ChatMessage message = new ChatMessage("test " + emoji.shortcode,"1531297414764433539",null);
        message.addAttachments(
                Collections.singletonList(
                        new Attachment(
                                "image",
                                ImageManager.images.get(GCImage.createGCImage("https://cdn.discordapp.com/attachments/1491451721606762616/1532146885073178644/image.png?ex=6a6c733b&is=6a6b21bb&hm=925ffaff0c49d1670bde3dd7b20ed9c81b85dbeb54481ca1474e56819188be36&"))
                        )
                )
        );
        HashMap<String, EmojiRef> emojiRefHashMap = new HashMap<>();
        EmojiRef ref = emoji.toEmoji();
        emojiRefHashMap.put(ref.name,ref);
        message.addEmojiRefs(emojiRefHashMap);
        GlobalChat.sendMessage(message);
        ChatMessage message1 = new ChatMessage("","1531297414764433539",null);
        HashMap<String, Sticker> stickers = new HashMap<>();
        Sticker sticker = new Sticker("859649755562508289","nono");
        stickers.put(sticker.id,sticker);
        message1.addStickers(stickers);
        GlobalChat.sendMessage(message1);
    }
}
