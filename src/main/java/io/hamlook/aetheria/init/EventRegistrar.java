package io.hamlook.aetheria.init;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.api.event.AetheriaEventBus;
import io.hamlook.aetheria.utils.compat.CommandCompat;
import io.hamlook.aetheria.utils.compat.KeybindCompat;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommand;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Auto-discovers all classes in the {@code io.hamlook.aetheria} package at mod
 * initialization and registers them based on annotations:
 * <ul>
 *   <li>{@link RegisterEvents {@code @RegisterEvents}} → {@link
 *       MinecraftForge#EVENT_BUS}</li>
 *   <li>{@link RegisterCommand {@code @RegisterCommand}} → {@link
 *       ClientCommandHandler}</li>
 *   <li>{@link RegisterInstance {@code @RegisterInstance}} → command or event bus
 *       depending on field type</li>
 *   <li>{@link RegisterKeybind {@code @RegisterKeybind}} → {@link
 *       ClientRegistry#registerKeyBinding}</li>
 * </ul>
 */
public class EventRegistrar {

    private static final String BASE_PACKAGE = "io.hamlook.aetheria";

    private static final List<Object> REGISTERED_EVENT_INSTANCES = new ArrayList<>();

    public static List<Object> getRegisteredEventInstances() {
        return Collections.unmodifiableList(REGISTERED_EVENT_INSTANCES);
    }

    public static void removeRegisteredInstance(Object instance) {
        REGISTERED_EVENT_INSTANCES.remove(instance);
    }

    public static void addRegisteredInstance(Object instance) {
        REGISTERED_EVENT_INSTANCES.add(instance);
    }

    public static void stopAllListeners() {
        for (Object instance : REGISTERED_EVENT_INSTANCES) {
            MinecraftForge.EVENT_BUS.unregister(instance);
        }
    }
    public static void reloadAllListeners() {
        for (Object instance : REGISTERED_EVENT_INSTANCES) {
            MinecraftForge.EVENT_BUS.unregister(instance);
            AetheriaEventBus.INSTANCE.unregister(instance);
            MinecraftForge.EVENT_BUS.register(instance);
            AetheriaEventBus.INSTANCE.register(instance);
        }
    }

    /**
     * Scans the classpath, loads every discovered class, and attempts each of the
     * four registration paths on it.
     */
    public static void registerAll() {
        List<String> classNames = ClasspathScanner.findClassNames(EventRegistrar.class, BASE_PACKAGE);
        List<Class<?>> classes = ClasspathScanner.loadClasses(classNames, EventRegistrar.class.getClassLoader());
        for (Class<?> clazz : classes) {
            tryRegisterEvents(clazz);
            tryRegisterCommand(clazz);
            tryRegisterInstanceFields(clazz);
            tryRegisterKeybindFields(clazz);
        }
    }

    /**
     * If {@code clazz} is annotated with {@code @RegisterEvents}, instantiate it
     * and subscribe it to {@link MinecraftForge#EVENT_BUS}.
     */
    private static void tryRegisterEvents(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(RegisterEvents.class)) return;
        try {
            Object instance = newInstance(clazz);
            MinecraftForge.EVENT_BUS.register(instance);
            AetheriaEventBus.INSTANCE.register(instance);
            REGISTERED_EVENT_INSTANCES.add(instance);
        } catch (Throwable t) {
            Aetheria.logger.severe("[ATHR] Failed to register events for " + clazz.getName() + ": " + t.getMessage());
        }
    }

    /**
     * If {@code clazz} is annotated with {@code @RegisterCommand} and implements
     * {@link ICommand}, instantiate and register it with
     * {@link ClientCommandHandler}.
     */
    private static void tryRegisterCommand(Class<?> clazz) {
        if (!clazz.isAnnotationPresent(RegisterCommand.class)) return;
        if (!ICommand.class.isAssignableFrom(clazz)) {
            Aetheria.logger.severe("[ATHR] @RegisterCommand class does not implement ICommand: " + clazz.getName());
            return;
        }
        try {
            CommandCompat.registerCommand((ICommand) newInstance(clazz));
        } catch (Throwable t) {
            Aetheria.logger.severe("[ATHR] Failed to register command: " + clazz.getName() + ": " + t.getMessage());
        }
    }

    /**
     * For every {@code @RegisterInstance} static field in {@code clazz}, read the
     * field value and register it, as a command if it implements {@link ICommand},
     * otherwise as an event-bus listener.
     */
    private static void tryRegisterInstanceFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(RegisterInstance.class)) continue;
            if (!Modifier.isStatic(field.getModifiers())) {
                Aetheria.logger.severe("[ATHR] @RegisterInstance field must be static: " + clazz.getName() + "." + field.getName());
                continue;
            }
            try {
                field.setAccessible(true);
                Object instance = field.get(null);
                if (instance == null) {
                    Aetheria.logger.severe("[ATHR] @RegisterInstance field is null: " + field.getName());
                    continue;
                }
                if (instance instanceof ICommand) {
                    CommandCompat.registerCommand((ICommand) instance);
                } else {
                    MinecraftForge.EVENT_BUS.register(instance);
                    AetheriaEventBus.INSTANCE.register(instance);
                    REGISTERED_EVENT_INSTANCES.add(instance);
                }
            } catch (Throwable t) {
                Aetheria.logger.severe("[ATHR] Failed to register instance field: " + field.getName() + ": " + t.getMessage());
            }
        }
    }

    /**
     * For every {@code @RegisterKeybind} static {@link KeyBinding} field in
     * {@code clazz}, read the field value and register it with
     * {@link ClientRegistry#registerKeyBinding}.
     */
    private static void tryRegisterKeybindFields(Class<?> clazz) {
        for (Field field : clazz.getDeclaredFields()) {
            if (!field.isAnnotationPresent(RegisterKeybind.class)) continue;
            if (!Modifier.isStatic(field.getModifiers())) {
                Aetheria.logger.severe("[ATHR] @RegisterKeybind field must be static: " + clazz.getName() + "." + field.getName());
                continue;
            }
            if (!KeyBinding.class.isAssignableFrom(field.getType())) {
                Aetheria.logger.severe("[ATHR] @RegisterKeybind field is not a KeyBinding: " + field.getName());
                continue;
            }
            try {
                field.setAccessible(true);
                KeyBinding key = (KeyBinding) field.get(null);
                if (key == null) {
                    Aetheria.logger.severe("[ATHR] @RegisterKeybind field is null: " + field.getName());
                    continue;
                }
                KeybindCompat.registerKeyBinding(key);
            } catch (Throwable t) {
                Aetheria.logger.severe("[ATHR] Failed to register keybind: " + field.getName() + ": " + t.getMessage());
            }
        }
    }

    private static Object newInstance(Class<?> clazz) throws Exception {
        try {
            Field f = clazz.getDeclaredField("INSTANCE");
            if (Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && f.getType() == clazz) {
                f.setAccessible(true);
                return f.get(null);
            }
        } catch (NoSuchFieldException ignored) {}
        Constructor<?> c = clazz.getDeclaredConstructor();
        c.setAccessible(true);
        return c.newInstance();
    }
}