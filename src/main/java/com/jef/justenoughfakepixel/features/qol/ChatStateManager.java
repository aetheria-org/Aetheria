package com.jef.justenoughfakepixel.features.qol;

import lombok.Getter;

public class ChatStateManager {

    private static ChatStateManager INSTANCE;
    @Getter
    private String savedText = "";
    private boolean shouldRestore = false;

    private ChatStateManager() {
    }

    public static ChatStateManager getInstance() {
        if (INSTANCE == null) INSTANCE = new ChatStateManager();
        return INSTANCE;
    }

    public void updateState(String text) {
        if (text.isEmpty()) {
            shouldRestore = false;
        } else {
            shouldRestore = true;
            savedText = text;
        }
    }

    public void resetState() {
        savedText = "";
        shouldRestore = false;
    }

    public boolean shouldRestore() {
        return shouldRestore;
    }

}
