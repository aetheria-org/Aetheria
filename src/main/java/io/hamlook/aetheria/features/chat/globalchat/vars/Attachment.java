package io.hamlook.aetheria.features.chat.globalchat.vars;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Attachment {

    public String name,url,proxyURL,imageType;
    public Attachment(String name, GCImage image){
        this.name = name;
        this.url = image.url;
        this.proxyURL = image.url;
        String ext = image.url.substring(image.url.lastIndexOf('.') + 1);
        switch (ext) {
            case "webp": this.imageType =  "image/webp"; break;
            case "gif": this.imageType =  "image/gif"; break;
            default: this.imageType =  "image/png"; break;
        };
    }

}
