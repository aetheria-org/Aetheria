package io.hamlook.aetheria.utils.item;

import io.hamlook.aetheria.utils.TimeLimitedCache;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;
import java.util.function.Function;

public final class CachedItemData {

    private static final long LORE_TTL_MS = 100L;
    private static final long NBT_TTL_MS = 1000L;
    private static final long IDLE_MS = 120_000L;
    private static final long MAX_ENTRIES = 2048L;

    private static final TimeLimitedCache<ItemStack, CachedItemData> CACHE =
            TimeLimitedCache.weakKeys(IDLE_MS, MAX_ENTRIES);

    public final Slot<List<String>> lore = new Slot<>(LORE_TTL_MS);
    public final Slot<List<String>> loreStripped = new Slot<>(LORE_TTL_MS);
    public final Slot<NbtData> nbt = new Slot<>(NBT_TTL_MS);
    public final Slot<String> displayName = new Slot<>(NBT_TTL_MS);

    private CachedItemData() {
    }

    public static CachedItemData of(ItemStack item) {
        return CACHE.getOrCompute(item, CachedItemData::new);
    }

    public static final class NbtData {

        public final NBTTagCompound extraAttributes;
        public final String internalName;
        public final String effectiveId;
        public final String uuid;

        public NbtData(NBTTagCompound extraAttributes, String internalName, String effectiveId, String uuid) {
            this.extraAttributes = extraAttributes;
            this.internalName = internalName;
            this.effectiveId = effectiveId;
            this.uuid = uuid;
        }
    }

    public static final class Slot<T> {

        private final long ttlNanos;
        private volatile Snapshot<T> current;

        private Slot(long ttlMs) {
            this.ttlNanos = ttlMs * 1_000_000L;
        }

        public <A> T get(A arg, Function<A, T> compute) {
            Snapshot<T> snapshot = current;
            if (snapshot != null && System.nanoTime() - snapshot.stampNanos < ttlNanos) return snapshot.value;
            T value = compute.apply(arg);
            current = new Snapshot<>(value);
            return value;
        }
    }

    private static final class Snapshot<T> {

        private final T value;
        private final long stampNanos;

        private Snapshot(T value) {
            this.value = value;
            this.stampNanos = System.nanoTime();
        }
    }
}
