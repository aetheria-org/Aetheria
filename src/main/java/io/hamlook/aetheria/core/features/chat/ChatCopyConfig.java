package io.hamlook.aetheria.core.features.chat;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigEditorBoolean;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.ConfigOption;

public class ChatCopyConfig {

    @Expose
    @ConfigOption(name = "Chat Copy", desc = "Click on a chat line while chat is open to copy it. CTRL+Click copies the line, SHIFT+Click copies the full original message")
    @ConfigEditorBoolean
    public boolean chatCopyEnabled = false;

    @Expose
    @ConfigOption(name = "Copy with Formatting", desc = "Includes §ccolour codes when copying chat lines")
    @ConfigEditorBoolean
    public boolean chatCopyFormatted = false;
}
