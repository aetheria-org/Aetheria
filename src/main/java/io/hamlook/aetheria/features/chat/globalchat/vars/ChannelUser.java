package io.hamlook.aetheria.features.chat.globalchat.vars;

/**
 * A mentionable user of a Global Chat channel, as provided by the server:
 * MC usernames and Discord users (their username AND display name each appear
 * as a separate entry, both resolvable as @tokens). dcId is empty for MC-only
 * users who have no linked Discord account.
 */
public class ChannelUser {

    public String username;
    public String displayname;
    public String dcId;

    public ChannelUser() {
    }

    public ChannelUser(String username, String displayname, String dcId) {
        this.username = username;
        this.displayname = displayname;
        this.dcId = dcId;
    }

    /** The identity to insert after "@" — the username, since display names may contain characters the token regex rejects. */
    public String mentionToken() {
        return username != null && !username.isEmpty() ? username : displayname;
    }

    /** The label shown in the mention suggestions. */
    public String display() {
        return displayname != null && !displayname.isEmpty() ? displayname : username;
    }
}
