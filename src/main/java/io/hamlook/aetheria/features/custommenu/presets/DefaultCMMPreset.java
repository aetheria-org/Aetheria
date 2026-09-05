package io.hamlook.aetheria.features.custommenu.presets;

import io.hamlook.aetheria.Resources;
import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.CustomMMConfig;
import io.hamlook.aetheria.features.custommenu.Position;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.ActionButton;
import io.hamlook.aetheria.features.custommenu.ui.buttons.impl.GuiButton;
import io.hamlook.aetheria.features.custommenu.ui.sprites.Sprite;
import io.hamlook.aetheria.features.custommenu.ui.text.Text;

public class DefaultCMMPreset extends CustomMMConfig {

    public DefaultCMMPreset() {
        super("Default");
        addElement(new GuiButton(
                        new Position("CENTER", "CENTER", -100, 0),
                        200, 20, "Singleplayer","Singleplayer Menu"));

        addElement(new GuiButton(
                        new Position("CENTER", "CENTER", -100, -25),
                        200, 20, "Multiplayer","Multiplayer Menu"));

        addElement(new GuiButton(
                        new Position("CENTER", "CENTER", -100, -50),
                        98, 20, "Options","Options Menu"));

        addElement(new GuiButton(
                        new Position("CENTER", "CENTER", 2, -50),
                        98, 20, "Aetheria Mod","ASM Options Menu"));
        addElement(new GuiButton(
                new Position("CENTER","CENTER",-100,-75),
                200,20,"Change Menu Style","CMM Editor"
        ));

        addElement(new ActionButton(
                        new Position("RIGHT", "TOP", -18, -2),
                        16, 16, "✕", ActionButton.Action.EXIT));

        addElement(new Sprite(new Position("CENTER", "CENTER", -80, 140),
                160, 160, null, Resources.ASM_LOGO));

        addElement(new Text(new Position("CENTER","CENTER",0,12), true,"<gradient:#E0FF91>Minecraft</gradient:#7dd1f5> <gradient:#7dd1f5>- Aetheria's Skyblock Mod</gradient:E0FF91>",-1,1.2f));
        this.background = ImageManager.images.get(GCImage.createGCImageFromResource(Resources.CMM_DEFAULT_BG));

    }


}
