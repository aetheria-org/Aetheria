package io.hamlook.aetheria.features.custommenu;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CustomMMConfig {

    public String configName;
    protected GCImage background;
    public List<CMMElement> elements;


    public GCImage getBackground(){
        if(background == null){
            return ImageManager.images.get(GCImage.createGCImage("https://hypixel.net/attachments/2021-01-26_13-52-16-png.2297033/"));
        }
        return background;
    }
    public CustomMMConfig(String configName) {
        this.configName = configName;
        this.elements = new ArrayList<>();
    }

    public List<CMMButton> getButtons() {
        return elements.stream()
                .filter(e -> e instanceof CMMButton)
                .map(e -> (CMMButton) e)
                .collect(Collectors.toList());
    }

    protected void addElement(CMMElement element){
        elements.add(element);
    }
}
