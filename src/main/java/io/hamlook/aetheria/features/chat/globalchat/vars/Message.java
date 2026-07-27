package io.hamlook.aetheria.features.chat.globalchat.vars;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Objects;

@AllArgsConstructor
@EqualsAndHashCode
public class Message {

    public static class Content {
        public String content;
        public HashMap<String,Sticker> stickers;
        public HashMap<String,GEmoji> emojiRefs;
        public HashMap<String,Attachment> attachments;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Content)) return false;
            Content other = (Content) o;
            return Objects.equals(content, other.content)
                    && Objects.equals(stickers, other.stickers)
                    && Objects.equals(emojiRefs, other.emojiRefs)
                    && Objects.equals(attachments, other.attachments);
        }

        @Override
        public int hashCode() {
            return Objects.hash(content, stickers, emojiRefs,attachments);
        }
    }
    public Content message;
    public String player;
    public String skin;
    public String id;
    public String messageID;
    public String replyTo;
    public String replyAuthor;
    public HashMap<String, Reaction> reactions;
}
