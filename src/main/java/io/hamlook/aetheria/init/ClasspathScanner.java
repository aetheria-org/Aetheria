package io.hamlook.aetheria.init;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClasspathScanner {

    private ClasspathScanner() {
    }

    public static List<String> findClassNames(Class<?> referenceClass, String basePackage, Predicate<String> filter) {
        try {
            URL location = referenceClass.getProtectionDomain().getCodeSource().getLocation();
            Path root = Paths.get(resolveRoot(referenceClass, location).toURI());
            Predicate<String> effectiveFilter = fqn -> fqn.startsWith(basePackage + ".") && !fqn.contains("$") && (filter == null || filter.test(fqn));

            List<String> names = new ArrayList<>();
            if (Files.isDirectory(root)) {
                scanDirectory(root, basePackage.replace('.', '/'), effectiveFilter, names);
            } else {
                scanJar(root, effectiveFilter, names);
            }
            return names;
        } catch (Exception e) {
            log("Failed to scan classes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<String> findClassNames(Class<?> referenceClass, String basePackage) {
        return findClassNames(referenceClass, basePackage, null);
    }

    public static List<Class<?>> loadClasses(List<String> classNames, ClassLoader loader) {
        List<Class<?>> classes = new ArrayList<>();
        for (String name : classNames) {
            try {
                classes.add(Class.forName(name, true, loader));
            } catch (Throwable ignored) {
            }
        }
        return classes;
    }

    static URL resolveRoot(Class<?> referenceClass, URL location) throws MalformedURLException {
        String s = location.toString();
        if (location.getProtocol().equals("jar")) {
            return new URL(s.substring(4).split("!")[0]);
        }
        if (s.endsWith(".class")) {
            return new URL(s.replace("\\", "/").replace(referenceClass.getCanonicalName().replace(".", "/") + ".class", ""));
        }
        return location;
    }

    static void scanDirectory(Path root, String basePath, Predicate<String> filter, List<String> out) throws IOException {
        Path target = root.resolve(basePath);
        if (!Files.exists(target)) return;
        try (Stream<Path> stream = Files.walk(target)) {
            stream.map(p -> root.relativize(p).toString()).filter(p -> p.endsWith(".class")).map(ClasspathScanner::toClassName).filter(filter).forEach(out::add);
        }
    }

    static void scanJar(Path jar, Predicate<String> filter, List<String> out) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(jar))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                if (name.endsWith(".class")) {
                    String fqn = toClassName(name);
                    if (filter.test(fqn)) out.add(fqn);
                }
                zis.closeEntry();
            }
        }
    }

    static String toClassName(String path) {
        return path.substring(0, path.length() - 6).replace('\\', '/').replace('/', '.');
    }

    private static void log(String msg) {
        try {
            io.hamlook.aetheria.Aetheria.logger.severe("[ClasspathScanner] " + msg);
        } catch (Throwable t) {
            System.err.println("[ClasspathScanner] " + msg);
        }
    }
}