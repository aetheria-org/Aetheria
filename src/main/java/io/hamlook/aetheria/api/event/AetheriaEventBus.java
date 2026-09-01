package io.hamlook.aetheria.api.event;

import io.hamlook.aetheria.Aetheria;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AetheriaEventBus {

    public static final AetheriaEventBus INSTANCE = new AetheriaEventBus();

    private final Map<Class<? extends AetheriaEvent>, List<EventHandler.Listener>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<? extends AetheriaEvent>, EventHandler> handlers = new ConcurrentHashMap<>();
    private final Map<Object, List<RegisteredListener>> instanceListeners = new ConcurrentHashMap<>();

    public void register(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getMethods()) {
            HandleEvent annotation = method.getAnnotation(HandleEvent.class);
            if (annotation == null) continue;
            if (method.getParameterCount() != 1) {
                Aetheria.logger.warning("[ATHR] @HandleEvent method must have exactly 1 parameter: " + clazz.getName() + "." + method.getName());
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!AetheriaEvent.class.isAssignableFrom(paramType)) {
                Aetheria.logger.warning("[ATHR] @HandleEvent parameter must extend AetheriaEvent: " + clazz.getName() + "." + method.getName());
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends AetheriaEvent> eventType = (Class<? extends AetheriaEvent>) paramType;
            method.setAccessible(true);

            String listenerName = clazz.getSimpleName() + "#" + method.getName();
            boolean receiveCancelled = annotation.receiveCancelled();
            int priority = annotation.priority();

            EventHandler.Listener listener = new EventHandler.Listener(listenerName, instance, method, priority, receiveCancelled);
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
            instanceListeners.computeIfAbsent(instance, k -> new ArrayList<>()).add(new RegisteredListener(eventType, listenerName));
            handlers.remove(eventType);
        }
    }

    public void unregister(Object instance) {
        List<RegisteredListener> registered = instanceListeners.remove(instance);
        if (registered == null) return;
        for (RegisteredListener rl : registered) {
            List<EventHandler.Listener> list = listeners.get(rl.eventType);
            if (list != null) {
                list.removeIf(l -> l.name.equals(rl.name));
                handlers.remove(rl.eventType);
            }
        }
    }

    public boolean post(AetheriaEvent event) {
        return getHandler(event.getClass()).post(event);
    }

    private EventHandler getHandler(Class<? extends AetheriaEvent> eventClass) {
        return handlers.computeIfAbsent(eventClass, clazz -> {
            List<EventHandler.Listener> all = new ArrayList<>();
            Class<?> c = clazz;
            while (c != null && AetheriaEvent.class.isAssignableFrom(c)) {
                List<EventHandler.Listener> direct = listeners.get(c);
                if (direct != null) all.addAll(direct);
                c = c.getSuperclass();
            }
            assert clazz != null;
            String name = clazz.getSimpleName();
            if (name.isEmpty()) name = clazz.getName();
            return new EventHandler(name, all);
        });
    }

    private static class RegisteredListener {

        final Class<? extends AetheriaEvent> eventType;
        final String name;

        RegisteredListener(Class<? extends AetheriaEvent> eventType, String name) {
            this.eventType = eventType;
            this.name = name;
        }
    }
}
