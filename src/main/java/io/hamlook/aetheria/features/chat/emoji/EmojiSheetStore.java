package io.hamlook.aetheria.features.chat.emoji;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.StorageManager;
import io.hamlook.aetheria.network.NetworkGuard;
import io.hamlook.aetheria.utils.HttpClient;
import io.hamlook.aetheria.utils.ThreadUtils;
import io.hamlook.aetheria.utils.compat.MinecraftCompat;
import io.hamlook.aetheria.utils.compat.TextureCompat;
import io.hamlook.aetheria.utils.image.PngInfo;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class EmojiSheetStore {

    private static final long RETRY_DELAY_MS = 30_000L;

    private static final Set<String> RESIDENT = ConcurrentHashMap.newKeySet();
    private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<String> REPAIRED = ConcurrentHashMap.newKeySet();
    private static final Set<String> WANTED = ConcurrentHashMap.newKeySet();
    private static final Set<String> IN_FLIGHT = ConcurrentHashMap.newKeySet();
    private static final Map<String, Integer> WIDTHS = new ConcurrentHashMap<>();
    private static final Map<String, Long> FAILED_AT = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> GENERATION = new ConcurrentHashMap<>();
    private static final List<Consumer<Boolean>> QUEUED = Collections.synchronizedList(new ArrayList<>());
    private static final Queue<Runnable> DEFERRED = new ConcurrentLinkedQueue<>();

    private static final AtomicBoolean DOWNLOADING = new AtomicBoolean();
    private static final AtomicInteger REQUIRED = new AtomicInteger();

    private static volatile int dataColumns;
    private static volatile String drawn;

    private EmojiSheetStore() {
    }

    static String theme() {
        return drawn;
    }

    static int width(String sheet) {
        if (sheet == null) return 0;
        Integer recorded = WIDTHS.get(sheet);
        if (recorded != null && recorded > 0) return recorded;
        if (EmojiLinks.CUSTOM_SHEET.equals(sheet) || dataColumns <= 0) return 0;
        return dataColumns * EmojiLinks.SHEET_CELL;
    }

    static String configuredTheme() {
        int index = 0;
        if (ATHRConfig.feature != null) {
            index = ATHRConfig.feature.chat.emojiConfig.emojiTheme;
        }
        int last = EmojiLinks.DEFAULT_SHEETS.length - 1;
        return EmojiLinks.DEFAULT_SHEETS[Math.max(0, Math.min(last, index))];
    }

    static boolean isResident(String sheet) {
        return sheet != null && RESIDENT.contains(sheet);
    }

    private static List<String> missingSheets() {
        List<String> missing = new ArrayList<>();
        for (String sheet : EmojiLinks.ALL_SHEETS) {
            File file = EmojiLinks.getSpriteFile(sheet);
            if (!file.isFile() || file.length() == 0) missing.add(sheet);
        }
        return missing;
    }

    static void downloadAll(Consumer<Boolean> onDone) {
        request(Arrays.asList(EmojiLinks.ALL_SHEETS), onDone);
    }

    static void downloadMissing(Consumer<Boolean> onDone) {
        List<String> missing = missingSheets();
        if (!missing.isEmpty()) request(missing, onDone);
    }

    static void repair(String sheet) {
        if (sheet == null || !REPAIRED.add(sheet)) return;
        request(Collections.singletonList(sheet), null);
    }

    private static void request(Collection<String> sheets, Consumer<Boolean> onDone) {
        if (!NetworkGuard.githubAllowed()) return;
        if (onDone != null) QUEUED.add(onDone);
        for (String sheet : sheets) {
            if (!IN_FLIGHT.contains(sheet)) WANTED.add(sheet);
        }
        startBatch();
    }

    private static void startBatch() {
        if (!NetworkGuard.githubAllowed()) return;
        if (!DOWNLOADING.compareAndSet(false, true)) return;
        Set<String> targets = new LinkedHashSet<>();
        for (String sheet : EmojiLinks.ALL_SHEETS) {
            if (WANTED.remove(sheet)) targets.add(sheet);
        }
        if (targets.isEmpty()) {
            DOWNLOADING.set(false);
            return;
        }
        AtomicInteger left = new AtomicInteger(targets.size());
        AtomicBoolean customOk = new AtomicBoolean(false);
        for (String sheet : targets) {
            IN_FLIGHT.add(sheet);
            ThreadUtils.run("EmojiSheet-Download-" + sheet, () -> {
                try {
                    if (download(sheet) && EmojiLinks.CUSTOM_SHEET.equals(sheet)) customOk.set(true);
                } finally {
                    IN_FLIGHT.remove(sheet);
                    if (left.decrementAndGet() == 0) batchFinished(customOk.get());
                }
            });
        }
    }

    private static void batchFinished(boolean customOk) {
        DOWNLOADING.set(false);
        List<Consumer<Boolean>> callbacks;
        synchronized (QUEUED) {
            callbacks = new ArrayList<>(QUEUED);
            QUEUED.clear();
        }
        for (Consumer<Boolean> callback : callbacks) {
            try {
                callback.accept(customOk);
            } catch (RuntimeException e) {
                Aetheria.logger.warning("[EMOJI] Download callback failed - " + e.getMessage());
            }
        }
        loadResident();
        if (!WANTED.isEmpty()) {
            startBatch();
            return;
        }
        reportGrid();
        reportCustom();
    }

    private static void reportGrid() {
        if (dataColumns <= 0) return;
        if (gridMatches(dataColumns * EmojiLinks.SHEET_CELL)) return;
        Aetheria.logger.warning("[EMOJI] Sheets still do not match the emoji data - sprites stay wrong until a usable sheet arrives");
    }

    private static void reportCustom() {
        int required = REQUIRED.get();
        if (required <= 0) return;
        if (width(EmojiLinks.CUSTOM_SHEET) >= required) return;
        Aetheria.logger.warning("[EMOJI] Custom sheet is missing or smaller than its data needs (" + required + "px)");
    }

    private static boolean download(String sheet) {
        File target = EmojiLinks.getSpriteFile(sheet);
        try {
            BufferedImage image = HttpClient.fetchImage(EmojiLinks.getSpriteURL(EmojiLinks.sheetToURL(sheet)));
            if (image.getWidth() < EmojiLinks.SHEET_RESOLUTION || image.getHeight() < EmojiLinks.SHEET_RESOLUTION) {
                return false;
            }
            if (!StorageManager.saveAtomicImage(target, image)) {
                Aetheria.logger.warning("[EMOJI] Failed to save " + sheet + " to " + target.getPath());
                return false;
            }
            WIDTHS.put(sheet, image.getWidth());
            generation(sheet).incrementAndGet();
            RESIDENT.remove(sheet);
            FAILED_AT.remove(sheet);
            Aetheria.logger.info("[EMOJI] Downloaded " + sheet + " (" + image.getWidth() + "px)");
            return true;
        } catch (Exception e) {
            Aetheria.logger.warning("[EMOJI] Failed to download " + sheet + " - " + e.getMessage());
            return false;
        }
    }

    static void recordWidths() {
        for (String sheet : EmojiLinks.ALL_SHEETS) {
            int[] size = PngInfo.dimensions(EmojiLinks.getSpriteFile(sheet));
            if (size != null) WIDTHS.put(sheet, size[0]);
        }
    }

    private static AtomicInteger generation(String sheet) {
        return GENERATION.computeIfAbsent(sheet, key -> new AtomicInteger());
    }

    static void loadResident() {
        recordWidths();
        load(EmojiLinks.CUSTOM_SHEET, null);
        selectTheme(configuredTheme());
    }

    private static void load(String sheet, Runnable afterSuccess) {
        if (sheet == null) return;
        if (RESIDENT.contains(sheet)) {
            onClient(afterSuccess);
            return;
        }
        if (retryBlocked(sheet) || !PENDING.add(sheet)) return;
        ThreadUtils.run("EmojiSheet-Load-" + sheet, () -> {
            int gen = generation(sheet).get();
            try {
                BufferedImage image = ImageIO.read(EmojiLinks.getSpriteFile(sheet));
                if (image == null) throw new IOException("unreadable");
                int width = image.getWidth();
                Object prepared = TextureCompat.prepareImage(image);
                onClient(() -> upload(sheet, prepared, width, gen, afterSuccess));
            } catch (Throwable t) {
                FAILED_AT.put(sheet, System.currentTimeMillis());
                PENDING.remove(sheet);
                if (!EmojiLinks.getSpriteFile(sheet).isFile()) return;
                Aetheria.logger.warning("[EMOJI] Error reading " + sheet + " - " + t);
                if (!(t instanceof Error)) repair(sheet);
            }
        });
    }

    private static void upload(String sheet, Object prepared, int width, int gen, Runnable afterSuccess) {
        if (gen != generation(sheet).get()) {
            PENDING.remove(sheet);
            ThreadUtils.run("EmojiSheet-Reload", EmojiSheetStore::loadResident);
            return;
        }
        try {
            TextureCompat.uploadPrepared(EmojiLinks.getSpriteResource(sheet), prepared);
            WIDTHS.put(sheet, width);
            RESIDENT.add(sheet);
            FAILED_AT.remove(sheet);
            if (afterSuccess != null) afterSuccess.run();
        } catch (Exception e) {
            FAILED_AT.put(sheet, System.currentTimeMillis());
            Aetheria.logger.warning("[EMOJI] Error uploading " + sheet + " - " + e.getMessage());
        } finally {
            PENDING.remove(sheet);
        }
    }

    static void selectTheme(String sheet) {
        if (sheet == null) return;
        if (sheet.equals(drawn) && RESIDENT.contains(sheet)) return;
        load(sheet, () -> {
            if (!sheet.equals(configuredTheme())) {
                release(sheet);
                return;
            }
            String previous = drawn;
            drawn = sheet;
            if (previous != null && !previous.equals(sheet) && !EmojiLinks.CUSTOM_SHEET.equals(previous)) {
                release(previous);
            }
        });
    }

    private static void release(String sheet) {
        TextureCompat.releaseTexture(EmojiLinks.getSpriteResource(sheet));
        RESIDENT.remove(sheet);
    }

    private static void onClient(Runnable task) {
        if (task == null) return;
        try {
            MinecraftCompat.getMinecraft().addScheduledTask(task);
        } catch (RuntimeException e) {
            DEFERRED.add(task);
        }
    }

    static void drainDeferred() {
        Runnable task;
        while ((task = DEFERRED.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                Aetheria.logger.warning("[EMOJI] Deferred client task failed - " + e.getMessage());
            }
        }
    }

    private static boolean retryBlocked(String sheet) {
        File file = EmojiLinks.getSpriteFile(sheet);
        if (!file.isFile() || file.length() == 0) return true;
        Long failedAt = FAILED_AT.get(sheet);
        return failedAt != null && System.currentTimeMillis() - failedAt < RETRY_DELAY_MS;
    }

    static void ensureGrid(int columns) {
        dataColumns = columns;
        recordWidths();
        int expected = columns * EmojiLinks.SHEET_CELL;

        List<String> mismatched = new ArrayList<>();
        boolean allPresent = true;
        for (String sheet : EmojiLinks.DEFAULT_SHEETS) {
            if (WIDTHS.getOrDefault(sheet, 0) == expected) continue;
            mismatched.add(sheet);
            if (!EmojiLinks.getSpriteFile(sheet).isFile()) allPresent = false;
        }
        if (mismatched.isEmpty()) return;

        if (allPresent && !DOWNLOADING.get()) {
            Aetheria.logger.warning("[EMOJI] Cached sheets do not match the emoji data (" + columns + " cols = "
                    + expected + "px) - re-downloading");
        }
        request(mismatched, null);
    }

    private static boolean gridMatches(int expectedWidth) {
        for (String sheet : EmojiLinks.DEFAULT_SHEETS) {
            if (WIDTHS.getOrDefault(sheet, 0) != expectedWidth) return false;
        }
        return true;
    }

    static void ensureCustomSheet(int maxRequired) {
        REQUIRED.accumulateAndGet(maxRequired, Math::max);
        if (width(EmojiLinks.CUSTOM_SHEET) >= maxRequired) return;
        repair(EmojiLinks.CUSTOM_SHEET);
    }
}
