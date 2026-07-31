package io.hamlook.aetheria.features.chat.globalchat.vars;

import java.util.List;

/**
 * A link/message embed. Currently populated for auto-detected chat links
 * (image embeds + website OpenGraph embeds); a message payload may also carry
 * pre-built embeds (the "rich" type) which render the same way.
 */
public class Embed {

    /** "image", "website", "rich" or "failed" (no preview available). */
    public String type;
    /** The original link this embed belongs to. */
    public String url;
    /** Display name for image embeds (file name or "image"). */
    public String name;
    /** Site / provider name (og:site_name). */
    public String siteName;
    public String title;
    public String description;
    public String imageUrl;
    /** True while a website preview is still being fetched. */
    public transient boolean loading;
    /** Discord-style fields (used by "rich" embeds). */
    public List<EmbedField> fields;

    public Embed() {}

    public Embed(String type, String url) {
        this.type = type;
        this.url = url;
    }

    public static class EmbedField {
        public String name;
        public String value;
        public boolean inline;

        public EmbedField() {}
    }
}
