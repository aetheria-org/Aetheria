package io.hamlook.aetheria.features.chat.globalchat.ui.util;

import io.hamlook.aetheria.features.chat.globalchat.image.GCImage;

public class ChatObject {

    public enum ObjectType { TEXT, EMOJI, STICKER, ATTACHMENT, EMBED }

    public GCImage image;
    public String text;
    public ObjectType type;

    public String embedLabel;

    public ChatObject(GCImage image, String text){
        this(image, text, image == null ? ObjectType.TEXT : ObjectType.EMOJI);
    }

    public ChatObject(GCImage image, String text, ObjectType type){
        this.image = image;
        this.text = (image == null) ? text : null;
        this.type = type;
    }

    public ChatObject(GCImage image, String text, ObjectType type, String embedLabel){
        this(image, text, type);
        this.embedLabel = embedLabel;
    }

    public boolean isImage(){
        return image != null && text == null;
    }
}