package io.hamlook.aetheria.features.custommenu;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import io.hamlook.aetheria.features.chat.globalchat.image.ImageManager;
import io.hamlook.aetheria.features.custommenu.ui.CMMElement;
import io.hamlook.aetheria.features.custommenu.ui.buttons.CMMButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

public class CustomMMConfig {

    public String configName;
    public GCImage background;
    private transient String loadingBackgroundUrl;
    public List<CMMElement> elements;


    public GCImage getBackground(){
        if(background == null){
            background = ImageManager.images.get(GCImage.createGCImage("https://hypixel.net/attachments/2021-01-26_13-52-16-png.2297033/"));
        }
        if (!background.isLoaded && background.url != null && !background.url.isEmpty() && !background.url.equals(loadingBackgroundUrl)) {
            String url = background.url;
            loadingBackgroundUrl = url;
            GCImage loaded = new File(url).isFile()
                    ? ImageManager.images.get(GCImage.createGCImageFromFile(url))
                    : ImageManager.images.get(GCImage.createGCImage(url));
            background = loaded;
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

    public void addElement(CMMElement element){
        elements.add(element);
    }

    public void removeElement(CMMElement element) {
        elements.remove(element);
    }

    public void setBackground(GCImage background) {
        this.background = background;
    }
}
