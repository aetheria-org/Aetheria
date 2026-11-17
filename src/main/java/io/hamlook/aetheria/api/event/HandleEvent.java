package io.hamlook.aetheria.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an Aetheria event handler. Dispatched by {@link EventHandler}.
 *
 * {@code receiveCancelled} is enforced per-listener: a listener with the default
 * {@code false} will never receive events that were already cancelled at dispatch
 * time or cancelled earlier in the same dispatch chain by another listener, regardless
 * of whether other listeners on the same event type have opted in.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HandleEvent {

    int HIGHEST = -2;
    int HIGH = -1;
    int NORMAL = 0;
    int LOW = 1;
    int LOWEST = 2;

    int priority() default NORMAL;

    boolean receiveCancelled() default false;
}
