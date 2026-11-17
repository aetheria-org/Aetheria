package io.hamlook.aetheria.core.config.migrations;

import io.hamlook.aetheria.core.config.ConfigFixContext;
import io.hamlook.aetheria.core.config.ConfigMigrationHandler;

public final class EnchantParserNested implements ConfigMigrationHandler {

    @Override
    public int version() {
        return 1;
    }

    @Override
    public void apply(ConfigFixContext ctx) {
        String b = "qol.enchantParser";
        ctx.move(b + ".enchantChroma", b + ".colors.enchantChroma");
        ctx.move(b + ".enchantChromaMode", b + ".colors.enchantChromaMode");
        ctx.move(b + ".enchantChromaSize", b + ".colors.enchantChromaSize");
        ctx.move(b + ".enchantPoorColor", b + ".colors.enchantPoorColor");
        ctx.move(b + ".enchantGoodColor", b + ".colors.enchantGoodColor");
        ctx.move(b + ".enchantGreatColor", b + ".colors.enchantGreatColor");
        ctx.move(b + ".enchantPerfectColor", b + ".colors.enchantPerfectColor");
        ctx.move(b + ".enchantUltimateColor", b + ".colors.enchantUltimateColor");
        ctx.move(b + ".hideEnchantDescriptions", b + ".descriptions.hideEnchantDescriptions");
    }
}
