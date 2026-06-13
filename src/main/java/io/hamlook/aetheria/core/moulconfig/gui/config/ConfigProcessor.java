// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui.config;

import com.google.gson.annotations.Expose;
import io.hamlook.aetheria.core.moulconfig.editors.*;
import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;

public class ConfigProcessor {

    public static LinkedHashMap<String, ProcessedCategory> create(Object config) {
        LinkedHashMap<String, ProcessedCategory> processedConfig = new LinkedHashMap<>();

        for (Field categoryField : config.getClass().getDeclaredFields()) {
            if (!categoryField.isAnnotationPresent(Expose.class)) continue;
            if (!categoryField.isAnnotationPresent(Category.class)) continue;

            Object categoryObj;
            try { categoryObj = categoryField.get(config); } catch (Exception e) { continue; }

            Category categoryAnnotation = categoryField.getAnnotation(Category.class);
            ProcessedCategory cat = new ProcessedCategory(categoryAnnotation.name(), categoryAnnotation.desc());
            processedConfig.put(categoryField.getName(), cat);

            // Scan fields inside the category class
            for (Field field : categoryObj.getClass().getDeclaredFields()) {

                // Sub-category: a @Category-annotated field inside the category class
                if (field.isAnnotationPresent(Category.class)) {
                    Object subObj;
                    try { subObj = field.get(categoryObj); } catch (Exception e) { continue; }

                    Category subAnnotation = field.getAnnotation(Category.class);
                    ProcessedSubcategory sub = new ProcessedSubcategory(subAnnotation.name(), subAnnotation.desc());
                    cat.subcategories.put(field.getName(), sub);

                    processSubcategoryOptions(subObj, sub, config);
                    continue;
                }

                // Top-level option inside category
                if (!field.isAnnotationPresent(ConfigOption.class)) continue;

                ConfigOption optionAnnotation = field.getAnnotation(ConfigOption.class);
                ProcessedOption option = new ProcessedOption(optionAnnotation.name(), optionAnnotation.desc(), optionAnnotation.subcategoryId(), field, categoryObj);
                if (field.isAnnotationPresent(ConfigAccordionId.class)) {
                    option.accordionId = field.getAnnotation(ConfigAccordionId.class).id();
                }

                GuiOptionEditor editor = buildEditor(option, field, config);
                if (editor == null) continue;
                option.editor = editor;
                cat.options.put(field.getName(), option);
            }
        }
        return processedConfig;
    }

    private static void processSubcategoryOptions(Object subObj, ProcessedSubcategory sub, Object config) {
        for (Field subField : subObj.getClass().getDeclaredFields()) {
            if (subField.isAnnotationPresent(Category.class)) {
                Object subSubObj;
                try { subSubObj = subField.get(subObj); } catch (Exception e) { continue; }

                Category subSubAnnotation = subField.getAnnotation(Category.class);
                ProcessedSubcategory subSub = new ProcessedSubcategory(subSubAnnotation.name(), subSubAnnotation.desc());
                sub.subcategories.put(subField.getName(), subSub);

                processSubcategoryOptions(subSubObj, subSub, config);
                continue;
            }

            if (!subField.isAnnotationPresent(ConfigOption.class)) continue;
            ConfigOption opt = subField.getAnnotation(ConfigOption.class);
            ProcessedOption option = new ProcessedOption(opt.name(), opt.desc(), opt.subcategoryId(), subField, subObj);
            GuiOptionEditor editor = buildEditor(option, subField, config);
            if (editor == null) continue;
            option.editor = editor;
            sub.options.put(subField.getName(), option);
        }
    }

