package io.hamlook.aetheria.core.hotswap;

import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.init.EventRegistrar;
import moe.nea.hotswapagentforge.forge.ClassDefinitionEvent;
import moe.nea.hotswapagentforge.forge.HotswapEvent;
import moe.nea.hotswapagentforge.forge.HotswapFinishedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;

public class HotswapSupportImpl implements HotswapSupportHandle {

    private static void removeFinal(Field field) {
        try {
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void load() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onHotswapClass(ClassDefinitionEvent.Redefinition event) {
        List<Object> instances = EventRegistrar.getRegisteredEventInstances();
        Object instance = null;
        for (Object obj : instances) {
            if (obj.getClass().getName().equals(event.getFullyQualifiedName())) {
                instance = obj;
                break;
            }
        }
        if (instance == null) return;

        Aetheria.logger.info("[ATHR] HotSwap: refreshing " + instance.getClass().getSimpleName());

        MinecraftForge.EVENT_BUS.unregister(instance);

        Constructor<?> primaryConstructor = null;
        try {
            primaryConstructor = instance.getClass().getDeclaredConstructor();
        } catch (NoSuchMethodException ignored) {
        }

        if (primaryConstructor == null) {
            MinecraftForge.EVENT_BUS.register(instance);
            return;
        }

        primaryConstructor.setAccessible(true);
        Object newInstance;
        try {
            newInstance = primaryConstructor.newInstance();
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] HotSwap: failed to reconstruct " + instance.getClass().getSimpleName() + ": " + e.getMessage());
            MinecraftForge.EVENT_BUS.register(instance);
            return;
        }

        try {
            Field instanceField = instance.getClass().getDeclaredField("INSTANCE");
            if (instanceField.getType() == instance.getClass()) {
                instanceField.setAccessible(true);
                removeFinal(instanceField);
                instanceField.set(null, newInstance);
            }
        } catch (NoSuchFieldException ignored) {
        } catch (Exception e) {
            Aetheria.logger.warning("[ATHR] HotSwap: failed to re-inject INSTANCE for " + instance.getClass().getSimpleName() + ": " + e.getMessage());
        }

        EventRegistrar.removeRegisteredInstance(instance);
        EventRegistrar.addRegisteredInstance(newInstance);
        MinecraftForge.EVENT_BUS.register(newInstance);

        Aetheria.logger.info("[ATHR] HotSwap: reconstructed and re-registered " + instance.getClass().getSimpleName());
    }

    @SubscribeEvent
    public void onHotswapDetected(HotswapFinishedEvent event) {
        Aetheria.logger.info("[ATHR] HotSwap finished");
    }

    @Override
    public boolean isLoaded() {
        return HotswapEvent.isReady();
    }
}
