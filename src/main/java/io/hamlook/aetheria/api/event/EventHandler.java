package io.hamlook.aetheria.api.event;

import io.hamlook.aetheria.Aetheria;

import lombok.Getter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class EventHandler {

    private final String name;
    private final List<Listener> listeners;
    private final boolean canReceiveCancelled;
    @Getter
    private int invokeCount;

    private static final Set<String> disabledHandlers = ConcurrentHashMap.newKeySet();
    private static final Set<String> disabledInvokers = ConcurrentHashMap.newKeySet();

    EventHandler(String name, List<Listener> listeners) {
        this.name = name;
        List<Listener> sorted = new ArrayList<>(listeners);
        sorted.sort(Comparator.comparingInt(l -> l.priority));
        this.listeners = sorted;
        this.canReceiveCancelled = sorted.stream().anyMatch(l -> l.receiveCancelled);
    }

    public boolean post(AetheriaEvent event) {
        if (listeners.isEmpty()) return false;
        if (disabledHandlers.contains(name)) return false;
        int errors = 0;
        for (Listener listener : listeners) {
            if (event.isCancelled() && !canReceiveCancelled) break;
            if (disabledInvokers.contains(listener.name)) continue;
            try {
                listener.method.invoke(listener.instance, event);
            } catch (InvocationTargetException e) {
                errors++;
                if (errors <= 3) {
                    Aetheria.logger.log(Level.WARNING, "[ATHR] Error in event handler " + listener.name, e.getCause());
                }
            } catch (IllegalAccessException e) {
                errors++;
                if (errors <= 3) {
                    Aetheria.logger.log(Level.WARNING, "[ATHR] Illegal access in event handler " + listener.name, e);
                }
            }
        }
        if (errors > 3) {
            Aetheria.logger.warning("[ATHR] " + errors + " total errors in event handler (3 shown above)");
        }
        return event.isCancelled();
    }

    public static void disableHandler(String handlerName) {
        disabledHandlers.add(handlerName);
    }

    public static void enableHandler(String handlerName) {
        disabledHandlers.remove(handlerName);
    }

    public static void disableInvoker(String invokerName) {
        disabledInvokers.add(invokerName);
    }

    public static void enableInvoker(String invokerName) {
        disabledInvokers.remove(invokerName);
    }

    public static boolean isHandlerDisabled(String handlerName) {
        return disabledHandlers.contains(handlerName);
    }

    public static boolean isInvokerDisabled(String invokerName) {
        return disabledInvokers.contains(invokerName);
    }

    static class Listener {

        final String name;
        final Object instance;
        final Method method;
        final int priority;
        final boolean receiveCancelled;

        Listener(String name, Object instance, Method method, int priority, boolean receiveCancelled) {
            this.name = name;
            this.instance = instance;
            this.method = method;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
        }
    }
}
