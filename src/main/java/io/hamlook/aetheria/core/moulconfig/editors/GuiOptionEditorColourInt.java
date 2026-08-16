// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.editors;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigProcessor;

/**
 * Color picker editor for plain ARGB {@code int} config fields. The color is
 * round-tripped through the chroma color-string format with chroma speed 0, so
 * existing int config values keep working (no type migration needed).
 */
public class GuiOptionEditorColourInt extends GuiOptionEditorColour {

    public GuiOptionEditorColourInt(ConfigProcessor.ProcessedOption option) {
        super(option, ChromaColour.special(0, (argb(option) >>> 24) & 0xFF, argb(option) & 0xFFFFFF), val -> option.set(ChromaColour.specialToSimpleRGB(val)));
    }

    private static int argb(ConfigProcessor.ProcessedOption option) {
        return (Integer) option.get();
    }
}