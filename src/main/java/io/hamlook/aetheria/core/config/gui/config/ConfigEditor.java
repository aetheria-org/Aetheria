package io.hamlook.aetheria.core.config.gui.config;

import com.google.common.collect.Lists;
import io.hamlook.aetheria.core.ATHRConfig;
import io.hamlook.aetheria.core.config.editors.GuiOptionEditor;
import io.hamlook.aetheria.core.config.editors.GuiOptionEditorAccordion;
import io.hamlook.aetheria.core.config.gui.GlScissorStack;
import io.hamlook.aetheria.core.config.gui.GuiElement;
import io.hamlook.aetheria.core.config.gui.GuiElementTextField;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.LerpUtils.LerpingInteger;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import io.hamlook.aetheria.Aetheria;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import static io.hamlook.aetheria.core.config.gui.GuiTextures.DISCORD;
import static io.hamlook.aetheria.core.config.gui.GuiTextures.GITHUB;
import static io.hamlook.aetheria.core.config.gui.GuiTextures.SEARCH_ICON;

public class ConfigEditor extends GuiElement {
    private static final ResourceLocation[] socialsIco = new ResourceLocation[]{DISCORD, GITHUB};
    private static final String[] socialsLink = new String[]{"https://discord.gg/4zKq2RkSZS", "https://github.com/aetheria-org/Aetheria"};
    public static ConfigEditor editor = new ConfigEditor(ATHRConfig.feature);
    private final long openedMillis;
    private final LerpingInteger optionsScroll = new LerpingInteger(0, 150);
    private final LerpingInteger categoryScroll = new LerpingInteger(0, 150);
    private final LinkedHashMap<String, ConfigProcessor.ProcessedCategory> processedConfig;
    private final TreeMap<String, Set<ConfigProcessor.ProcessedOption>> searchOptionMap = new TreeMap<>();
    private final HashMap<ConfigProcessor.ProcessedOption, ConfigProcessor.ProcessedCategory> categoryForOption = new HashMap<>();
    private final LerpingInteger minimumSearchSize = new LerpingInteger(0, 150);
    private final GuiElementTextField searchField = new GuiElementTextField("", 0, 20, 0);
    private String selectedCategory = null;
    private final List<String> selectedSubcategoryPath = new ArrayList<>();
    private Set<ConfigProcessor.ProcessedCategory> searchedCategories = null;
    private Map<ConfigProcessor.ProcessedCategory, Set<String>> searchedSubcategories = null;
    private Map<ConfigProcessor.ProcessedSubcategory, Set<String>> searchedSubSubcategories = null;
    private Map<ConfigProcessor.ProcessedCategory, Set<Integer>> searchedAccordions = null;
    private Set<ConfigProcessor.ProcessedOption> searchedOptions = null;
    private final HashMap<ConfigProcessor.ProcessedOption, List<String>> optionSubcategoryPath = new HashMap<>();
    private float optionsBarStart;
    private float optionsBarend;
    private int lastMouseX = 0;
    private int keyboardScrollXCutoff = 0;

    private static final long SEARCH_REPEAT_DELAY_MS = 500;
    private static final long SEARCH_REPEAT_RATE_MS  =  50;
    private int  searchRepeatKey   = 0;
    private long searchRepeatStart = 0;
    private long searchRepeatLast  = 0;

    private static final int TREE_INDENT = 14;
    private static final int BASE_INDENT = 6;

    public ConfigEditor(Object config) {
        this(config, null);
    }

