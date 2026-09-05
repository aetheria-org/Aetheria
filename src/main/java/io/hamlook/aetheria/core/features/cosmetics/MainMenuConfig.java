package io.hamlook.aetheria.core.features.cosmetics;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;
import org.lwjgl.input.Keyboard;

public class MainMenuConfig {

    @ConfigOption(name = "Enable Custom Main Menu",desc = "Enable/Disable the Custom Main Menu")
    @ConfigEditorBoolean
    public boolean enabled = true;

    @ConfigOption(name = "Editor: Show Grid", desc = "Show grid in editor")
    @ConfigEditorBoolean
    public boolean showGrid = true;

    @ConfigOption(name = "Editor: Grid Size", desc = "Grid snap size in pixels")
    @ConfigEditorSliderAnnotation(minValue = 5, maxValue = 50, minStep = 1)
    public int gridSize = 10;

    @ConfigOption(name = "Editor: Preview Mode", desc = "Hide editor UI to preview menu")
    @ConfigEditorBoolean
    public boolean previewMode = false;

    @ConfigOption(name = "Editor: Snap to Grid", desc = "Snap to grid by default (hold Shift to free move)")
    @ConfigEditorBoolean
    public boolean snapByDefault = false;

    @ConfigOption(name = "Editor: Show Alignment Guides", desc = "Show alignment lines when snapping")
    @ConfigEditorBoolean
    public boolean showAlignmentGuides = true;

    @ConfigOption(name = "Key: Save", desc = "Save config key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_C)
    public int keySave = Keyboard.KEY_C;

    @ConfigOption(name = "Key: Undo", desc = "Undo key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Z)
    public int keyUndo = Keyboard.KEY_Z;

    @ConfigOption(name = "Key: Redo", desc = "Redo key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_Y)
    public int keyRedo = Keyboard.KEY_Y;

    @ConfigOption(name = "Key: Delete", desc = "Delete selected key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_DELETE)
    public int keyDelete = Keyboard.KEY_DELETE;

    @ConfigOption(name = "Key: Duplicate", desc = "Duplicate selected key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_D)
    public int keyDuplicate = Keyboard.KEY_D;

    @ConfigOption(name = "Key: Toggle Preview", desc = "Toggle preview mode key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_P)
    public int keyPreview = Keyboard.KEY_P;

    @ConfigOption(name = "Key: Toggle Grid", desc = "Toggle grid visibility key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_G)
    public int keyGrid = Keyboard.KEY_G;

    @ConfigOption(name = "Key: Select All", desc = "Select all elements key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_A)
    public int keySelectAll = Keyboard.KEY_A;

    @ConfigOption(name = "Key: Export", desc = "Export config to clipboard key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_E)
    public int keyExport = Keyboard.KEY_E;

    @ConfigOption(name = "Key: Import", desc = "Import config from clipboard key")
    @ConfigEditorKeybind(defaultKey = Keyboard.KEY_I)
    public int keyImport = Keyboard.KEY_I;
}
