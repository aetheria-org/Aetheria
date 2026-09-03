package io.hamlook.aetheria;

import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.GuiScreenElementWrapper;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigEditor;
import io.hamlook.aetheria.utils.compat.KeyboardCompat;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.IModGuiFactory;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Set;

public class GuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraft) {
    }

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return WrappedATHRConfig.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class WrappedATHRConfig extends GuiScreenElementWrapper {

        private final GuiScreen parent;

        public WrappedATHRConfig(GuiScreen parent) {
            super(new ConfigEditor(ATHRConfig.feature));
            this.parent = parent;
        }

        @Override
        public void handleKeyboardInput() throws IOException {
            if (KeyboardCompat.getEventKeyState() && KeyboardCompat.getEventKey() == Keyboard.KEY_ESCAPE) {
                MinecraftCompat.getMinecraft().displayGuiScreen(parent);
                return;
            }
            super.handleKeyboardInput();
        }
    }
}