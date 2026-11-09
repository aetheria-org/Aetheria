package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class AlertsConfig {

    @Expose
    @Category(name = "Blood Room", desc = "Alert when the blood room is cleared")
    public Alert blood = Alert.of("§c§lBLOOD CLEARED!", "§b[ASM] §eBlood Room cleared!");

    @Expose
    @Category(name = "Room Cleared", desc = "Alert when a dungeon room is cleared")
    public Alert roomCleared = Alert.of("§eROOM CLEARED!", "§b[ASM] §eRoom cleared!");

    @Expose
    @Category(name = "Secrets Cleared", desc = "Alert when all secrets in a room are found")
    public Alert secretsCleared = Alert.of("§aSECRETS CLEARED!", "§b[ASM] §eAll secrets found!");

    public static class Alert {

        @Expose
        @ConfigOption(name = "Enable", desc = "Enable this alert")
        @ConfigEditorBoolean
        public boolean enabled = true;

        @Expose
        @ConfigOption(name = "Alert Text", desc = "On-screen alert text. Leave empty to hide the banner.")
        @ConfigEditorText
        public String alertText = "";

        @Expose
        @ConfigOption(name = "Chat Text", desc = "Chat message sent on clear. Leave empty for no message.")
        @ConfigEditorText
        public String chatText = "";

        @Expose
        @ConfigOption(name = "Alert Sound", desc = "Which sound the alert plays")
        @ConfigEditorDropdown(values = {"Pling", "Dragon Roar", "Cat Meow", "Orb", "Level Up"})
        public int alertSound = 0;

        private static final String[] SOUND_IDS = {
                "note.pling", "mob.enderdragon.growl", "mob.cat.meow", "random.orb", "random.levelup"
        };

        public static Alert of(String alertText, String chatText) {
            Alert alert = new Alert();
            alert.alertText = alertText;
            alert.chatText = chatText;
            return alert;
        }

        public boolean hasAlertText() {
            return alertText != null && !alertText.trim().isEmpty();
        }

        public boolean hasChatText() {
            return chatText != null && !chatText.trim().isEmpty();
        }

        public String soundId() {
            return SOUND_IDS[Math.max(0, Math.min(SOUND_IDS.length - 1, alertSound))];
        }
    }
}
