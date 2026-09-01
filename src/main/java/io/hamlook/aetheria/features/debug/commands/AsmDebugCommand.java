package io.hamlook.aetheria.features.debug.commands;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.command.ASMCommand;
import io.hamlook.aetheria.events.DebugReportEvent;
import io.hamlook.aetheria.features.misc.PerformanceHUD;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * /asmdebug [search] — copies a single debug report to the clipboard.
 * Mirrors SkyHanni's /shdebug: always shows anything flagged as unusual,
 * and shows everything else too when searched with "all" or a keyword.
 */
@RegisterCommand
public class AsmDebugCommand extends ASMCommand {

    private static final double TPS_LIMIT = 15.0;
    private static final double PING_LIMIT_MS = 1500.0;

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
        String search = args.length > 0 ? String.join(" ", java.util.Arrays.asList(args)) : "";

        List<String> out = new ArrayList<>();
        out.add("```");
        out.add("= Debug Report for Aetheria " + Aetheria.VERSION + " =");
        out.add("");
        out.add(!search.isEmpty()
            ? (search.equalsIgnoreCase("all") ? "search for everything:" : "search '" + search + "':")
            : "no search specified, only showing interesting stuff:");

        DebugReportEvent event = new DebugReportEvent(out, search);

        player(event);
        repoData(event);
        skyblockStatus(event);
        networkInfo(event);

        event.post();

        if (event.isEmpty()) {
            out.add("");
            out.add("Nothing interesting to show right now!");
            out.add("Looking for something specific? /asmdebug <search>");
            out.add("Wanna see everything? /asmdebug all");
        }

        out.add("```");

        GuiScreen.setClipboardString(String.join("\n", out));
        ChatUtils.sendMessage(EnumChatFormatting.YELLOW + "Copied Aetheria debug data to the clipboard.");
    }

    private void player(DebugReportEvent event) {
        event.title("Player");
        Minecraft mc = Minecraft.getMinecraft();
        String name = mc.getSession() != null ? mc.getSession().getUsername() : "";
        String uuid = mc.thePlayer != null ? String.valueOf(mc.thePlayer.getUniqueID()) : "";
        event.addIrrelevant(
            "name: '" + name + "'",
            "uuid: '" + uuid + "'"
        );
    }

    private void repoData(DebugReportEvent event) {
        event.title("Repo Information");
        String repoJson = RepoHandler.getJson(ATHRRepo.KEY_REPO);
        if (repoJson == null || repoJson.isEmpty()) {
            event.addData("repo data for '" + ATHRRepo.KEY_REPO + "' is empty/missing! (network issue or repo down)");
        } else {
            event.addIrrelevant("repo loaded fine (" + repoJson.length() + " chars)");
        }
    }

    private void skyblockStatus(DebugReportEvent event) {
        event.title("SkyBlock Status");
        if (mcWorldMissing()) {
            event.addData("not in a world");
            return;
        }
        if (!SkyblockData.isOnSkyblock()) {
            event.addIrrelevant("not detected as being on SkyBlock");
            return;
        }

        SkyblockData.Location loc = SkyblockData.getCurrentLocation();
        String serverPrefix = TablistParser.getServerPrefix();

        if (loc == SkyblockData.Location.NONE) {
            event.addData(
                "on SkyBlock, but current Location is NONE (unknown area)",
                " server prefix: '" + serverPrefix + "'"
            );
            return;
        }

        String activeEvent = TablistParser.getActiveEvent();
        List<String> lines = new ArrayList<>();
        lines.add("island: " + loc);
        lines.add(" server prefix: '" + serverPrefix + "'");
        lines.add(" environment: " + SkyblockData.getEnvironment() + (SkyblockData.getEnvironment().isTest() ? " (TEST ENVIRONMENT)" : ""));
        lines.add(" active event: " + (activeEvent == null ? "none" : activeEvent + " (" + TablistParser.getActiveEventTimeLeft() + ")"));

        if (SkyblockData.getEnvironment().isTest()) {
            lines.add("§eNote: you are currently on a sandbox/alpha/test server.");
            event.addData(lines);
        } else {
            event.addIrrelevant(lines);
        }
    }

    private void networkInfo(DebugReportEvent event) {
        event.title("Network Information");
        float tps = PerformanceHUD.getCurrentTps();
        double ping = PerformanceHUD.getPingMs();

        List<String> lines = new ArrayList<>();
        lines.add("tps: " + String.format(Locale.ROOT, "%.1f", tps));
        lines.add("ping: " + (ping < 0 ? "..." : String.format(Locale.ROOT, "%.0fms", ping)));

        if (tps < TPS_LIMIT || ping > PING_LIMIT_MS) {
            event.addData(lines);
        } else {
            event.addIrrelevant(lines);
        }
    }

    private boolean mcWorldMissing() {
        return Minecraft.getMinecraft().theWorld == null;
    }
}
