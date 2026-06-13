// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Structural:
 * <ul>
 *   <li>{@link Category}              – marks a field as a config category</li>
 *   <li>{@link ConfigOption}          – marks a field as a configurable option</li>
 *   <li>{@link ConfigAccordionId}     – assigns an option to an accordion group</li>
 *   <li>{@link ConfigEditorAccordion} – marks a field as the accordion toggle</li>
 * </ul>
 * Editor types (one per field):
 * <ul>
 *   <li>{@link ConfigEditorBoolean}</li>
 *   <li>{@link ConfigEditorButton}</li>
 *   <li>{@link ConfigEditorColour}</li>
 *   <li>{@link ConfigEditorDraggableList}</li>
 *   <li>{@link ConfigEditorDropdown}</li>
 *   <li>{@link ConfigEditorKeybind}</li>
 *   <li>{@link ConfigEditorSliderAnnotation}</li>
 *   <li>{@link ConfigEditorStyle}</li>
 *   <li>{@link ConfigEditorText}</li>
 *   <li>{@link ConfigEditorVersionDisplay} – renders the mod version + update check button</li>
 * </ul>
 */
public final class ConfigAnnotations {

    private ConfigAnnotations() {
    }

    // Structural

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category {
        String name();

        String desc();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigOption {
        String name();

        String desc();

        int subcategoryId() default -1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigAccordionId {
        int id();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorAccordion {
        int id();
    }

    // Editor types

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorBoolean {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorButton {
        String runnableId() default "";

        String buttonText() default "";
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorColour {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorDraggableList {
        String[] exampleText();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorDropdown {
        String[] values();

        int initialIndex() default 0;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorKeybind {
        int defaultKey();
    }

    /**
     * Named SliderAnnotation to avoid clashing with the GuiElementSlider class.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorSliderAnnotation {
        float minValue();

        float maxValue();

        float minStep();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorStyle {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorText {
    }

    /**
     * Renders the current mod version at 2× scale with a "Check for Updates" button.
     * Use on a {@code transient Void} field — nothing is serialised.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorVersionDisplay {
    }
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigEditorTextDisplay {
        String text();
    }
}