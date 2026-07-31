package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class IEmoji {

    public String id,identifier,shortcode,url;
    public boolean animated;


    public EmojiRef toEmoji(){
        return new EmojiRef(
                url,id,animated,shortcode.replace(":","")
        );
    }

}
