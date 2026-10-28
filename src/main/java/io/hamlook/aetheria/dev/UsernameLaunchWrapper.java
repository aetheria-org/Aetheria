package io.hamlook.aetheria.dev;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Applies the optional -Dusername value to the 1.8.9 development launch.
 * This is a launcher helper and is not used in the packaged mod.
 */
public final class UsernameLaunchWrapper {
    private UsernameLaunchWrapper() {}

    public static void main(String[] args) throws Throwable {
        String username = System.getProperty("username");
        if (username != null && !username.trim().isEmpty()) {
            args = withUsername(args, username.trim());
        }

        Method launchMain = Class.forName("net.minecraft.launchwrapper.Launch")
                .getMethod("main", String[].class);
        try {
            launchMain.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    private static String[] withUsername(String[] args, String username) {
        List<String> launchArgs = new ArrayList<>(Arrays.asList(args));
        for (int i = 0; i < launchArgs.size(); i++) {
            if ("--username".equals(launchArgs.get(i))) {
                if (i + 1 < launchArgs.size()) {
                    launchArgs.set(i + 1, username);
                } else {
                    launchArgs.add(username);
                }
                return launchArgs.toArray(new String[0]);
            }
        }

        launchArgs.add("--username");
        launchArgs.add(username);
        return launchArgs.toArray(new String[0]);
    }
}
