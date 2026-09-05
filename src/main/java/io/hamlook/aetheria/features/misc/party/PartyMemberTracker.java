package io.hamlook.aetheria.features.misc.party;

import io.hamlook.aetheria.api.event.HandleEvent;
import io.hamlook.aetheria.events.ASMChatEvent;
import io.hamlook.aetheria.events.ASMWorldUnloadEvent;
import io.hamlook.aetheria.init.RegisterEvents;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks who is currently in the player's party by parsing Hypixel's party chat
 * messages (join/leave/kick/transfer/disband, and the "/p list" printout). This is
 * what {@link io.hamlook.aetheria.features.misc.party.PartyMemberOutline} checks
 * against to decide which nearby players to highlight.
 * <p>
 * The message formats mirrored here are Hypixel's own server output (the same
 * strings every SkyBlock mod has to match against), not another mod's code.
 */
@RegisterEvents
public class PartyMemberTracker {

    // Strips a leading rank tag like "[MVP+] " or "[VIP] " off a raw chat name.
    private static final Pattern RANK_PREFIX = Pattern.compile("^\\[[^]]*]\\s*");

    private static final Pattern YOU_JOINED = Pattern.compile("^You have joined (.+?)'s? party!$");
    private static final Pattern OTHER_JOINED = Pattern.compile("^(.+?) joined the party\\.$");
    private static final Pattern PARTYING_WITH = Pattern.compile("^You'll be partying with: (.+)$");
    private static final Pattern KUUDRA_FINDER_JOIN = Pattern.compile("^Party Finder > (.+?) joined the group! \\(.*Level \\d+\\)$");
    private static final Pattern DUNGEON_FINDER_JOIN = Pattern.compile("^Party Finder > (.+?) joined the dungeon group! \\(.*Level \\d+\\)$");

    private static final Pattern OTHER_LEFT = Pattern.compile("^(.+?) has left the party\\.$");
    private static final Pattern OTHER_KICKED = Pattern.compile("^(.+?) has been removed from the party\\.$");
    private static final Pattern OTHER_OFFLINE_KICKED = Pattern.compile("^Kicked (.+?) because they were offline\\.$");
    private static final Pattern OTHER_DISCONNECTED = Pattern.compile("^(.+?) was removed from your party because they disconnected\\.$");
    private static final Pattern TRANSFER_ON_LEAVE = Pattern.compile("^The party was transferred to (?:.+?) because (.+?) left$");
    private static final Pattern DISBANDED = Pattern.compile("^.* has disbanded the party!$");
    private static final Pattern YOU_KICKED = Pattern.compile("^You have been kicked from the party by .+$");

    private static final Pattern MEMBERS_HEADER = Pattern.compile("^Party Members \\(\\d+\\)$");
    private static final Pattern MEMBER_LIST_LINE = Pattern.compile("^Party (?:Leader|Moderators|Members): (.+)$");

    // Lowercase usernames of everyone currently tracked as being in the party (excluding yourself).
    private static final Set<String> partyMembers = new LinkedHashSet<>();

    public static boolean isInParty() {
        return !partyMembers.isEmpty();
    }

    public static boolean isPartyMember(String name) {
        return name != null && partyMembers.contains(name.toLowerCase());
    }

    @HandleEvent
    public void onChat(ASMChatEvent event) {
        String msg = ChatUtils.clean(event);
        Matcher m;

        if ((m = YOU_JOINED.matcher(msg)).matches()) {
            add(m.group(1));
        } else if ((m = OTHER_JOINED.matcher(msg)).matches()) {
            add(m.group(1));
        } else if ((m = PARTYING_WITH.matcher(msg)).matches()) {
            for (String name : m.group(1).split(", ")) add(name);
        } else if ((m = KUUDRA_FINDER_JOIN.matcher(msg)).matches()) {
            add(m.group(1));
        } else if ((m = DUNGEON_FINDER_JOIN.matcher(msg)).matches()) {
            add(m.group(1));
        } else if ((m = OTHER_LEFT.matcher(msg)).matches()) {
            remove(m.group(1));
        } else if ((m = OTHER_KICKED.matcher(msg)).matches()) {
            remove(m.group(1));
        } else if ((m = OTHER_OFFLINE_KICKED.matcher(msg)).matches()) {
            remove(m.group(1));
        } else if ((m = OTHER_DISCONNECTED.matcher(msg)).matches()) {
            remove(m.group(1));
        } else if ((m = TRANSFER_ON_LEAVE.matcher(msg)).matches()) {
            remove(m.group(1));
        } else if (DISBANDED.matcher(msg).matches() || YOU_KICKED.matcher(msg).matches()) {
            partyLeft();
        } else if (msg.equals("You left the party.")
                || msg.equals("The party was disbanded because all invites expired and the party was empty.")
                || msg.equals("You are not currently in a party.")
                || msg.equals("You are not in a party.")
                || msg.equals("The party was disbanded because the party leader disconnected.")) {
            partyLeft();
        } else if (MEMBERS_HEADER.matcher(msg).matches()) {
            partyMembers.clear();
        } else if ((m = MEMBER_LIST_LINE.matcher(msg)).matches()) {
            for (String raw : m.group(1).split(" ● ")) {
                add(raw.replace(" ●", ""));
            }
        }
    }

    @HandleEvent
    public void onWorldUnload(ASMWorldUnloadEvent event) {
        partyLeft();
    }

    private static void add(String rawName) {
        String name = clean(rawName);
        if (name.isEmpty() || name.equalsIgnoreCase(ownName())) return;
        partyMembers.add(name.toLowerCase());
    }

    private static void remove(String rawName) {
        partyMembers.remove(clean(rawName).toLowerCase());
    }

    private static String clean(String raw) {
        if (raw == null) return "";
        return RANK_PREFIX.matcher(raw.trim()).replaceAll("").trim();
    }

    private static String ownName() {
        Minecraft mc = MinecraftCompat.getMinecraft();
        return MinecraftCompat.getLocalPlayer() != null ? MinecraftCompat.getLocalPlayer().getName() : "";
    }

    private static void partyLeft() {
        partyMembers.clear();
    }
}
