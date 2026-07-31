package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ChatLine {

    public ChatMessage message;

    public List<ChatLine> getLines(){
        return Collections.singletonList(this);
    }

}