    private static GuiOptionEditor buildEditor(ProcessedOption option, Field field, Object config) {
        GuiOptionEditor editor = null;
        Class<?> t = field.getType();

        if (field.isAnnotationPresent(ConfigEditorVersionDisplay.class))
            editor = new GuiOptionEditorVersionDisplay(option);
        if (field.isAnnotationPresent(ConfigEditorTextDisplay.class)) {
            ConfigEditorTextDisplay a = field.getAnnotation(ConfigEditorTextDisplay.class);
            editor = new GuiOptionEditorTextDisplay(option,a.text());
        }
        if (editor == null && t == int.class && field.isAnnotationPresent(ConfigEditorKeybind.class))
            editor = new GuiOptionEditorKeybind(option, (int) option.get(), field.getAnnotation(ConfigEditorKeybind.class).defaultKey());
        if (editor == null && field.isAnnotationPresent(ConfigEditorButton.class)) {
            ConfigEditorButton a = field.getAnnotation(ConfigEditorButton.class);
            editor = new GuiOptionEditorButton(option, a.runnableId(), a.buttonText(), config);
        }
        if (editor == null && t == boolean.class && field.isAnnotationPresent(ConfigEditorBoolean.class))
            editor = new GuiOptionEditorBoolean(option);
        if (editor == null && t == boolean.class && field.isAnnotationPresent(ConfigEditorAccordion.class))
            editor = new GuiOptionEditorAccordion(option, field.getAnnotation(ConfigEditorAccordion.class).id());
        if (editor == null && t == int.class) {
            if (field.isAnnotationPresent(ConfigEditorDropdown.class))
                editor = new GuiOptionEditorDropdown(option, field.getAnnotation(ConfigEditorDropdown.class).values(), (int) option.get(), true);
            else if (field.isAnnotationPresent(ConfigEditorStyle.class))
                editor = new GuiOptionEditorStyle(option, (int) option.get());
        }
        if (editor == null && List.class.isAssignableFrom(t) && field.isAnnotationPresent(ConfigEditorDraggableList.class))
            editor = new GuiOptionEditorDraggableList(option, field.getAnnotation(ConfigEditorDraggableList.class).exampleText());
        if (editor == null && t == String.class) {
            if (field.isAnnotationPresent(ConfigEditorDropdown.class)) {
                ConfigEditorDropdown a = field.getAnnotation(ConfigEditorDropdown.class);
                editor = new GuiOptionEditorDropdown(option, a.values(), a.initialIndex(), false);
            } else if (field.isAnnotationPresent(ConfigEditorColour.class))
                editor = new GuiOptionEditorColour(option);
            else if (field.isAnnotationPresent(ConfigEditorText.class))
                editor = new GuiOptionEditorText(option);
        }
        if (editor == null && (t == int.class || t == float.class || t == double.class) && field.isAnnotationPresent(ConfigEditorSliderAnnotation.class)) {
            ConfigEditorSliderAnnotation a = field.getAnnotation(ConfigEditorSliderAnnotation.class);
            editor = new GuiOptionEditorSlider(option, a.minValue(), a.maxValue(), a.minStep());
        }
        return editor;
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class ProcessedCategory {
        public final String name;
        public final String desc;
        public final LinkedHashMap<String, ProcessedOption> options = new LinkedHashMap<>();
        public final LinkedHashMap<String, ProcessedSubcategory> subcategories = new LinkedHashMap<>();

        public ProcessedCategory(String name, String desc) { this.name = name; this.desc = desc; }
    }

    public static class ProcessedSubcategory {
        public final String name;
        public final String desc;
        public final LinkedHashMap<String, ProcessedOption> options = new LinkedHashMap<>();
        public final LinkedHashMap<String, ProcessedSubcategory> subcategories = new LinkedHashMap<>();

        public ProcessedSubcategory(String name, String desc) { this.name = name; this.desc = desc; }
    }

    public static class ProcessedOption {
        public final String name;
        public final String desc;
        public final int subcategoryId;
        private final Field field;
        private final Object container;
        public GuiOptionEditor editor;
        public int accordionId = -1;

        public ProcessedOption(String name, String desc, int subcategoryId, Field field, Object container) {
            this.name = name; this.desc = desc; this.subcategoryId = subcategoryId;
            this.field = field; this.container = container;
        }

        public Object get() {
            try { return field.get(container); } catch (Exception e) { return null; }
        }

        public boolean set(Object value) {
            try {
                if (field.getType() == int.class && value instanceof Number)
                    field.set(container, ((Number) value).intValue());
                else
                    field.set(container, value);
                return true;
            } catch (Exception e) { e.printStackTrace(); return false; }
        }
    }
}
