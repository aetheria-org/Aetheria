package io.hamlook.aetheria.features.profile;

import io.hamlook.aetheria.utils.ContainerUtils;
import lombok.NonNull;
import net.minecraft.client.gui.inventory.GuiContainer;


public abstract class ProfileModule {

    public String moduleName,moduleID;
    public String initialGUITitle;

    public ProfileModule(String moduleName, String moduleID,@NonNull String initialGUITitle) {
        this.moduleName = moduleName;
        this.moduleID = moduleID;
        this.initialGUITitle = initialGUITitle;
    }


    public abstract void parse(GuiContainer container) throws Exception;

    public boolean isInitialGUI(GuiContainer container){
        return initialGUITitle.equals(ContainerUtils.getContainerName(container));
    }
}
