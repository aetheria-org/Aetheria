package io.hamlook.aetheria.utils;

import io.hamlook.aetheria.Aetheria;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class ThreadUtils {
    public static final ExecutorService IO = createDynamicPool();

    private ThreadUtils() {
    }

    public static void run(Runnable task) {
        submit(null, task);
    }

    public static void run(String name, Runnable task) {
        submit(name, task);
    }

    private static void submit(String name, Runnable task) {
        final String taskName = name != null ? name : task.getClass().getSimpleName();
        IO.execute(() -> {
            try {
                task.run();
            } catch (Throwable t) {
                if (Aetheria.logger != null) {
                    Aetheria.logger.log(Level.SEVERE, "Async task '" + taskName + "' failed", t);
                } else {
                    t.printStackTrace();
                }
                if (t instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    private static ExecutorService createDynamicPool() {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DaemonThreadFactory("ATHR-Async"));
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-" + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
