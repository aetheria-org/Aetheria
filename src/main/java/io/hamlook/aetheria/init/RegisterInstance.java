package io.hamlook.aetheria.init;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Registers a static field value on both the Forge and Aetheria event buses
 * (or as a command if it implements {@link net.minecraft.command.ICommand}).
 *
 * <p>The annotated field must be non-null when {@link EventRegistrar#registerAll()}
 * runs. If the field is {@code private static} and lazily initialized, it may be
 * {@code null} at scan time and the handler silently never registers. For singleton
 * classes, prefer {@link RegisterEvents} on the class with a {@code private static
 * final INSTANCE} field instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RegisterInstance {}