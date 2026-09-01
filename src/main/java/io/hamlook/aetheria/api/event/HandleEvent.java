package io.hamlook.aetheria.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
