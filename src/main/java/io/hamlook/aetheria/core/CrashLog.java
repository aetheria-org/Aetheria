package io.hamlook.aetheria.core;

import io.hamlook.aetheria.Aetheria;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

/**
 * Writes a Minecraft-style crash report to config/Aetheria/logs/ whenever a
 * config or data file fails to load or save. One report is created per launch:
 * the first failure creates it, later failures append an "Affected file" block.
 * Old reports are pruned to the newest {@link #MAX_REPORTS}.
 */
public final class CrashLog {

    private static final int MAX_REPORTS = 10;
    private static final String REPORT_PREFIX = "storagecrash-";
    private static final String REPORT_SUFFIX = ".txt";

    private static File logDir;
    private static File currentReport;

    private CrashLog() {}

    /**
     * Records a failed load or save. Creates the launch report on the first
     * failure of a session and appends a per-file block for each later one.
     *
     * @param file   the file that failed
     * @param source whether the file failed while "loading" or "saving"
     * @param error  the error message from the reader or writer
     */
    public static synchronized void report(File file, String source, String error) {
        if (file == null) return;
        ensureInit();
        if (currentReport == null) {
            currentReport = new File(logDir, REPORT_PREFIX + timestamp() + REPORT_SUFFIX);
            write(buildHeader());
            prune();
        }
        write(buildBlock(file, source, error));
    }

    private static void ensureInit() {
        if (logDir != null) return;
        logDir = new File(ATHRConfig.configDirectory, "logs");
        try {
            Files.createDirectories(logDir.toPath());
        } catch (IOException e) {
            System.err.println("[ATHR] Failed to create crash log directory " + logDir + ": " + e.getMessage());
        }
    }

    private static String timestamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date());
    }

    private static String buildHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("---- Aetheria Storage Crash Report ----\n");
        sb.append("Written only when Aetheria fails to load or save a config or data file.\n");
        sb.append("Each \"Affected file\" block below describes one failed file.\n\n");
        sb.append("Field meanings:\n");
        sb.append("  Aetheria version     the mod version that was running\n");
        sb.append("  Minecraft version    the game version\n");
        sb.append("  Previous session clean\n");
        sb.append("                       true  = the last launch shut down normally\n");
        sb.append("                       false = the last launch did not shut down normally\n");
        sb.append("                               (crash, power loss, or forced kill)\n");
        sb.append("  Source               whether the file failed while loading or saving\n");
        sb.append("  Size                 the file size on disk; a full-size file filled with\n");
        sb.append("                       zero bytes means the write was interrupted\n");
        sb.append("  Last modified        when the file was last changed on disk\n");
        sb.append("  Diagnosis            the likely cause. \"interrupted write\" = damaged by a\n");
        sb.append("                       crash during a save; \"not crash-related\" = write bug\n");
        sb.append("                       or an external process\n");
        sb.append("  Error                the exact error message from the reader or writer\n\n");
        sb.append("Aetheria version: ").append(Aetheria.VERSION).append("\n");
        sb.append("Minecraft version: ").append(getMinecraftVersion()).append("\n");
        sb.append("Previous session clean: ").append(ATHRConfig.previousSessionClean).append("\n\n");
        sb.append("Corrupted files are backed up as <file>.<timestamp>.corrupted next to the\n");
        sb.append("original before defaults are used.\n\n");
        return sb.toString();
    }

    private static String buildBlock(File file, String source, String error) {
        StringBuilder sb = new StringBuilder();
        sb.append("Affected file: ").append(file.getName()).append("\n");
        sb.append("  Source: ").append(source).append("\n");
        sb.append("  Size: ").append(file.length()).append(" bytes\n");
        sb.append("  Last modified: ")
                .append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(file.lastModified())))
                .append("\n");
        sb.append("  Diagnosis: ").append(ATHRConfig.previousSessionClean
                ? "not crash-related (write bug or external process)"
                : "interrupted write (crash, power loss, or forced kill)")
                .append("\n");
        sb.append("  Error: ").append(error == null ? "unknown" : error).append("\n\n");
        return sb.toString();
    }

    private static String getMinecraftVersion() {
        try {
            return Loader.instance().getMCVersionString().replace("Minecraft ", "");
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static void prune() {
        File[] reports = logDir.listFiles((dir, name) ->
                name.startsWith(REPORT_PREFIX) && name.endsWith(REPORT_SUFFIX));
        if (reports == null || reports.length <= MAX_REPORTS) return;
        Arrays.sort(reports, Comparator.comparing(File::getName).reversed());
        for (int i = MAX_REPORTS; i < reports.length; i++) {
            reports[i].delete();
        }
    }

    private static void write(String content) {
        try {
            Files.write(currentReport.toPath(), content.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("[ATHR] Failed to write crash report " + currentReport.getName() + ": " + e.getMessage());
        }
    }
}