    public ConfigEditor(Object config, String categoryOpen) {
        this.openedMillis = System.currentTimeMillis();
        this.processedConfig = ConfigProcessor.create(config);

        for (ConfigProcessor.ProcessedCategory category : processedConfig.values()) {
            for (ConfigProcessor.ProcessedOption option : category.options.values()) {
                categoryForOption.put(option, category);
                optionSubcategoryPath.put(option, Collections.emptyList());
                String combined = category.name + " " + category.desc + " " + option.name + " " + option.desc;
                combined = combined.replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
                for (String word : combined.split("[ _]")) {
                    searchOptionMap.computeIfAbsent(word, k -> new HashSet<>()).add(option);
                }
            }
            indexSubcategoryOptions(category, category.subcategories, new ArrayList<>());
        }

        if (categoryOpen != null) {
            for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                if (category.getValue().name.equalsIgnoreCase(categoryOpen)) {
                    selectedCategory = category.getKey();
                    break;
                }
            }
            if (selectedCategory == null) {
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                    if (category.getValue().name.toLowerCase().startsWith(categoryOpen.toLowerCase())) {
                        selectedCategory = category.getKey();
                        break;
                    }
                }
            }
            if (selectedCategory == null) {
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
                    if (category.getValue().name.toLowerCase().contains(categoryOpen.toLowerCase())) {
                        selectedCategory = category.getKey();
                        break;
                    }
                }
            }
        }

        editor = this;
    }

    private void indexSubcategoryOptions(ConfigProcessor.ProcessedCategory category,
                                         LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subcategories,
                                         List<String> parentPath) {
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : subcategories.entrySet()) {
            ConfigProcessor.ProcessedSubcategory sub = subEntry.getValue();
            List<String> fullPath = new ArrayList<>(parentPath);
            fullPath.add(subEntry.getKey());

            for (ConfigProcessor.ProcessedOption option : sub.options.values()) {
                categoryForOption.put(option, category);
                optionSubcategoryPath.put(option, fullPath);

                StringBuilder combined = new StringBuilder(category.name + " " + sub.name + " " + option.name + " " + option.desc);
                combined.append(" ").append(String.join(" ", fullPath));
                String sc = combined.toString().replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
                for (String sw : sc.split("[ _]")) {
                    searchOptionMap.computeIfAbsent(sw, k -> new HashSet<>()).add(option);
                }
            }

            if (!sub.subcategories.isEmpty()) {
                indexSubcategoryOptions(category, sub.subcategories, fullPath);
            }
        }
    }

    private LinkedHashMap<String, ConfigProcessor.ProcessedCategory> getCurrentConfigEditing() {
        LinkedHashMap<String, ConfigProcessor.ProcessedCategory> newMap = new LinkedHashMap<>(processedConfig);
        if (searchedCategories != null) newMap.values().retainAll(searchedCategories);
        return newMap;
    }

    private LinkedHashMap<String, ConfigProcessor.ProcessedOption> getOptionsInCategory(ConfigProcessor.ProcessedCategory cat) {
        LinkedHashMap<String, ConfigProcessor.ProcessedOption> newMap;
        if (!selectedSubcategoryPath.isEmpty()) {
            ConfigProcessor.ProcessedSubcategory sub = walkSubPath(cat, selectedSubcategoryPath);
            if (sub != null) {
                newMap = new LinkedHashMap<>(sub.options);
            } else {
                newMap = new LinkedHashMap<>(cat.options);
            }
        } else {
            newMap = new LinkedHashMap<>(cat.options);
        }

        if (searchedOptions != null) {
            Set<ConfigProcessor.ProcessedOption> retain = new HashSet<>();
            retain.addAll(searchedOptions);
            if (searchedAccordions != null) {
                Set<Integer> visibleAccordions = searchedAccordions.get(cat);
                if (visibleAccordions != null && !visibleAccordions.isEmpty()) {
                    for (ConfigProcessor.ProcessedOption option : newMap.values()) {
                        if (option.editor instanceof GuiOptionEditorAccordion) {
                            int accordionId = ((GuiOptionEditorAccordion) option.editor).getAccordionId();
                            if (visibleAccordions.contains(accordionId)) {
                                retain.add(option);
                            }
                        }
                    }
                }
            }
            newMap.values().retainAll(retain);
        }
        return newMap;
    }

    private ConfigProcessor.ProcessedSubcategory walkSubPath(ConfigProcessor.ProcessedCategory cat, List<String> path) {
        ConfigProcessor.ProcessedSubcategory current = null;
        LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs = cat.subcategories;
        for (String key : path) {
            if (!subs.containsKey(key)) return null;
            current = subs.get(key);
            subs = current.subcategories;
        }
        return current;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    private void setSelectedCategory(String category) {
        if (!java.util.Objects.equals(this.selectedCategory, category)) {
            selectedSubcategoryPath.clear();
        }
        selectedCategory = category;
        optionsScroll.setValue(0);
        if (category != null) {
            ConfigProcessor.ProcessedCategory cat = processedConfig.get(category);
            if (cat != null) {
                boolean hasDirectMatches = searchedOptions == null
                    || cat.options.values().stream().anyMatch(opt -> searchedOptions.contains(opt));
                if (!hasDirectMatches && selectedSubcategoryPath.isEmpty()) {
                    List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = getVisibleSubcategories(cat.subcategories, cat, 1);
                    if (!visible.isEmpty()) {
                        selectedSubcategoryPath.add(visible.get(0).getKey());
                    }
                }
            }
        }
    }

    private void toggleTreePath(String catKey, List<String> targetPath) {
        if (targetPath.isEmpty()) {
            if (selectedSubcategoryPath.isEmpty() && java.util.Objects.equals(selectedCategory, catKey)) {
                selectedCategory = null;
            } else {
                selectedCategory = catKey;
                selectedSubcategoryPath.clear();
            }
        } else {
            selectedCategory = catKey;
            if (selectedSubcategoryPath.equals(targetPath)) {
                selectedSubcategoryPath.remove(selectedSubcategoryPath.size() - 1);
            } else {
                selectedSubcategoryPath.clear();
                selectedSubcategoryPath.addAll(targetPath);
            }
        }
        optionsScroll.setValue(0);
    }

    public String getSelectedCategoryName() {
        return processedConfig.get(selectedCategory).name;
    }

    public void search() {
        String search = searchField.getText().trim().replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
        searchedCategories = null;
        searchedOptions = null;
        searchedAccordions = null;
        searchedSubcategories = null;
        searchedSubSubcategories = null;

        if (!search.isEmpty()) {
            searchedCategories = new HashSet<>();
            searchedAccordions = new HashMap<>();

            for (String word : search.split(" ")) {
                if (word.trim().isEmpty()) continue;

                Set<ConfigProcessor.ProcessedOption> options = new HashSet<>();
                Map<String, Set<ConfigProcessor.ProcessedOption>> map = StringUtils.subMapWithKeysThatAreSuffixes(word, searchOptionMap);
                map.values().forEach(options::addAll);

                if (!options.isEmpty()) {
                    if (searchedOptions == null) {
                        searchedOptions = new HashSet<>(options);
                    } else {
                        searchedOptions.retainAll(options);
                    }
                }
            }

            if (searchedOptions == null) {
                searchedOptions = new HashSet<>();
            } else {
                searchedSubcategories = new HashMap<>();
                searchedSubSubcategories = new HashMap<>();
                for (ConfigProcessor.ProcessedOption option : searchedOptions) {
                    ConfigProcessor.ProcessedCategory cat = categoryForOption.get(option);
                    if (cat == null) continue;

                    searchedCategories.add(cat);
                    searchedAccordions.computeIfAbsent(cat, k -> new HashSet<>()).add(option.accordionId);

                    List<String> path = optionSubcategoryPath.get(option);
                    if (path != null && !path.isEmpty()) {
                        searchedSubcategories.computeIfAbsent(cat, k -> new HashSet<>()).add(path.get(0));

                        ConfigProcessor.ProcessedSubcategory sub = walkSubPath(cat, path.subList(0, 1));
                        if (sub != null && path.size() >= 2) {
                            searchedSubSubcategories.computeIfAbsent(sub, k -> new HashSet<>()).add(path.get(1));
                        }
                    }
                }
            }
        }
    }

    public void render() {
        optionsScroll.tick();
        categoryScroll.tick();
        handleKeyboardPresses();

        List<String> tooltipToDisplay = null;

        long currentTime = System.currentTimeMillis();
        long delta = currentTime - openedMillis;

        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();
        int mouseX = Mouse.getX() * width / Minecraft.getMinecraft().displayWidth;
        int mouseY = height - Mouse.getY() * height / Minecraft.getMinecraft().displayHeight - 1;

        float opacityFactor = LerpUtils.sigmoidZeroOne(delta / 500f);
        RenderUtils.drawGradientRect(0, 0, 0, width, height, (int) (0x80 * opacityFactor) << 24 | 0x101010, (int) (0x90 * opacityFactor) << 24 | 0x101010);

        int xSize = Math.min(scaledResolution.getScaledWidth() - 100 / scaledResolution.getScaleFactor(), 540);
        int ySize = Math.min(scaledResolution.getScaledHeight() - 100 / scaledResolution.getScaleFactor(), 400);

        int x = (scaledResolution.getScaledWidth() - xSize) / 2;
        int y = (scaledResolution.getScaledHeight() - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int openingXSize = xSize;
        int openingYSize = ySize;
        if (delta < 150) {
            openingXSize = (int) (delta * xSize / 150);
            openingYSize = 5;
        } else if (delta < 300) {
            openingYSize = 5 + (int) (delta - 150) * (ySize - 5) / 150;
        }
        RenderUtils.drawFloatingRectDark((scaledResolution.getScaledWidth() - openingXSize) / 2, (scaledResolution.getScaledHeight() - openingYSize) / 2, openingXSize, openingYSize);
        GlScissorStack.clear();
        GlScissorStack.push((scaledResolution.getScaledWidth() - openingXSize) / 2, (scaledResolution.getScaledHeight() - openingYSize) / 2, (scaledResolution.getScaledWidth() + openingXSize) / 2, (scaledResolution.getScaledHeight() + openingYSize) / 2, scaledResolution);

        RenderUtils.drawFloatingRectDark(x + 5, y + 5, xSize - 10, 20, false);

        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        TextRenderUtils.drawStringCenteredScaledMaxWidth("Aetheria" + " " + Aetheria.VERSION + " by " + EnumChatFormatting.RED + "h4mlock", fr, x + xSize / 2f, y + 15, false, 380, 0xa0a0a0);
        RenderUtils.drawFloatingRectDark(x + 4, y + 49 - 20, 180, ySize - 54 + 20, false);

        int innerPadding = 20 / adjScaleFactor;
        int innerLeft = x + 4 + innerPadding;
        int innerRight = x + 184 - innerPadding;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        Gui.drawRect(innerLeft, innerTop, innerLeft + 1, innerBottom, 0xff080808);
        Gui.drawRect(innerLeft + 1, innerTop, innerRight, innerTop + 1, 0xff080808);
        Gui.drawRect(innerRight - 1, innerTop + 1, innerRight, innerBottom, 0xff282828);
        Gui.drawRect(innerLeft + 1, innerBottom - 1, innerRight - 1, innerBottom, 0xff282828);
        Gui.drawRect(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, 0x60080808);

        GlScissorStack.push(0, innerTop + 1, scaledResolution.getScaledWidth(), innerBottom - 1, scaledResolution);

        float catBarSize = 1;
        int catY = -categoryScroll.getValue();

        LinkedHashMap<String, ConfigProcessor.ProcessedCategory> currentConfigEditing = getCurrentConfigEditing();
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : currentConfigEditing.entrySet()) {
            if (selectedCategory == null || !currentConfigEditing.containsKey(selectedCategory)) {
                setSelectedCategory(entry.getKey());
            }
            String catKey = entry.getKey();
            ConfigProcessor.ProcessedCategory cat = entry.getValue();
            boolean isSelectedRoot = catKey.equals(selectedCategory);

            String catName = isSelectedRoot && selectedSubcategoryPath.isEmpty()
                ? EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.UNDERLINE + cat.name
                : (isSelectedRoot ? EnumChatFormatting.AQUA + cat.name : EnumChatFormatting.GRAY + cat.name);
            fr.drawString(catName, innerLeft + 9 + BASE_INDENT, y + 70 + catY, -1);
            catY += 15;

            if (isSelectedRoot) {
                catY = renderSubTree(cat.subcategories, cat, selectedSubcategoryPath, 1, catY, y, innerLeft, fr);
            }

            if (catY > 0) {
                catBarSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (catY + 5 + categoryScroll.getValue()));
            }
        }

        float catBarStart = categoryScroll.getValue() / (float) (catY + categoryScroll.getValue());
        float catBarEnd = catBarStart + catBarSize;
        if (catBarEnd > 1) {
            catBarEnd = 1;
            if (categoryScroll.getTarget() / (float) (catY + categoryScroll.getValue()) + catBarSize < 1) {
                int target = optionsScroll.getTarget();
                categoryScroll.setValue((int) Math.ceil((catY + 5 + categoryScroll.getValue()) - catBarSize * (catY + 5 + categoryScroll.getValue())));
                categoryScroll.setTarget(target);
            } else {
                categoryScroll.setValue((int) Math.ceil((catY + 5 + categoryScroll.getValue()) - catBarSize * (catY + 5 + categoryScroll.getValue())));
            }
        }
        int catDist = innerBottom - innerTop - 12;
        Gui.drawRect(innerLeft + 2, innerTop + 5, innerLeft + 7, innerBottom - 5, 0xff0d0d0d);
        Gui.drawRect(innerLeft + 3, innerTop + 6 + (int) (catDist * catBarStart), innerLeft + 6, innerTop + 6 + (int) (catDist * catBarEnd), 0xff3a3a3a);

        GlScissorStack.pop(scaledResolution);

        TextRenderUtils.drawStringCenteredScaledMaxWidth("Categories", fr, x + 95, y + 44, false, 120, 0x00b8b8);

        RenderUtils.drawFloatingRectDark(x + 189, y + 29, xSize - 194, ySize - 34, false);

        innerLeft = x + 189 + innerPadding;
        innerRight = x + xSize - 5 - innerPadding;
        innerBottom = y + ySize - 5 - innerPadding;

        Minecraft.getMinecraft().getTextureManager().bindTexture(SEARCH_ICON);
        GlStateManager.color(1, 1, 1, 1);
        RenderUtils.drawTexturedRect(innerRight - 20, innerTop - (20 + innerPadding) / 2 - 9, 18, 18, GL11.GL_NEAREST);

        minimumSearchSize.tick();
        boolean shouldShow = !searchField.getText().trim().isEmpty() || searchField.getFocus();
        if (shouldShow && minimumSearchSize.getTarget() < 30) {
            minimumSearchSize.setTarget(30);
            minimumSearchSize.resetTimer();
        } else if (!shouldShow && minimumSearchSize.getTarget() > 0) {
            minimumSearchSize.setTarget(0);
            minimumSearchSize.resetTimer();
        }

        int rightStuffLen = 20;
        if (minimumSearchSize.getValue() > 1) {
            int strLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(searchField.getText()) + 10;
            if (!shouldShow) strLen = 0;

            int len = Math.max(strLen, minimumSearchSize.getValue());
            searchField.setSize(len, 18);
            searchField.render(innerRight - 25 - len, innerTop - (20 + innerPadding) / 2 - 9);

            rightStuffLen += 5 + len;
        }

        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            TextRenderUtils.drawStringScaledMaxWidth(cat.desc, fr, innerLeft + 5, y + 40, true, innerRight - innerLeft - rightStuffLen - 10, 0xb0b0b0);
        }

        Gui.drawRect(innerLeft, innerTop, innerLeft + 1, innerBottom, 0xff080808);
        Gui.drawRect(innerLeft + 1, innerTop, innerRight, innerTop + 1, 0xff080808);
        Gui.drawRect(innerRight - 1, innerTop + 1, innerRight, innerBottom, 0xff303030);
        Gui.drawRect(innerLeft + 1, innerBottom - 1, innerRight - 1, innerBottom, 0xff303030);
        Gui.drawRect(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, 0x60080808);

        GlScissorStack.push(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, scaledResolution);
        float barSize = 1;
        int optionY = -optionsScroll.getValue();
        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;
            GlStateManager.enableDepth();
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                    int accordionDepth = activeAccordions.get(option.accordionId);
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                int optionHeight = editor.getHeight();
                if (innerTop + 5 + optionY + optionHeight > innerTop + 1 && innerTop + 5 + optionY < innerBottom - 1) {
                    editor.render((innerLeft + innerRight - optionWidth) / 2 - 5, innerTop + 5 + optionY, optionWidth);
                }
                optionY += optionHeight + 5;
            }
            GlStateManager.disableDepth();
            if (optionY > 0) {
                barSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (optionY + 5 + optionsScroll.getValue()));
            }
        }

        GlScissorStack.pop(scaledResolution);

        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            int optionYOverlay = -optionsScroll.getValue();
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;

            GlStateManager.translate(0, 0, 10);
            GlStateManager.enableDepth();
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) {
                        continue;
                    }
                    int accordionDepth = activeAccordions.get(option.accordionId);
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (accordionDepth + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) {
                    continue;
                }
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = 0;
                        if (option.accordionId >= 0) {
                            accordionDepth = activeAccordions.get(option.accordionId) + 1;
                        }
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                int optionHeight = editor.getHeight();
                if (innerTop + 5 + optionYOverlay + optionHeight > innerTop + 1 && innerTop + 5 + optionYOverlay < innerBottom - 1) {
                    editor.renderOverlay((innerLeft + innerRight - optionWidth) / 2 - 5, innerTop + 5 + optionYOverlay, optionWidth);
                }
                optionYOverlay += optionHeight + 5;
            }
            GlStateManager.disableDepth();
            GlStateManager.translate(0, 0, -10);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        optionsBarStart = optionsScroll.getValue() / (float) (optionY + optionsScroll.getValue());
        optionsBarend = optionsBarStart + barSize;
        if (optionsBarend > 1) {
            optionsBarend = 1;
            if (optionsScroll.getTarget() / (float) (optionY + optionsScroll.getValue()) + barSize < 1) {
                int target = optionsScroll.getTarget();
                optionsScroll.setValue((int) Math.ceil((optionY + 5 + optionsScroll.getValue()) - barSize * (optionY + 5 + optionsScroll.getValue())));
                optionsScroll.setTarget(target);
            } else {
                optionsScroll.setValue((int) Math.ceil((optionY + 5 + optionsScroll.getValue()) - barSize * (optionY + 5 + optionsScroll.getValue())));
            }
        }
        int dist = innerBottom - innerTop - 12;
        Gui.drawRect(innerRight - 10, innerTop + 5, innerRight - 5, innerBottom - 5, 0xff0d0d0d);
        Gui.drawRect(innerRight - 9, innerTop + 6 + (int) (dist * optionsBarStart), innerRight - 6, innerTop + 6 + (int) (dist * optionsBarend), 0xff3a3a3a);

        for (int socialIndex = 0; socialIndex < socialsIco.length; socialIndex++) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(socialsIco[socialIndex]);
            GlStateManager.color(1, 1, 1, 1);
            int socialLeft = x + xSize - 23 - 18 * socialIndex;
            RenderUtils.drawTexturedRect(socialLeft, y + 7, 16, 16, GL11.GL_LINEAR);

            if (mouseX >= socialLeft && mouseX <= socialLeft + 16 && mouseY >= y + 6 && mouseY <= y + 23) {
                tooltipToDisplay = Lists.newArrayList(EnumChatFormatting.YELLOW + "Go to: " + EnumChatFormatting.RESET + socialsLink[socialIndex]);
            }
        }

        GlScissorStack.clear();

        if (tooltipToDisplay != null) {
            TextRenderUtils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, fr);
        }

        GlStateManager.translate(0, 0, -2);
    }

    private int renderSubTree(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs,
                              Object parent, List<String> activePath, int depth, int catY,
                              int y, int innerLeft, FontRenderer fr) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = getVisibleSubcategories(subs, parent, depth);
        int lineX = innerLeft + 9 + BASE_INDENT + TREE_INDENT * (depth - 1);
        int textBaseX = innerLeft + 9 + BASE_INDENT + TREE_INDENT * depth;
        int parentHeight = depth == 1 ? 15 : 13;
        int parentCenterY = y + 70 + catY - parentHeight;
        int firstChildCenterY = -1;
        int lastChildCenterY = -1;
        int activeChildCenterY = -1;
        int currentCatY = catY;
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : visible) {
            boolean inPath = activePath.size() >= depth && activePath.get(depth - 1).equals(subEntry.getKey());
            boolean isDeepestSelected = inPath && activePath.size() == depth;
            if (inPath) activeChildCenterY = y + 70 + currentCatY;

            int itemCenterY = y + 70 + currentCatY;
            if (firstChildCenterY == -1) firstChildCenterY = itemCenterY;
            lastChildCenterY = itemCenterY;

            int hColor = inPath ? 0xff00b8b8 : 0xff606060;
            Gui.drawRect(lineX, itemCenterY + 4, textBaseX - 2, itemCenterY + 5, hColor);

            String name = isDeepestSelected
                ? EnumChatFormatting.DARK_AQUA + "" + EnumChatFormatting.UNDERLINE + subEntry.getValue().name
                : (inPath ? EnumChatFormatting.AQUA + subEntry.getValue().name : EnumChatFormatting.DARK_GRAY + subEntry.getValue().name);
            fr.drawString(name, textBaseX, y + 70 + currentCatY, -1);
            currentCatY += 13;

            if (inPath && !subEntry.getValue().subcategories.isEmpty()) {
                currentCatY = renderSubTree(subEntry.getValue().subcategories, subEntry.getValue(), activePath, depth + 1, currentCatY, y, innerLeft, fr);
            }
        }
        if (!visible.isEmpty()) {
            int trunkStart = parentCenterY + parentHeight / 2 + 3;
            if (activeChildCenterY != -1) {
                Gui.drawRect(lineX, trunkStart, lineX + 1, activeChildCenterY + 5, 0xff00b8b8);
                if (activeChildCenterY < lastChildCenterY) {
                    Gui.drawRect(lineX, activeChildCenterY + 5, lineX + 1, lastChildCenterY + 1, 0xff606060);
                }
            } else {
                Gui.drawRect(lineX, trunkStart, lineX + 1, lastChildCenterY + 1, 0xff606060);
            }
        }
        return currentCatY;
    }

    private List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> getVisibleSubcategories(
            LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, Object parent, int depth) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = new ArrayList<>();

        Set<String> matchedSubs = null;
        if (searchedSubcategories != null) {
            if (depth == 1) {
                matchedSubs = searchedSubcategories.get(parent);
            } else if (searchedSubSubcategories != null && parent instanceof ConfigProcessor.ProcessedSubcategory) {
                matchedSubs = searchedSubSubcategories.get(parent);
            }
        }

        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : subs.entrySet()) {
            if (matchedSubs == null || matchedSubs.contains(subEntry.getKey())) {
                visible.add(subEntry);
            }
        }
        return visible;
    }

    public boolean mouseInput(int mouseX, int mouseY) {
        lastMouseX = mouseX;
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();
        int height = scaledResolution.getScaledHeight();

        int xSize = Math.min(width - 100 / scaledResolution.getScaleFactor(), 540);
        int ySize = Math.min(height - 100 / scaledResolution.getScaleFactor(), 400);

        int x = (scaledResolution.getScaledWidth() - xSize) / 2;
        int y = (scaledResolution.getScaledHeight() - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int innerPadding = 20 / adjScaleFactor;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        int innerLeft = x + 189 + innerPadding;
        int innerRight = x + xSize - 5 - innerPadding;

        int dist = innerBottom - innerTop - 12;
        int optionsBarStartY = innerTop + 6 + (int) (dist * optionsBarStart);
        int optionsBarEndY = innerTop + 6 + (int) (dist * optionsBarend);
        int optionsBarStartX = innerRight - 12;
        int optionsBarEndX = innerRight - 3;

        int categoryY = -categoryScroll.getValue();
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> _e : getCurrentConfigEditing().entrySet()) {
            categoryY += 15;
            if (_e.getKey().equals(selectedCategory)) {
                ConfigProcessor.ProcessedCategory cat = processedConfig.get(_e.getKey());
                if (cat != null) {
                    categoryY += treeSubcategoryCount(cat.subcategories, cat, selectedSubcategoryPath, 1) * 13;
                }
            }
        }
        int catDist = innerBottom - innerTop - 12;
        float catBarStart = categoryScroll.getValue() / (float) (categoryY + categoryScroll.getValue());
        float categoryBarSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (categoryY + 5 + categoryScroll.getValue()));
        float catBarEnd = catBarStart + categoryBarSize;
        int categoryBarStartY = innerTop + 6 + (int) (catDist * catBarStart);
        int categoryBarEndY = innerTop + 6 + (int) (catDist * catBarEnd);
        int categoryBarStartX = x + innerPadding + 7;
        int categoryBarEndX = x + innerPadding + 12;
        keyboardScrollXCutoff = innerLeft - 10;
        if (Mouse.getEventButtonState()) {
            if ((mouseY < optionsBarStartY || mouseY > optionsBarEndY) && (mouseX >= optionsBarStartX && mouseX <= optionsBarEndX) && mouseY > innerTop + 6 && mouseY < innerBottom - 6) {
                optionsScroll.setTimeToReachTarget(200);
                optionsScroll.resetTimer();
                optionsScroll.setTarget(mouseY - innerTop);
                return true;
            } else if ((mouseY < categoryBarStartY || mouseY > categoryBarEndY) && (mouseX >= categoryBarStartX && mouseX <= categoryBarEndX) && mouseY > innerTop + 6 && mouseY < innerBottom - 6) {
                categoryScroll.setTimeToReachTarget(200);
                categoryScroll.resetTimer();
                categoryScroll.setTarget(mouseY - innerTop);
                return true;
            }

            searchField.setFocus(mouseX >= innerRight - 20 && mouseX <= innerRight - 2 && mouseY >= innerTop - (20 + innerPadding) / 2 - 9 && mouseY <= innerTop - (20 + innerPadding) / 2 + 9);

            if (minimumSearchSize.getValue() > 1) {
                int strLen = Minecraft.getMinecraft().fontRendererObj.getStringWidth(searchField.getText()) + 10;
                int len = Math.max(strLen, minimumSearchSize.getValue());

                if (mouseX >= innerRight - 25 - len && mouseX <= innerRight - 25 && mouseY >= innerTop - (20 + innerPadding) / 2 - 9 && mouseY <= innerTop - (20 + innerPadding) / 2 + 9) {
                    String old = searchField.getText();
                    searchField.mouseClicked(mouseX, mouseY, Mouse.getEventButton());

                    if (!searchField.getText().equals(old)) search();
                }
            }
        }

        int dWheel = Mouse.getEventDWheel();
        if (mouseY > innerTop && mouseY < innerBottom && dWheel != 0) {
            if (dWheel < 0) dWheel = -1;
            if (dWheel > 0) dWheel = 1;

            if (mouseX < innerLeft) {
                int newTarget = categoryScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) newTarget = 0;

                float catBarSize = 1;
                int catY = -newTarget;
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
                    if (getSelectedCategory() == null) setSelectedCategory(entry.getKey());

                    catY += 15;
                    if (entry.getKey().equals(selectedCategory)) {
                        ConfigProcessor.ProcessedCategory cat = processedConfig.get(entry.getKey());
                        if (cat != null) {
                            catY += treeSubcategoryCount(cat.subcategories, cat, selectedSubcategoryPath, 1) * 13;
                        }
                    }
                    if (catY > 0) {
                        catBarSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (catY + 5 + newTarget));
                    }
                }

                int barMax = (int) Math.floor((catY + 5 + newTarget) - catBarSize * (catY + 5 + newTarget));
                if (newTarget > barMax) newTarget = barMax;
                categoryScroll.resetTimer();
                categoryScroll.setTarget(newTarget);
            } else {
                int newTarget = optionsScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) newTarget = 0;

                float barSize = 1;
                int optionY = -newTarget;
                if (getSelectedCategory() != null && getCurrentConfigEditing() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                    ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                    HashMap<Integer, Integer> activeAccordions = new HashMap<>();
                    for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                        if (option.accordionId >= 0 && !activeAccordions.containsKey(option.accordionId)) continue;

                        GuiOptionEditor editor = option.editor;
                        if (editor == null) continue;
                        if (editor instanceof GuiOptionEditorAccordion) {
                            GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                            if (accordion.getToggled()) {
                                int accordionDepth = option.accordionId >= 0 ? activeAccordions.get(option.accordionId) + 1 : 0;
                                activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                            }
                        }
                        optionY += editor.getHeight() + 5;
                        if (optionY > 0) {
                            barSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (optionY + 5 + newTarget));
                        }
                    }
                }

                int barMax = (int) Math.floor((optionY + 5 + newTarget) - barSize * (optionY + 5 + newTarget));
                if (newTarget > barMax) newTarget = barMax;
                optionsScroll.setTimeToReachTarget(Math.min(150, Math.max(10, 5 * Math.abs(newTarget - optionsScroll.getValue()))));
                optionsScroll.resetTimer();
                optionsScroll.setTarget(newTarget);
            }
        } else if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            if (getCurrentConfigEditing() != null) {
                FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
                int catYBasis = x + 4 + innerPadding + 9 + BASE_INDENT;
                int catY = -categoryScroll.getValue();
                for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
                    if (getSelectedCategory() == null) setSelectedCategory(entry.getKey());

                    String ck = entry.getKey();
                    int textX = catYBasis;
                    int textW = fr.getStringWidth(entry.getValue().name);
                    if (mouseX >= textX - 4 && mouseX <= textX + textW + 4 && mouseY >= y + 70 + catY - 7 && mouseY <= y + 70 + catY + 7) {
                        toggleTreePath(ck, Collections.emptyList());
                        return true;
                    }
                    catY += 15;

                    if (ck.equals(selectedCategory)) {
                        List<String> beforePath = new ArrayList<>(selectedSubcategoryPath);
                        catY = handleSubTreeClick(entry.getValue().subcategories, entry.getValue(), new ArrayList<>(), 1, catY, mouseX, mouseY, x, y, ck, catYBasis);
                        if (!selectedSubcategoryPath.equals(beforePath)) return true;
                    }
                }
            }

            for (int socialIndex = 0; socialIndex < socialsLink.length; socialIndex++) {
                int socialLeft = x + xSize - 23 - 18 * socialIndex;
                if (mouseX >= socialLeft && mouseX <= socialLeft + 16 && mouseY >= y + 6 && mouseY <= y + 23) {
                    try { Desktop.getDesktop().browse(new URI(socialsLink[socialIndex])); } catch (Exception ignored) {}
                    return true;
                }
            }
        }

        int optionY = -optionsScroll.getValue();
        if (getSelectedCategory() != null && getCurrentConfigEditing() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            int optionWidthDefault = innerRight - innerLeft - 20;
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                int optionWidth = optionWidthDefault;
                if (option.accordionId >= 0) {
                    if (!activeAccordions.containsKey(option.accordionId)) continue;
                    optionWidth = optionWidthDefault - (2 * innerPadding) * (activeAccordions.get(option.accordionId) + 1);
                }

                GuiOptionEditor editor = option.editor;
                if (editor == null) continue;
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = option.accordionId >= 0 ? activeAccordions.get(option.accordionId) + 1 : 0;
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                if (editor.mouseInputOverlay((innerLeft + innerRight - optionWidth) / 2 - 5, innerTop + 5 + optionY, optionWidth, mouseX, mouseY)) {
                    return true;
                }
                optionY += editor.getHeight() + 5;
            }
        }

        if (mouseX > innerLeft && mouseX < innerRight && mouseY > innerTop && mouseY < innerBottom) {
            optionY = -optionsScroll.getValue();
            if (getSelectedCategory() != null && getCurrentConfigEditing() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                int optionWidthDefault = innerRight - innerLeft - 20;
                ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                HashMap<Integer, Integer> activeAccordions = new HashMap<>();
                for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                    int optionWidth = optionWidthDefault;
                    if (option.accordionId >= 0) {
                        if (!activeAccordions.containsKey(option.accordionId)) continue;
                        optionWidth = optionWidthDefault - (2 * innerPadding) * (activeAccordions.get(option.accordionId) + 1);
                    }

                    GuiOptionEditor editor = option.editor;
                    if (editor == null) continue;
                    if (editor instanceof GuiOptionEditorAccordion) {
                        GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                        if (accordion.getToggled()) {
                            int accordionDepth = option.accordionId >= 0 ? activeAccordions.get(option.accordionId) + 1 : 0;
                            activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                        }
                    }
                    if (editor.mouseInput((innerLeft + innerRight - optionWidth) / 2 - 5, innerTop + 5 + optionY, optionWidth, mouseX, mouseY)) {
                        return true;
                    }
                    optionY += editor.getHeight() + 5;
                }
            }
        }

        return true;
    }

    private int handleSubTreeClick(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs,
                                   Object parent, List<String> parentPath, int depth, int catY,
                                   int mouseX, int mouseY, int x, int y, String catKey, int catYBasis) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = getVisibleSubcategories(subs, parent, depth);
        FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : visible) {
            int textX = catYBasis + TREE_INDENT * depth;
            int textW = fr.getStringWidth(subEntry.getValue().name);
            if (mouseX >= textX - 4 && mouseX <= textX + textW + 4 && mouseY >= y + 70 + catY - 8 && mouseY <= y + 70 + catY + 8) {
                List<String> targetPath = new ArrayList<>(parentPath);
                targetPath.add(subEntry.getKey());
                toggleTreePath(catKey, targetPath);
                catY += 13;
                return catY;
            }
            catY += 13;

            if (selectedSubcategoryPath.size() >= depth && selectedSubcategoryPath.get(depth - 1).equals(subEntry.getKey()) && !subEntry.getValue().subcategories.isEmpty()) {
                List<String> childPath = new ArrayList<>(parentPath);
                childPath.add(subEntry.getKey());
                catY = handleSubTreeClick(subEntry.getValue().subcategories, subEntry.getValue(), childPath, depth + 1, catY, mouseX, mouseY, x, y, catKey, catYBasis);
            }
        }
        return catY;
    }

    private int treeSubcategoryCount(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs,
                                     Object parent, List<String> activePath, int depth) {
        int count = 0;
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : getVisibleSubcategories(subs, parent, depth)) {
            count++;
            if (activePath.size() >= depth && activePath.get(depth - 1).equals(subEntry.getKey()) && !subEntry.getValue().subcategories.isEmpty()) {
                count += treeSubcategoryCount(subEntry.getValue().subcategories, subEntry.getValue(), activePath, depth + 1);
            }
        }
        return count;
    }

    public boolean keyboardInput() {
        ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
        int width = scaledResolution.getScaledWidth();

        int xSize = Math.min(width - 100 / scaledResolution.getScaleFactor(), 540);

        int adjScaleFactor = Math.max(2, scaledResolution.getScaleFactor());

        int innerPadding = 20 / adjScaleFactor;
        int innerWidth = xSize - 194 - innerPadding * 2;

        if (Keyboard.getEventKeyState()) {
            String old = searchField.getText();
            searchField.keyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());
            searchField.setText(Minecraft.getMinecraft().fontRendererObj.trimStringToWidth(searchField.getText(), innerWidth / 2 - 20));

            if (!searchField.getText().equals(old)) search();
        }

        if (getSelectedCategory() != null && getCurrentConfigEditing() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            HashMap<Integer, Integer> activeAccordions = new HashMap<>();
            for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
                if (option.accordionId >= 0 && !activeAccordions.containsKey(option.accordionId)) continue;

                GuiOptionEditor editor = option.editor;
                if (editor == null) continue;
                if (editor instanceof GuiOptionEditorAccordion) {
                    GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) editor;
                    if (accordion.getToggled()) {
                        int accordionDepth = option.accordionId >= 0 ? activeAccordions.get(option.accordionId) + 1 : 0;
                        activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                    }
                }
                if (editor.keyboardInput()) return true;
            }
        }

        return true;
    }

    private void handleKeyboardPresses() {
        LerpingInteger target = lastMouseX < keyboardScrollXCutoff ? categoryScroll : optionsScroll;
        if (Keyboard.isKeyDown(Keyboard.KEY_DOWN)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            target.setTarget(target.getTarget() + 5);
        } else if (Keyboard.isKeyDown(Keyboard.KEY_UP)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            if (target.getTarget() >= 0) target.setTarget(target.getTarget() - 5);
        }

        if (searchField.getFocus()) {
            int heldKey = 0;
            if      (Keyboard.isKeyDown(Keyboard.KEY_BACK))   heldKey = Keyboard.KEY_BACK;
            else if (Keyboard.isKeyDown(Keyboard.KEY_DELETE))  heldKey = Keyboard.KEY_DELETE;

            if (heldKey != 0) {
                long now = System.currentTimeMillis();
                if (searchRepeatKey != heldKey) {
                    searchRepeatKey   = heldKey;
                    searchRepeatStart = now;
                    searchRepeatLast  = now;
                } else if (now - searchRepeatStart >= SEARCH_REPEAT_DELAY_MS && now - searchRepeatLast >= SEARCH_REPEAT_RATE_MS) {
                    searchRepeatLast = now;
                    String old = searchField.getText();
                    searchField.keyTyped((char) 0, heldKey);
                    if (!searchField.getText().equals(old)) search();
                }
            } else {
                searchRepeatKey = 0;
            }
        }
    }
}
