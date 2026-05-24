package io.hamlook.aetheria.features.misc.pet;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

final class PetFileValidator {

    static final String HEADER = "// Aetheria Pet";

    private PetFileValidator() {
    }

    static void deleteIfLegacy(File file) {
        if (file == null || !file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String first = br.readLine();
            if (first == null || !first.trim().equals(HEADER)) {
                file.delete();
                System.out.println("[Aetheria/Pet] Deleted legacy file: " + file.getName());
            }
        } catch (Exception e) {
            file.delete();
            System.err.println("[Aetheria/Pet] Deleted unreadable file: " + file.getName());
        }
    }

    static <T> T load(File file, Type type) {
        if (file == null || !file.exists()) return null;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            br.readLine(); // skip header
            return io.hamlook.aetheria.core.GsonBuilder.GSON.fromJson(br, type);
        } catch (Exception e) {
            System.err.println("[Aetheria/Pet] Failed to load " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    static void save(File file, Object data) {
        if (file == null) return;
        file.getParentFile().mkdirs();
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try (Writer w = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(tmp.toPath(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING), StandardCharsets.UTF_8))) {
            w.write(HEADER);
            w.write('\n');
            io.hamlook.aetheria.core.GsonBuilder.GSON.toJson(data, w);
            w.flush();
        } catch (Exception e) {
            System.err.println("[Aetheria/Pet] Failed to write " + tmp.getName() + ": " + e.getMessage());
            tmp.delete();
            return;
        }
        try {
            try {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("[Aetheria/Pet] Failed to commit " + file.getName() + ": " + e.getMessage());
            tmp.delete();
        }
    }
}
