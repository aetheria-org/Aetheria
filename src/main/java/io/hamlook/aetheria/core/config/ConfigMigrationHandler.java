package io.hamlook.aetheria.core.config;

public interface ConfigMigrationHandler {
    int version();
    void apply(ConfigFixContext ctx);
}
