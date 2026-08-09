package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.events.DebugReportEvent;
import io.hamlook.aetheria.init.RegisterCommand;
import io.hamlook.aetheria.repo.ATHRRepo;
import io.hamlook.aetheria.repo.RepoHandler;
import io.hamlook.aetheria.utils.chat.ChatUtils;
import io.hamlook.aetheria.utils.data.SkyblockData;
import io.hamlook.aetheria.utils.data.TablistParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.common.MinecraftForge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * /asmdebug [search] — copies a single debug report to the clipboard.
 * Mirrors SkyHanni's /shdebug: always shows anything flagged as unusual,
 * and shows everything else too when searched with "all" or a keyword.
 */
@RegisterCommand
public class AsmDebugCommand extends ASMCommand {

    private static final String PREFIX = EnumChatFormatting.GRAY + "[ASM Debug] " + EnumChatFormatting.RESET;

    @Override
    public String getName() {
        return "asmdebug";
    }

    @Override
    public String getUsage() {
        return "/asmdebug [search|all]";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("asmdbg");
    }

    @Override
    public void execute(ICommandSender sender, String[] args) throws CommandException {
        String search = args.length > 0 ? String.join(" ", Arrays.asList(args)) : "";

        DebugReportEvent event = new DebugReportEvent(search);

        List<String> out = new ArrayList<>();
        out.add("```");
        out.add("= Debug Report for Aetheria " + Aetheria.VERSION + " =");
        out.add(search.isEmpty()
                ? "no search specified, only showing flagged/unusual stuff:"
                : (event.isSearchAll() ? "search for everything:" : "search '" + search + "':"));

        player(event);
        location(event);
        scoreboardStatus(event);
        repoStatus(event);

        // let any other feature contribute (dungeons, mob detection, etc. can @SubscribeEvent this)
        MinecraftForge.EVENT_BUS.post(event);

        out.addAll(event.getLines());

        if (!event.isAnyFlagged() && search.isEmpty()) {
            out.add("");
            out.add("Nothing unusual to show right now!");
            out.add("Looking for something specific? /asmdebug <search>");
            out.add("Wanna see everything? /asmdebug all");
        }

        out.add("```");

        GuiScreen.setClipboardString(String.join("\n", out));
        ChatUtils.sendMessage(PREFIX + EnumChatFormatting.GREEN + "Copied debug report to the clipboard.");
    }

    private void player(DebugReportEvent event) {
        event.title("Player");
        Minecraft mc = Minecraft.getMinecraft();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "";
        event.addNormal("name: '" + name + "'");
    }

    private void location(DebugReportEvent event) {
        event.title("Location");
        if (mcWorldMissing()) {
            event.addFlagged("not in a world");
            return;
        }
        SkyblockData.Location loc = SkyblockData.getCurrentLocation();
        String serverPrefix = TablistParser.getServerPrefix();
        boolean onSkyblock = SkyblockData.isOnSkyblock();

        if (!onSkyblock) {
            event.addNormal("not detected as being on SkyBlock");
            return;
        }

        if (loc == SkyblockData.Location.NONE) {
            event.addFlagged(
                "on SkyBlock, but current Location is NONE (unknown area)",
                " server prefix: '" + serverPrefix + "'"
            );
            return;
        }

        String activeEvent = TablistParser.getActiveEvent();
        event.addNormal(
            "island: " + loc,
            " server prefix: '" + serverPrefix + "'",
            " environment: " + SkyblockData.getEnvironment() + (SkyblockData.getEnvironment().isTest() ? " (TEST ENVIRONMENT)" : ""),
            " active event: " + (activeEvent == null ? "none" : activeEvent + " (" + TablistParser.getActiveEventTimeLeft() + ")")
        );

        if (SkyblockData.getEnvironment().isTest()) {
            event.addFlagged("§eNote: you are currently on a sandbox/alpha/test server.");
        }
    }

    private void scoreboardStatus(DebugReportEvent event) {
        event.title("Scoreboard");
        if (mcWorldMissing()) {
            event.addFlagged("not in a world");
            return;
        }
        String title = SkyblockData.getScoreboardTitle();
        List<String> lines = SkyblockData.getCleanScoreboardLines();

        if (title == null) {
            event.addFlagged("no scoreboard objective in the sidebar slot!");
            return;
        }
        if (lines.isEmpty()) {
            event.addFlagged("scoreboard title present ('" + title + "') but no sidebar lines!");
            return;
        }

        event.addNormal(buildList(
            "title: '" + title + "'",
            "lines (" + lines.size() + "):"
        ));
        List<String> withLines = new ArrayList<>();
        for (String line : lines) withLines.add("  '" + line + "'");
        event.addNormal(withLines);
    }

    private void repoStatus(DebugReportEvent event) {
        event.title("Repo");
        String repoJson = RepoHandler.getJson(ATHRRepo.KEY_REPO);
        if (repoJson == null || repoJson.isEmpty()) {
            event.addFlagged("repo data for '" + ATHRRepo.KEY_REPO + "' is empty/missing! (network issue or repo down)");
        } else {
            event.addNormal("repo loaded fine (" + repoJson.length() + " chars)");
        }
    }

    private static List<String> buildList(String... s) {
        return Arrays.asList(s);
    }

    private boolean mcWorldMissing() {
        return Minecraft.getMinecraft().theWorld == null;
    }
}
