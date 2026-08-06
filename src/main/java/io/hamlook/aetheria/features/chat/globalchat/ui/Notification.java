package io.hamlook.aetheria.features.chat.globalchat.ui;

import io.hamlook.aetheria.utils.chat.ExpiringArrayList;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Notification implements ExpiringArrayList.Trackable {

    public Long startTime;
    public Long timeFrame;
    public String header;
    public String message;

    public static Notification createPunishmentNotif(String msg) {
        String header = "";
        String message = msg.toLowerCase();
        if(message.contains("muted")){
            if(message.contains("unmuted")){
                header = "Unmuted";
            }else {
                header = "Mute";
            }
        }else if(message.contains("banned")){
            if(message.contains("unbanned")){
                header = "Unbanned";
            }else {
                header = "Banned";
            }
        }
        return new Notification(System.currentTimeMillis(),5000L,header,msg);
    }


    @Override
    public boolean isExpired() {
        return (System.currentTimeMillis() - startTime) > timeFrame;
    }
}
