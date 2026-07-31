package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class Sticker {

    public String id,url,name;
    public List<String> tags;

    public Sticker(String id,String name){
        this.id = id;
        this.name = name;
        this.url = "https://media.discordapp.net/stickers/" + id + ".png?size=240&quality=lossless&passthrough=true";
        this.tags = new ArrayList<>();
    }
}
