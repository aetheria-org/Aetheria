package io.hamlook.aetheria.init;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class AutoDiscoveryMixinPlugin implements IMixinConfigPlugin {

    private String mixinPackage;
    private List<String> mixins;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
    }

    @Override
    public List<String> getMixins() {
        if (mixins != null) return mixins;
        mixins = new ArrayList<>();
        try {
            URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
            System.out.println("[AutoDiscoveryMixinPlugin] Raw location: " + location);

            Path root = resolveRoot(location);
            System.out.println("[AutoDiscoveryMixinPlugin] Resolved root: " + root);

            String basePath = mixinPackage.replace('.', '/');
            Predicate<String> filter = fqn -> fqn.startsWith(mixinPackage + ".") && !fqn.contains("$");

            List<String> fqns = new ArrayList<>();
            if (Files.isDirectory(root)) {
                ClasspathScanner.scanDirectory(root, basePath, filter, fqns);
            } else {
                ClasspathScanner.scanJar(root, filter, fqns);
            }
            for (String fqn : fqns) {
                mixins.add(fqn.substring(mixinPackage.length() + 1));
            }
            System.out.println("[AutoDiscoveryMixinPlugin] Found mixins: " + mixins);
        } catch (Exception e) {
            System.out.println("[AutoDiscoveryMixinPlugin] FAILED: " + e);
            e.printStackTrace(System.out);
        }
        return mixins;
    }

    private Path resolveRoot(URL location) throws Exception {
        String s = location.toString();

        // jar:file:/path/mod.jar!/some/Class.class  or  jar:file:/path/mod.jar!/
        if (s.startsWith("jar:")) {
            String inner = s.substring(4).split("!")[0]; // file:/path/mod.jar
            return Paths.get(new URI(inner));
        }

        // file:/path/to/classes/ (directory, dev env)
        if (s.endsWith(".class")) {
            String stripped = s.replace("\\", "/").replace(getClass().getCanonicalName().replace(".", "/") + ".class", "");
            return Paths.get(new URI(stripped));
        }

        return Paths.get(new URI(s));
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String t, ClassNode tc, String mc, IMixinInfo mi) {
    }

    @Override
    public void postApply(String t, ClassNode tc, String mc, IMixinInfo mi) {
    }
}