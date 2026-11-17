package io.hamlook.aetheria.core.features.dungeons;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class SecretsConfig {

    @Expose
    @ConfigOption(name = "Enable", desc = "Enable secret finder and rotation detection in dungeon rooms")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @Expose
    @Category(name = "Secret Waypoints", desc = "How uncollected secrets are drawn in the world")
    public DungeonSecretFinderConfig finder = new DungeonSecretFinderConfig();

    @Expose
    @Category(name = "Highlights", desc = "Solid highlights for levers and essence")
    public SecretHighlightsConfig highlights = new SecretHighlightsConfig();

    @Expose
    @Category(name = "Sounds", desc = "Sound played when a secret is collected")
    public SecretSoundsConfig sounds = new SecretSoundsConfig();

    public static SecretsConfig get() {
        if (ATHRConfig.feature == null) return null;
        return ATHRConfig.feature.dungeons.secrets;
    }
}
