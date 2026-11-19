// SPDX-License-Identifier: LGPL-3.0-only
// Derived from MoulConfig (https://github.com/NotEnoughUpdates/MoulConfig)

package io.hamlook.aetheria.core.moulconfig.gui.config;

import com.google.common.collect.Lists;
import io.hamlook.aetheria.Aetheria;
import io.hamlook.aetheria.ThemeContext;
import io.hamlook.aetheria.core.moulconfig.editors.GuiOptionEditor;
import io.hamlook.aetheria.core.moulconfig.editors.GuiOptionEditorAccordion;
import io.hamlook.aetheria.core.moulconfig.gui.GlScissorStack;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElement;
import io.hamlook.aetheria.core.moulconfig.gui.GuiElementTextField;
import io.hamlook.aetheria.repo.RepoLinks;
import io.hamlook.aetheria.utils.KeybindHelper;
import io.hamlook.aetheria.utils.LerpUtils;
import io.hamlook.aetheria.utils.LerpUtils.LerpingInteger;
import io.hamlook.aetheria.utils.StringUtils;
import io.hamlook.aetheria.utils.UrlOpener;
import io.hamlook.aetheria.utils.compat.*;
import io.hamlook.aetheria.utils.render.RenderUtils;
import io.hamlook.aetheria.utils.render.TextRenderUtils;
import lombok.Getter;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

import static io.hamlook.aetheria.Resources.*;

public class ConfigEditor extends GuiElement {
    private static final int MAX_WIDTH = 620;
    private static final int SIDEBAR_WIDTH = 170;
    private static final int RIGHT_PANEL_OFFSET = SIDEBAR_WIDTH + 9;
    private static final int RIGHT_PANEL_GAP = 5;
    private static final ResourceLocation[] socialsIco = new ResourceLocation[]{DISCORD, GITHUB, MODRINTH, SKYATLAS};

    private static String socialUrl(int index) {
        switch (index) {
            case 0: return RepoLinks.discord();
            case 1: return RepoLinks.github();
            case 2: return RepoLinks.modrinth();
            default: return RepoLinks.skyatlas();
        }
    }
    private static final long SEARCH_REPEAT_DELAY_MS = 500;
    private static final long SEARCH_REPEAT_RATE_MS = 50;
    private static final int TREE_INDENT = 14;
    private static final int BASE_INDENT = 6;
    private long openedMillis;
    private final LerpingInteger optionsScroll = new LerpingInteger(0, 150);
    private final LerpingInteger categoryScroll = new LerpingInteger(0, 150);
    private final LinkedHashMap<String, ConfigProcessor.ProcessedCategory> processedConfig;
    private final TreeMap<String, Set<ConfigProcessor.ProcessedOption>> searchOptionMap = new TreeMap<>();
    private final HashMap<ConfigProcessor.ProcessedOption, ConfigProcessor.ProcessedCategory> categoryForOption = new HashMap<>();
    private final LerpingInteger minimumSearchSize = new LerpingInteger(0, 150);
    private final GuiElementTextField searchField = new GuiElementTextField("", 0, 20, 0);
    private final List<String> selectedSubcategoryPath = new ArrayList<>();
    private final List<String> anchorSubcategoryPath = new ArrayList<>();
    private final Map<String, Set<List<String>>> expandedSubcategoryPaths = new HashMap<>();
    private final Set<String> categoriesWithTreeOpen = new HashSet<>();
    private final HashMap<ConfigProcessor.ProcessedOption, List<String>> optionSubcategoryPath = new HashMap<>();
    @Getter
    private String selectedCategory = null;
    private Set<ConfigProcessor.ProcessedCategory> searchedCategories = null;
    private Map<ConfigProcessor.ProcessedCategory, Set<String>> searchedSubcategories = null;
    private Map<ConfigProcessor.ProcessedSubcategory, Set<String>> searchedSubSubcategories = null;
    private Map<ConfigProcessor.ProcessedCategory, Set<Integer>> searchedAccordions = null;
    private Set<ConfigProcessor.ProcessedOption> searchedOptions = null;
    private float optionsBarStart;
    private float optionsBarend;
    private int lastMouseX = 0;
    private int keyboardScrollXCutoff = 0;
    private float categoryBarSize;
    private boolean isDraggingOptionsScroll = false;
    private boolean isDraggingCategoryScroll = false;
    private int dragScrollStartValue = 0;
    private int dragMouseStartY = 0;
    private int searchRepeatKey = 0;
    private long searchRepeatStart = 0;
    private long searchRepeatLast = 0;

    public ConfigEditor(Object config) {
        openedMillis = System.currentTimeMillis();
        processedConfig = ConfigProcessor.create(config);

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
    }

    private String resolveCategoryKey(String categoryOpen) {
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
            if (category.getValue().name.equalsIgnoreCase(categoryOpen)) return category.getKey();
        }
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
            if (category.getValue().name.toLowerCase().startsWith(categoryOpen.toLowerCase())) return category.getKey();
        }
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> category : processedConfig.entrySet()) {
            if (category.getValue().name.toLowerCase().contains(categoryOpen.toLowerCase())) return category.getKey();
        }
        return null;
    }

    private void clearSearch() {
        searchField.setFocus(false);
        searchField.setText("");
        search();
    }

    public void resetForOpen() {
        openedMillis = System.currentTimeMillis();
        clearSearch();
        resetTransientState();
    }

    public void focusSearch(String query) {
        searchField.setText(query);
        search();
        searchField.setFocus(true);
    }

    public void focusCategory(String categoryOpen) {
        String key = resolveCategoryKey(categoryOpen);
        if (key == null) return;
        selectedCategory = key;
        anchorSubcategoryPath.clear();
        selectedSubcategoryPath.clear();
        optionsScroll.setValue(0);
    }

    private void resetTransientState() {
        isDraggingOptionsScroll = false;
        isDraggingCategoryScroll = false;
        dragScrollStartValue = 0;
        dragMouseStartY = 0;
        lastMouseButton = -1;
        lastMousePressed = false;
        lastMouseX = 0;
        lastKeyChar = 0;
        lastKeyCode = 0;
        lastKeyPressed = false;
        searchRepeatKey = 0;
        resetOptionTransientState(processedConfig);
    }

    private void resetOptionTransientState(Map<String, ConfigProcessor.ProcessedCategory> categories) {
        for (ConfigProcessor.ProcessedCategory category : categories.values()) {
            resetOptionsTransientState(category.options);
            resetSubcategoriesTransientState(category.subcategories);
        }
    }

    private void resetSubcategoriesTransientState(Map<String, ConfigProcessor.ProcessedSubcategory> subcategories) {
        for (ConfigProcessor.ProcessedSubcategory sub : subcategories.values()) {
            resetOptionsTransientState(sub.options);
            resetSubcategoriesTransientState(sub.subcategories);
        }
    }

    private void resetOptionsTransientState(Map<String, ConfigProcessor.ProcessedOption> options) {
        for (ConfigProcessor.ProcessedOption option : options.values()) {
            if (option.editor != null) option.editor.resetTransientState();
        }
    }

    /**
     * Selects a specific nested {@code @Category} (identified by its field
     * name, e.g. "playerButtons") within a top-level category (identified by
     * its display name, e.g. "Chat Utils"), expanding the tree to reveal it.
     */
    public void openSubcategory(String categoryName, String... subcategoryFieldPath) {
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : processedConfig.entrySet()) {
            if (entry.getValue().name.equalsIgnoreCase(categoryName)) {
                selectSubcategory(entry.getKey(), Arrays.asList(subcategoryFieldPath));
                return;
            }
        }
    }

    private void indexSubcategoryOptions(ConfigProcessor.ProcessedCategory category, LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subcategories, List<String> parentPath) {
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : subcategories.entrySet()) {
            ConfigProcessor.ProcessedSubcategory sub = subEntry.getValue();
            List<String> fullPath = new ArrayList<>(parentPath);
            fullPath.add(subEntry.getKey());

            for (ConfigProcessor.ProcessedOption option : sub.options.values()) {
                categoryForOption.put(option, category);
                optionSubcategoryPath.put(option, fullPath);

                String combined = category.name + " " + sub.name + " " + option.name + " " + option.desc + " " + String.join(" ", fullPath);
                String sc = combined.replaceAll("[^a-zA-Z_ ]", "").toLowerCase();
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
            Set<ConfigProcessor.ProcessedOption> retain = new HashSet<>(searchedOptions);
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

    private static final class VisibleOption {
        private final ConfigProcessor.ProcessedOption option;
        private final int accordionDepth;

        private VisibleOption(ConfigProcessor.ProcessedOption option, int accordionDepth) {
            this.option = option;
            this.accordionDepth = accordionDepth;
        }
    }

    private List<VisibleOption> visibleOptions(ConfigProcessor.ProcessedCategory cat) {
        List<VisibleOption> visible = new ArrayList<>();
        HashMap<Integer, Integer> activeAccordions = new HashMap<>();
        for (ConfigProcessor.ProcessedOption option : getOptionsInCategory(cat).values()) {
            if (option.accordionId >= 0 && !activeAccordions.containsKey(option.accordionId)) continue;

            int accordionDepth = option.accordionId >= 0 ? activeAccordions.get(option.accordionId) + 1 : 0;
            visible.add(new VisibleOption(option, accordionDepth));

            if (option.editor instanceof GuiOptionEditorAccordion) {
                GuiOptionEditorAccordion accordion = (GuiOptionEditorAccordion) option.editor;
                if (accordion.getToggled() || searchedOptions != null) {
                    activeAccordions.put(accordion.getAccordionId(), accordionDepth);
                }
            }
        }
        return visible;
    }

    private int forEachOption(ConfigProcessor.ProcessedCategory cat, int startY, int innerLeft, int innerRight, int innerTop, int innerBottom, int innerPadding, int optionWidthDefault, OptionCallback callback) {
        int y = startY;
        for (VisibleOption visible : visibleOptions(cat)) {
            ConfigProcessor.ProcessedOption option = visible.option;
            int optionWidth = option.accordionId >= 0
                    ? optionWidthDefault - (2 * innerPadding) * visible.accordionDepth
                    : optionWidthDefault;
            GuiOptionEditor editor = option.editor;
            if (editor == null) continue;

            int optionHeight = editor.getHeight();
            if (innerTop + 5 + y + optionHeight > innerTop + 1 && innerTop + 5 + y < innerBottom - 1) {
                int x = (innerLeft + innerRight - optionWidth) / 2 - 5;
                if (callback.apply(editor, x, innerTop + 5 + y, optionWidth)) return y;
            }
            y += optionHeight + 5;
        }
        return y;
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

    private String breadcrumb(ConfigProcessor.ProcessedCategory cat) {
        StringBuilder crumb = new StringBuilder(EnumChatFormatting.WHITE + cat.name);
        LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs = cat.subcategories;
        ConfigProcessor.ProcessedSubcategory current;
        for (String key : selectedSubcategoryPath) {
            current = subs.get(key);
            if (current == null) break;
            crumb.append(EnumChatFormatting.DARK_GRAY).append(" › ").append(EnumChatFormatting.WHITE).append(current.name);
            subs = current.subcategories;
        }
        return crumb.toString();
    }

    private void setSelectedCategory(String category) {
        selectedCategory = category;
        anchorSubcategoryPath.clear();
        selectedSubcategoryPath.clear();
        optionsScroll.setValue(0);
        selectedSubcategoryPath.addAll(resolveVisiblePath(category, anchorSubcategoryPath));
    }

    private void clickRootCategory(String catKey) {
        if (catKey.equals(selectedCategory) && categoriesWithTreeOpen.contains(catKey)) {
            categoriesWithTreeOpen.remove(catKey);
            getExpandedPaths(catKey).clear();
            anchorSubcategoryPath.clear();
            selectedSubcategoryPath.clear();
        } else {
            selectedCategory = catKey;
            anchorSubcategoryPath.clear();
            categoriesWithTreeOpen.add(catKey);
            selectedSubcategoryPath.clear();
            selectedSubcategoryPath.addAll(resolveVisiblePath(catKey, anchorSubcategoryPath));
        }
        optionsScroll.setValue(0);
    }

    private void clickSubcategory(String catKey, List<String> path) {
        selectedCategory = catKey;
        if (path.equals(anchorSubcategoryPath)) {
            if (isPathExpanded(getExpandedPaths(catKey), parentPathOf(path), lastOf(path))) {
                collapsePath(catKey, path);
                selectedSubcategoryPath.clear();
                selectedSubcategoryPath.addAll(path);
            } else {
                expandPath(catKey, path);
                selectedSubcategoryPath.clear();
                selectedSubcategoryPath.addAll(resolveVisiblePath(catKey, path));
            }
        } else if (isAncestorOf(path, anchorSubcategoryPath)) {
            anchorSubcategoryPath.clear();
            anchorSubcategoryPath.addAll(path);
            collapsePath(catKey, path);
            selectedSubcategoryPath.clear();
            selectedSubcategoryPath.addAll(path);
        } else {
            anchorSubcategoryPath.clear();
            anchorSubcategoryPath.addAll(path);
            expandPath(catKey, path);
            selectedSubcategoryPath.clear();
            selectedSubcategoryPath.addAll(resolveVisiblePath(catKey, path));
        }
        optionsScroll.setValue(0);
    }

    private void selectSubcategory(String catKey, List<String> path) {
        selectedCategory = catKey;
        categoriesWithTreeOpen.add(catKey);
        anchorSubcategoryPath.clear();
        anchorSubcategoryPath.addAll(path);
        expandPath(catKey, path);
        selectedSubcategoryPath.clear();
        selectedSubcategoryPath.addAll(resolveVisiblePath(catKey, path));
        optionsScroll.setValue(0);
    }

    private List<String> resolveVisiblePath(String catKey, List<String> path) {
        ConfigProcessor.ProcessedCategory cat = processedConfig.get(catKey);
        if (cat == null) return new ArrayList<>(path);
        if (path.isEmpty()) {
            if (isRelevant(cat.options)) return new ArrayList<>();
            return descendInto(catKey, cat.subcategories, new ArrayList<>());
        }
        ConfigProcessor.ProcessedSubcategory node = walkSubPath(cat, path);
        if (node == null) return new ArrayList<>(path);
        if (isRelevant(node.options)) return new ArrayList<>(path);
        return descendInto(catKey, node.subcategories, new ArrayList<>(path));
    }

    private List<String> descendInto(String catKey, LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, List<String> basePath) {
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> entry : subs.entrySet()) {
            List<String> childPath = new ArrayList<>(basePath);
            childPath.add(entry.getKey());
            ConfigProcessor.ProcessedSubcategory child = entry.getValue();
            if (isRelevant(child.options)) {
                expandPath(catKey, childPath);
                return childPath;
            }
            List<String> deeper = descendInto(catKey, child.subcategories, childPath);
            if (!deeper.isEmpty()) {
                expandPath(catKey, deeper);
                return deeper;
            }
        }
        return new ArrayList<>();
    }

    private boolean isRelevant(LinkedHashMap<String, ConfigProcessor.ProcessedOption> options) {
        if (searchedOptions == null) return !options.isEmpty();
        for (ConfigProcessor.ProcessedOption option : options.values()) {
            if (searchedOptions.contains(option)) return true;
        }
        return false;
    }

    private static List<String> parentPathOf(List<String> path) {
        return path.subList(0, Math.max(0, path.size() - 1));
    }

    private static String lastOf(List<String> path) {
        return path.isEmpty() ? null : path.get(path.size() - 1);
    }

    private static boolean isAncestorOf(List<String> candidate, List<String> path) {
        return candidate.size() < path.size() && path.subList(0, candidate.size()).equals(candidate);
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
            searchedOptions = matchOptions(search);

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

        if (selectedCategory != null) {
            selectedSubcategoryPath.clear();
            selectedSubcategoryPath.addAll(resolveVisiblePath(selectedCategory, anchorSubcategoryPath));
        }
    }

    private Set<ConfigProcessor.ProcessedOption> matchOptions(String search) {
        Set<ConfigProcessor.ProcessedOption> matches = null;
        for (String word : search.split(" ")) {
            if (word.trim().isEmpty()) continue;

            Set<ConfigProcessor.ProcessedOption> wordMatches = new HashSet<>();
            StringUtils.subMapWithKeysThatAreSuffixes(word, searchOptionMap).values().forEach(wordMatches::addAll);
            if (wordMatches.isEmpty()) return new HashSet<>();

            if (matches == null) {
                matches = wordMatches;
            } else {
                matches.retainAll(wordMatches);
            }
        }
        return matches == null ? new HashSet<>() : matches;
    }

    public void render() {
        optionsScroll.tick();
        categoryScroll.tick();
        handleKeyboardPresses();

        List<String> tooltipToDisplay = null;

        long currentTime = System.currentTimeMillis();
        long delta = currentTime - openedMillis;

        int screenW = GuiScreenUtils.getScaledWindowWidth();
        int screenH = GuiScreenUtils.getScaledWindowHeight();
        int scaleFactor = GuiScreenUtils.getScaleFactor();
        int width = screenW;
        int height = screenH;
        int[] mouse = KeybindHelper.getMouseCoords(width, height);
        int mouseX = mouse[0], mouseY = mouse[1];

        handleDragScroll(mouseX, mouseY);

        float opacityFactor = LerpUtils.sigmoidZeroOne(delta / 500f);
        RenderUtils.drawGradientRect(0, 0, 0, width, height, ThemeContext.withAlpha(ThemeContext.DIM, opacityFactor), ThemeContext.withAlpha(ThemeContext.DIM, opacityFactor));

        int xSize = Math.min(screenW - 100 / scaleFactor, MAX_WIDTH);
        int ySize = Math.min(screenH - 100 / scaleFactor, 400);

        int x = (screenW - xSize) / 2;
        int y = (screenH - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaleFactor);

        int openingXSize = xSize;
        int openingYSize = ySize;
        if (delta < 150) {
            openingXSize = (int) (delta * xSize / 150);
            openingYSize = 5;
        } else if (delta < 300) {
            openingYSize = 5 + (int) (delta - 150) * (ySize - 5) / 150;
        }
        ThemeContext.panel((screenW - openingXSize) / 2, (screenH - openingYSize) / 2, openingXSize, openingYSize, opacityFactor);
        GlScissorStack.clear();
        GlScissorStack.push((screenW - openingXSize) / 2, (screenH - openingYSize) / 2, (screenW + openingXSize) / 2, (screenH + openingYSize) / 2, scaleFactor);

        ThemeContext.rect(x + 1, y + 1, xSize - 2, 2, ThemeContext.ACCENT);
        ThemeContext.panel(x + 5, y + 5, xSize - 10, 20);

        FontRenderer fr = MinecraftCompat.getFontRenderer();
        TextRenderUtils.drawStringCenteredScaledMaxWidth("Aetheria" + " " + Aetheria.VERSION + " by " + EnumChatFormatting.RED + "h4mlock", fr, x + xSize / 2f, y + 15, false, 380, ThemeContext.MUTED);
        ThemeContext.panel(x + 4, y + 29, SIDEBAR_WIDTH, ySize - 34);

        int innerPadding = 20 / adjScaleFactor;
        int innerLeft = x + 4 + innerPadding;
        int innerRight = x + 4 + SIDEBAR_WIDTH - innerPadding;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        ThemeContext.inset(innerLeft, innerTop, innerRight - innerLeft, innerBottom - innerTop);

        GlScissorStack.push(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, scaleFactor);

        int catY = -categoryScroll.getValue();

        LinkedHashMap<String, ConfigProcessor.ProcessedCategory> currentConfigEditing = getCurrentConfigEditing();
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : currentConfigEditing.entrySet()) {
            if (selectedCategory == null || !currentConfigEditing.containsKey(selectedCategory)) {
                setSelectedCategory(entry.getKey());
            }
            String catKey = entry.getKey();
            ConfigProcessor.ProcessedCategory cat = entry.getValue();
            boolean isSelectedRoot = catKey.equals(selectedCategory);

            boolean isViewingCategoryDirectly = selectedSubcategoryPath.isEmpty();
            int categoryColor = isSelectedRoot ? ThemeContext.ACCENT : ThemeContext.MUTED;
            boolean hasChildren = !cat.subcategories.isEmpty();
            boolean catOpen = categoriesWithTreeOpen.contains(catKey);
            String indicator = hasChildren ? (catOpen ? "▼ " : "▶ ") : "  ";
            int rowY = y + 70 + catY;
            if (isSelectedRoot) {
                ThemeContext.rect(innerLeft + 8, rowY - 3, innerRight - innerLeft - 18, 14,
                        ThemeContext.withAlpha(ThemeContext.ACCENT, isViewingCategoryDirectly ? 0.24f : 0.12f));
                ThemeContext.rect(innerLeft + 8, rowY - 3, 2, 14, ThemeContext.ACCENT);
            }
            ThemeContext.text(fr, indicator + cat.name, innerLeft + 9 + BASE_INDENT, rowY, categoryColor);
            catY += 15;

            Set<List<String>> paths = getExpandedPaths(catKey);
            if (categoriesWithTreeOpen.contains(catKey)) {
                boolean categoryHasSelection = !selectedSubcategoryPath.isEmpty() && isSelectedRoot;
                catY = renderSubTree(cat.subcategories, cat, paths, 1, catY, y, innerLeft, fr, new ArrayList<>(), categoryHasSelection);
            }
        }
        catY += SIDEBAR_CONTENT_PADDING;

        float categoryTotal = catY + categoryScroll.getValue();
        float catBarSize = categoryTotal > 0
                ? LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / categoryTotal)
                : 1;
        float catBarStart = categoryTotal > 0 ? categoryScroll.getValue() / categoryTotal : 0f;
        float catBarEnd = catBarStart + catBarSize;
        if (catBarEnd > 1) {
            categoryScroll.setValue((int) Math.ceil(categoryTotal - catBarSize * categoryTotal));
        }
        int catDist = innerBottom - innerTop - 12;
        DrawContextUtils.fill(innerLeft + 2, innerTop + 5, innerLeft + 7, innerBottom - 5, ThemeContext.INSET);
        DrawContextUtils.fill(innerLeft + 3, innerTop + 6 + (int) (catDist * catBarStart), innerLeft + 6, innerTop + 6 + (int) (catDist * catBarEnd), ThemeContext.ACCENT);

        GlScissorStack.pop(scaleFactor);

        TextRenderUtils.drawStringCenteredScaledMaxWidth("Categories", fr, x + 4 + SIDEBAR_WIDTH / 2f, y + 44, false, 120, ThemeContext.ACCENT);

        ThemeContext.panel(x + RIGHT_PANEL_OFFSET, y + 29, xSize - RIGHT_PANEL_OFFSET - RIGHT_PANEL_GAP, ySize - 34);

        innerLeft = x + RIGHT_PANEL_OFFSET + innerPadding;
        innerRight = x + xSize - 5 - innerPadding;
        innerBottom = y + ySize - 5 - innerPadding;

        float searchFieldCenterY = innerTop - (20 + innerPadding) / 2f;

        GlStateManagerCompat.color(1, 1, 1, 1);
        TextureCompat.drawBoundTextured(SEARCH_ICON, innerRight - 20, searchFieldCenterY - 9, 18, 18, 0, 0, 1, 1);

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
            int strLen = MinecraftCompat.getFontRenderer().getStringWidth(searchField.getText()) + 10;
            if (!shouldShow) strLen = 0;

            int len = Math.max(strLen, minimumSearchSize.getValue());
            searchField.setSize(len, 18);
            searchField.render(innerRight - 25 - len, (int) (searchFieldCenterY - 9));

            rightStuffLen += 5 + len;
        }

        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            TextRenderUtils.drawStringScaledMaxWidth(breadcrumb(cat), fr, innerLeft + 5, y + 32, false, innerRight - innerLeft - 20, ThemeContext.TEXT);
            TextRenderUtils.drawStringScaledMaxWidth(cat.desc, fr, innerLeft + 5, y + 43, true, innerRight - innerLeft - rightStuffLen - 10, ThemeContext.MUTED);
        }

        ThemeContext.inset(innerLeft, innerTop, innerRight - innerLeft, innerBottom - innerTop);

        GlScissorStack.push(innerLeft + 1, innerTop + 1, innerRight - 1, innerBottom - 1, scaleFactor);
        float barSize = 1;

        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;
            GlStateManagerCompat.enableDepth();
            forEachOption(cat, -optionsScroll.getValue(), innerLeft, innerRight, innerTop, innerBottom, innerPadding, optionWidthDefault, (ed, edX, edY, edW) -> {
                ed.render(edX, edY, edW);
                return false;
            });
            GlStateManagerCompat.disableDepth();
            int contentHeight = optionContentHeight();
            if (contentHeight > 0) {
                barSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (contentHeight + 5));
            }
        }

        if (searchedOptions != null && searchedOptions.isEmpty()) {
            TextRenderUtils.drawStringCenteredScaledMaxWidth("No options match \"" + searchField.getText().trim() + "\"",
                    fr, (innerLeft + innerRight) / 2f, innerTop + 20, false, innerRight - innerLeft - 20, ThemeContext.MUTED);
        } else if (searchedOptions == null
                && getSelectedCategory() != null
                && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            if (visibleOptions(cat).isEmpty()) {
                TextRenderUtils.drawStringCenteredScaledMaxWidth("No options here - open a category on the left",
                        fr, (innerLeft + innerRight) / 2f, innerTop + 20, false, innerRight - innerLeft - 20, ThemeContext.MUTED);
            }
        }

        GlScissorStack.pop(scaleFactor);

        float optionsTotal = optionContentHeight();
        optionsBarStart = optionsTotal > 0 ? optionsScroll.getValue() / optionsTotal : 0f;
        optionsBarend = optionsBarStart + barSize;
        if (optionsBarend > 1) {
            optionsBarend = 1;
            if (optionsTotal > 0 && optionsScroll.getTarget() / optionsTotal + barSize < 1) {
                int target = optionsScroll.getTarget();
                optionsScroll.setValue((int) Math.ceil(optionsTotal - barSize * optionsTotal));
                optionsScroll.setTarget(target);
            } else {
                optionsScroll.setValue((int) Math.ceil(optionsTotal - barSize * optionsTotal));
            }
        }
        int dist = innerBottom - innerTop - 12;
        DrawContextUtils.fill(innerRight - 10, innerTop + 5, innerRight - 5, innerBottom - 5, ThemeContext.INSET);
        DrawContextUtils.fill(innerRight - 9, innerTop + 6 + (int) (dist * optionsBarStart), innerRight - 6, innerTop + 6 + (int) (dist * optionsBarend), ThemeContext.ACCENT);

        // Draw popovers after scrollbars so color pickers and dropdowns own the top render layer.
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        if (getSelectedCategory() != null && currentConfigEditing.containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = currentConfigEditing.get(getSelectedCategory());
            int optionWidthDefault = innerRight - innerLeft - 20;

            GlStateManagerCompat.translate(0, 0, 10);
            GlStateManagerCompat.enableDepth();
            forEachOption(cat, -optionsScroll.getValue(), innerLeft, innerRight, innerTop, innerBottom, innerPadding, optionWidthDefault, (ed, edX, edY, edW) -> {
                ed.renderOverlay(edX, edY, edW);
                return false;
            });
            GlStateManagerCompat.disableDepth();
            GlStateManagerCompat.translate(0, 0, -10);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        for (int socialIndex = 0; socialIndex < socialsIco.length; socialIndex++) {
            GlStateManagerCompat.color(1, 1, 1, 1);
            int socialLeft = x + xSize - 23 - 18 * socialIndex;
            TextureCompat.drawBoundTextured(socialsIco[socialIndex], socialLeft, y + 7, 16, 16, 0, 0, 1, 1);

            if (mouseX >= socialLeft && mouseX <= socialLeft + 16 && mouseY >= y + 6 && mouseY <= y + 23) {
                tooltipToDisplay = Lists.newArrayList(EnumChatFormatting.YELLOW + "Go to: " + EnumChatFormatting.RESET + socialUrl(socialIndex));
            }
        }

        GlScissorStack.clear();

        if (tooltipToDisplay != null) {
            TextRenderUtils.drawHoveringText(tooltipToDisplay, mouseX, mouseY, width, height, -1, fr);
        }

        GlStateManagerCompat.translate(0, 0, -2);
    }

    private int renderSubTree(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, Object parent, Set<List<String>> expandedPaths, int depth, int catY, int y, int innerLeft, FontRenderer fr, List<String> parentPath, boolean parentInSelection) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = getVisibleSubcategories(subs, parent, depth);
        int lineX = innerLeft + 9 + BASE_INDENT + TREE_INDENT * (depth - 1);
        int textBaseX = innerLeft + 9 + BASE_INDENT + TREE_INDENT * depth;
        int parentHeight = depth == 1 ? 15 : 13;
        int parentCenterY = y + 70 + catY - parentHeight;
        int currentCatY = catY;
        int lastItemY = currentCatY;

        boolean subtreeContainsSelection = parentInSelection;
        int firstAncestorY = -1;
        int lastAncestorY = -1;

        int pathSize = parentPath.size();
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : visible) {
            lastItemY = currentCatY;

            boolean inPath = isPathExpanded(expandedPaths, parentPath, subEntry.getKey());
            int itemCenterY = y + 70 + currentCatY;

            parentPath.add(subEntry.getKey());
            boolean isDeepest = selectedSubcategoryPath.equals(parentPath);
            boolean isAncestor = !selectedSubcategoryPath.isEmpty() &&
                    selectedSubcategoryPath.size() >= parentPath.size() &&
                    selectedSubcategoryPath.subList(0, parentPath.size()).equals(parentPath);
            boolean thisInSelection = isDeepest || isAncestor;
            parentPath.remove(pathSize);

            if (thisInSelection) {
                subtreeContainsSelection = true;
                if (firstAncestorY == -1) firstAncestorY = currentCatY;
                lastAncestorY = currentCatY;
            }

            int hColor = thisInSelection ? ThemeContext.ACCENT : ThemeContext.LINE;
            DrawContextUtils.fill(lineX, itemCenterY + 4, textBaseX - 2, itemCenterY + 5, hColor);

            boolean hasChildren = !subEntry.getValue().subcategories.isEmpty();
            String indicator = hasChildren ? (inPath ? "▼ " : "▶ ") : "  ";
            if (isDeepest) {
                ThemeContext.rect(textBaseX - 3, itemCenterY - 2, 2, 12, ThemeContext.ACCENT);
            }
            int textColor = isDeepest ? ThemeContext.ACCENT : hasChildren && inPath ? ThemeContext.TEXT : ThemeContext.MUTED;
            ThemeContext.text(fr, indicator + subEntry.getValue().name, textBaseX, itemCenterY, textColor);
            currentCatY += 13;

            if (inPath && hasChildren) {
                parentPath.add(subEntry.getKey());
                currentCatY = renderSubTree(subEntry.getValue().subcategories, subEntry.getValue(), expandedPaths, depth + 1, currentCatY, y, innerLeft, fr, parentPath, thisInSelection);
                parentPath.remove(parentPath.size() - 1);
            }
        }

        if (!visible.isEmpty()) {
            int trunkStart = parentCenterY + parentHeight / 2 + 3;
            int trunkEnd = y + 70 + lastItemY + 4;

            if (subtreeContainsSelection && firstAncestorY != -1) {
                int cyanStart = trunkStart;
                int cyanEnd = y + 70 + lastAncestorY + 4;
                DrawContextUtils.fill(lineX, cyanStart, lineX + 1, cyanEnd, ThemeContext.ACCENT);
                if (cyanEnd < trunkEnd) {
                    DrawContextUtils.fill(lineX, cyanEnd, lineX + 1, trunkEnd, ThemeContext.LINE);
                }
            } else {
                DrawContextUtils.fill(lineX, trunkStart, lineX + 1, trunkEnd, ThemeContext.LINE);
            }
        }

        return currentCatY;
    }

    private List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> getVisibleSubcategories(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, Object parent, int depth) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = new ArrayList<>();

        Set<String> matchedSubs = null;
        if (searchedSubcategories != null) {
            if (depth == 1) {
                matchedSubs = searchedSubcategories.get((ConfigProcessor.ProcessedCategory) parent);
            } else if (searchedSubSubcategories != null && parent instanceof ConfigProcessor.ProcessedSubcategory) {
                matchedSubs = searchedSubSubcategories.get((ConfigProcessor.ProcessedSubcategory) parent);
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
        lastMouseButton = MouseCompat.getEventButton();
        lastMousePressed = MouseCompat.getEventButtonState();
        return mouseInput(mouseX, mouseY, lastMouseButton, lastMousePressed);
    }

    private int lastMouseButton = -1;
    private boolean lastMousePressed = false;

    public boolean mouseInput(int mouseX, int mouseY, int button, boolean pressed) {
        lastMouseButton = button;
        lastMousePressed = pressed;
        lastMouseX = mouseX;

        if (!lastMousePressed && lastMouseButton == 0) {
            isDraggingOptionsScroll = false;
            isDraggingCategoryScroll = false;
        }

        int screenW2 = GuiScreenUtils.getScaledWindowWidth();
        int screenH2 = GuiScreenUtils.getScaledWindowHeight();
        int scaleFactor2 = GuiScreenUtils.getScaleFactor();
        int width = screenW2;
        int height = screenH2;

        int xSize = Math.min(width - 100 / scaleFactor2, MAX_WIDTH);
        int ySize = Math.min(height - 100 / scaleFactor2, 400);

        int x = (screenW2 - xSize) / 2;
        int y = (screenH2 - ySize) / 2;

        int adjScaleFactor = Math.max(2, scaleFactor2);

        int innerPadding = 20 / adjScaleFactor;
        int innerTop = y + 49 + innerPadding;
        int innerBottom = y + ySize - 5 - innerPadding;
        int innerLeft = x + RIGHT_PANEL_OFFSET + innerPadding;
        int innerRight = x + xSize - 5 - innerPadding;
        float searchFieldCenterY = innerTop - (20 + innerPadding) / 2f;

        int dist = innerBottom - innerTop - 12;
        int optionsBarStartY = innerTop + 6 + (int) (dist * optionsBarStart);
        int optionsBarEndY = innerTop + 6 + (int) (dist * optionsBarend);
        int optionsBarStartX = innerRight - 12;
        int optionsBarEndX = innerRight - 3;

        int categoryY = -categoryScroll.getValue() + computeCategoryHeight();
        int catDist = innerBottom - innerTop - 12;
        float categoryTotalM = categoryY + categoryScroll.getValue();
        float catBarStart = categoryTotalM > 0 ? categoryScroll.getValue() / categoryTotalM : 0f;
        categoryBarSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / Math.max(1, categoryY + 5 + categoryScroll.getValue()));
        float catBarEnd = catBarStart + categoryBarSize;
        int categoryBarStartY = innerTop + 6 + (int) (catDist * catBarStart);
        int categoryBarEndY = innerTop + 6 + (int) (catDist * catBarEnd);
        int categoryBarStartX = x + innerPadding + 7;
        int categoryBarEndX = x + innerPadding + 12;
        keyboardScrollXCutoff = innerLeft - 10;
        if (lastMousePressed && lastMouseButton == 0) {
            if (mouseX >= optionsBarStartX && mouseX <= optionsBarEndX && mouseY > innerTop + 6 && mouseY < innerBottom - 6) {
                if (mouseY >= optionsBarStartY && mouseY <= optionsBarEndY) {
                    isDraggingOptionsScroll = true;
                    dragMouseStartY = mouseY;
                    dragScrollStartValue = optionsScroll.getValue();
                } else {
                    int optionY = -optionsScroll.getValue() + computeOptionHeight();
                    int visibleHeight = innerBottom - innerTop - 2;
                    int maxScroll = Math.max(0, optionY + optionsScroll.getValue() - visibleHeight);
                    int newScroll = Math.max(0, Math.min((int) ((mouseY - innerTop - 6) / (float) dist * (optionY + optionsScroll.getValue())), maxScroll));
                    optionsScroll.setValue(newScroll);
                    optionsScroll.setTarget(newScroll);
                }
                return true;
            }
            if (mouseX >= categoryBarStartX && mouseX <= categoryBarEndX && mouseY > innerTop + 6 && mouseY < innerBottom - 6) {
                if (mouseY >= categoryBarStartY && mouseY <= categoryBarEndY) {
                    isDraggingCategoryScroll = true;
                    dragMouseStartY = mouseY;
                    dragScrollStartValue = categoryScroll.getValue();
                } else {
                    int catY = -categoryScroll.getValue() + computeCategoryHeight();
                    int visibleHeight = innerBottom - innerTop - 2;
                    int maxScroll = Math.max(0, catY + categoryScroll.getValue() - visibleHeight);
                    int newScroll = Math.max(0, Math.min((int) ((mouseY - innerTop - 6) / (float) dist * (catY + categoryScroll.getValue())), maxScroll));
                    categoryScroll.setValue(newScroll);
                    categoryScroll.setTarget(newScroll);
                }
                return true;
            }

            boolean inIcon = mouseX >= innerRight - 20 && mouseX <= innerRight - 2
                    && mouseY >= searchFieldCenterY - 9 && mouseY <= searchFieldCenterY + 9;
            int strLen = MinecraftCompat.getFontRenderer().getStringWidth(searchField.getText()) + 10;
            int len = Math.max(strLen, minimumSearchSize.getValue());
            boolean inTextRect = minimumSearchSize.getValue() > 1
                    && mouseX >= innerRight - 25 - len && mouseX <= innerRight - 25
                    && mouseY >= searchFieldCenterY - 9 && mouseY <= searchFieldCenterY + 9;
            searchField.setFocus(inIcon || inTextRect);

            if (inTextRect) {
                String old = searchField.getText();
                searchField.mouseClicked(mouseX, mouseY, lastMouseButton);

                if (!searchField.getText().equals(old)) search();
            }
        }

        int dWheel = MouseCompat.getEventDWheel();
        if (mouseY > innerTop && mouseY < innerBottom && dWheel != 0) {
            if (dWheel < 0) dWheel = -1;
            if (dWheel > 0) dWheel = 1;

            if (mouseX < innerLeft) {
                int newTarget = categoryScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) newTarget = 0;

                int catContent = computeCategoryHeight();
                int catY = -newTarget + catContent;
                float catBarSize = catY > 0 ? LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (catY + newTarget)) : 1;
                int barMax = (int) Math.floor((catY + newTarget) - catBarSize * (catY + newTarget));
                if (newTarget > barMax) newTarget = barMax;
                categoryScroll.setTarget(newTarget);
                categoryScroll.resetTimer();
            } else {
                int newTarget = optionsScroll.getTarget() - dWheel * 30;
                if (newTarget < 0) newTarget = 0;

                float barSize = 1;
                int optionY = -newTarget;
                if (getSelectedCategory() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                    ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                    for (VisibleOption visible : visibleOptions(cat)) {
                        GuiOptionEditor editor = visible.option.editor;
                        if (editor == null) continue;
                        optionY += editor.getHeight() + 5;
                        if (optionY > 0) {
                            barSize = LerpUtils.clampZeroOne((float) (innerBottom - innerTop - 2) / (optionY + 5 + newTarget));
                        }
                    }
                }

                int barMax = Math.max(0, (int) Math.floor((optionY + 5 + newTarget) - barSize * (optionY + 5 + newTarget)));
                if (newTarget > barMax) newTarget = barMax;
                optionsScroll.setTarget(newTarget);
                optionsScroll.resetTimer();
            }
        } else if (lastMousePressed && lastMouseButton == 0) {
            int catY = -categoryScroll.getValue();
            int treeClickLeft = x + 4 + innerPadding + 8;
            int treeClickRight = x + 4 + SIDEBAR_WIDTH - innerPadding;
            for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
                if (getSelectedCategory() == null) setSelectedCategory(entry.getKey());

                String ck = entry.getKey();
                if (mouseY >= y + 70 + catY - 7 && mouseY <= y + 70 + catY + 7 && mouseX >= treeClickLeft && mouseX <= treeClickRight) {
                    clickRootCategory(ck);
                    return true;
                }
                catY += 15;

                if (categoriesWithTreeOpen.contains(ck)) {
                    int result = handleSubTreeClick(entry.getValue().subcategories, entry.getValue(), new ArrayList<>(), 1, catY, mouseX, mouseY, y, ck, treeClickLeft, treeClickRight);
                    if (result == -1) return true;
                    catY = result;
                }
            }

            for (int socialIndex = 0; socialIndex < socialsIco.length; socialIndex++) {
                int socialLeft = x + xSize - 23 - 18 * socialIndex;
                if (mouseX >= socialLeft && mouseX <= socialLeft + 16 && mouseY >= y + 6 && mouseY <= y + 23) {
                    UrlOpener.open(socialUrl(socialIndex));
                    return true;
                }
            }
        }

        if (getSelectedCategory() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            int optionWidthDefault = innerRight - innerLeft - 20;
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            boolean[] consumed = {false};
            forEachOption(cat, -optionsScroll.getValue(), innerLeft, innerRight, innerTop, innerBottom, innerPadding, optionWidthDefault, (ed, edX, edY, edW) -> {
                if (ed.mouseInputOverlay(edX, edY, edW, mouseX, mouseY, lastMouseButton, lastMousePressed)) {
                    consumed[0] = true;
                    return true;
                }
                return false;
            });
            if (consumed[0]) return true;
        }

        if (mouseX > innerLeft && mouseX < innerRight && mouseY > innerTop && mouseY < innerBottom) {
            if (getSelectedCategory() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
                int optionWidthDefault = innerRight - innerLeft - 20;
                ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
                boolean[] consumed = {false};
                forEachOption(cat, -optionsScroll.getValue(), innerLeft, innerRight, innerTop, innerBottom, innerPadding, optionWidthDefault, (ed, edX, edY, edW) -> {
                    if (ed.mouseInput(edX, edY, edW, mouseX, mouseY, lastMouseButton, lastMousePressed)) {
                        consumed[0] = true;
                        return true;
                    }
                    return false;
                });
                if (consumed[0]) return true;
            }
        }

        return true;
    }

    private int handleSubTreeClick(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, Object parent, List<String> parentPath, int depth, int catY, int mouseX, int mouseY, int y, String catKey, int treeClickLeft, int treeClickRight) {
        List<Map.Entry<String, ConfigProcessor.ProcessedSubcategory>> visible = getVisibleSubcategories(subs, parent, depth);
        Set<List<String>> expandedPaths = getExpandedPaths(catKey);
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : visible) {
            if (mouseY >= y + 70 + catY - 6 && mouseY <= y + 70 + catY + 6 && mouseX >= treeClickLeft && mouseX <= treeClickRight) {
                List<String> targetPath = new ArrayList<>(parentPath);
                targetPath.add(subEntry.getKey());
                clickSubcategory(catKey, targetPath);
                return -1;
            }
            catY += 13;

            if (isPathExpanded(expandedPaths, parentPath, subEntry.getKey()) && !subEntry.getValue().subcategories.isEmpty()) {
                List<String> childPath = new ArrayList<>(parentPath);
                childPath.add(subEntry.getKey());
                int result = handleSubTreeClick(subEntry.getValue().subcategories, subEntry.getValue(), childPath, depth + 1, catY, mouseX, mouseY, y, catKey, treeClickLeft, treeClickRight);
                if (result == -1) return -1;
                catY = result;
            }
        }
        return catY;
    }

    private Set<List<String>> getExpandedPaths(String catKey) {
        return expandedSubcategoryPaths.computeIfAbsent(catKey, k -> new HashSet<>());
    }

    private boolean isPathExpanded(Set<List<String>> paths, List<String> parentPath, String key) {
        if (paths == null || paths.isEmpty() || key == null) return false;
        List<String> target = new ArrayList<>(parentPath);
        target.add(key);
        return paths.contains(target);
    }

    private void expandPath(String catKey, List<String> path) {
        Set<List<String>> paths = getExpandedPaths(catKey);
        for (int i = 1; i <= path.size(); i++) {
            paths.add(new ArrayList<>(path.subList(0, i)));
        }
    }

    private void collapsePath(String catKey, List<String> path) {
        Set<List<String>> paths = getExpandedPaths(catKey);
        paths.removeIf(p -> p.size() >= path.size() && p.subList(0, path.size()).equals(path));
    }

    private int treeSubcategoryCount(LinkedHashMap<String, ConfigProcessor.ProcessedSubcategory> subs, Object parent, Set<List<String>> expandedPaths, List<String> parentPath, int depth) {
        int count = 0;
        for (Map.Entry<String, ConfigProcessor.ProcessedSubcategory> subEntry : getVisibleSubcategories(subs, parent, depth)) {
            count++;
            if (isPathExpanded(expandedPaths, parentPath, subEntry.getKey()) && !subEntry.getValue().subcategories.isEmpty()) {
                List<String> childPath = new ArrayList<>(parentPath);
                childPath.add(subEntry.getKey());
                count += treeSubcategoryCount(subEntry.getValue().subcategories, subEntry.getValue(), expandedPaths, childPath, depth + 1);
            }
        }
        return count;
    }

    private static final int SIDEBAR_CONTENT_PADDING = 15;

    private int computeCategoryHeight() {
        int h = SIDEBAR_CONTENT_PADDING;
        for (Map.Entry<String, ConfigProcessor.ProcessedCategory> entry : getCurrentConfigEditing().entrySet()) {
            h += 15;
            String catKey = entry.getKey();
            if (categoriesWithTreeOpen.contains(catKey)) {
                ConfigProcessor.ProcessedCategory cat = processedConfig.get(catKey);
                if (cat != null) {
                    Set<List<String>> paths = getExpandedPaths(catKey);
                    h += treeSubcategoryCount(cat.subcategories, cat, paths, new ArrayList<>(), 1) * 13;
                }
            }
        }
        return h;
    }

    private int computeOptionHeight() {
        int h = 5;
        if (getSelectedCategory() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            for (VisibleOption visible : visibleOptions(cat)) {
                GuiOptionEditor editor = visible.option.editor;
                if (editor != null) h += editor.getHeight() + 5;
            }
        }
        return h;
    }

    private int optionContentHeight() {
        return Math.max(0, computeOptionHeight() - 5);
    }


    @Override
    public boolean capturesEscape() {
        if (searchField.getFocus()) return true;

        String category = getSelectedCategory();
        if (category == null || !getCurrentConfigEditing().containsKey(category)) return false;

        ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(category);
        if (cat == null) return false;

        for (VisibleOption visible : visibleOptions(cat)) {
            if (visible.option.editor != null && visible.option.editor.capturesEscape()) return true;
        }
        return false;
    }

    public boolean keyboardInput() {
        return keyboardInput(
                KeyboardCompat.getEventCharacter(),
                KeyboardCompat.getEventKey());
    }

    private char lastKeyChar = 0;
    private int lastKeyCode = 0;
    private boolean lastKeyPressed = false;

    public boolean keyboardInput(char typedChar, int keyCode) {
        lastKeyChar = typedChar;
        lastKeyCode = keyCode;
        lastKeyPressed = true;
        int scaleFactor3 = GuiScreenUtils.getScaleFactor();
        int width = GuiScreenUtils.getScaledWindowWidth();

        int xSize = Math.min(width - 100 / scaleFactor3, MAX_WIDTH);

        int adjScaleFactor = Math.max(2, scaleFactor3);

        int innerPadding = 20 / adjScaleFactor;
        int innerWidth = xSize - RIGHT_PANEL_OFFSET - RIGHT_PANEL_GAP - innerPadding * 2;

        boolean searchFocused = searchField.getFocus();

        if (KeybindHelper.isEscapeKey(keyCode)) {
            if (searchField.getFocus()) {
                searchField.setFocus(false);
                return true;
            }
        }

        if (lastKeyPressed) {
            String old = searchField.getText();
            searchField.keyTyped(lastKeyChar, lastKeyCode);
            searchField.setText(MinecraftCompat.getFontRenderer().trimStringToWidth(searchField.getText(), innerWidth / 2 - 20));

            if (!searchField.getText().equals(old)) search();
        }

        if (!searchFocused && getSelectedCategory() != null && getCurrentConfigEditing().containsKey(getSelectedCategory())) {
            ConfigProcessor.ProcessedCategory cat = getCurrentConfigEditing().get(getSelectedCategory());
            for (VisibleOption visible : visibleOptions(cat)) {
                GuiOptionEditor editor = visible.option.editor;
                if (editor == null) continue;
                if (editor.keyboardInput(lastKeyChar, lastKeyCode)) return true;
            }
        }

        return true;
    }

    private void handleKeyboardPresses() {
        LerpingInteger target = lastMouseX < keyboardScrollXCutoff ? categoryScroll : optionsScroll;
        if (KeyboardCompat.isKeyDown(Keyboard.KEY_DOWN)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            target.setTarget(target.getTarget() + 5);
        } else if (KeyboardCompat.isKeyDown(Keyboard.KEY_UP)) {
            target.setTimeToReachTarget(50);
            target.resetTimer();
            if (target.getTarget() >= 0) target.setTarget(target.getTarget() - 5);
        }

        if (searchField.getFocus()) {
            int heldKey = 0;
            if (KeyboardCompat.isKeyDown(Keyboard.KEY_BACK)) heldKey = Keyboard.KEY_BACK;
            else if (KeyboardCompat.isKeyDown(Keyboard.KEY_DELETE)) heldKey = Keyboard.KEY_DELETE;

            if (heldKey != 0) {
                long now = System.currentTimeMillis();
                if (searchRepeatKey != heldKey) {
                    searchRepeatKey = heldKey;
                    searchRepeatStart = now;
                    searchRepeatLast = now;
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

    private void handleDragScroll(int mouseX, int mouseY) {
        if (isDraggingOptionsScroll) {
            if (MouseCompat.isButtonDown(0)) {
                int sh = GuiScreenUtils.getScaledWindowHeight();
                int sf = GuiScreenUtils.getScaleFactor();
                int ySize = Math.min(sh - 100 / sf, 400);
                int y = (sh - ySize) / 2;
                int adjScaleFactor = Math.max(2, sf);
                int innerPadding = 20 / adjScaleFactor;
                int innerTop = y + 49 + innerPadding;
                int innerBottom = y + ySize - 5 - innerPadding;
                int dist = innerBottom - innerTop - 12;
                float visibleHeight = innerBottom - innerTop - 2;
                float totalContent = visibleHeight / Math.max(0.01f, optionsBarend - optionsBarStart);
                float scrollDelta = (mouseY - dragMouseStartY) * totalContent / dist;
                int maxScroll = Math.max(0, (int) (totalContent - visibleHeight));
                int newScroll = Math.max(0, Math.min((int) (dragScrollStartValue + scrollDelta), maxScroll));
                optionsScroll.setValue(newScroll);
                optionsScroll.setTarget(newScroll);
            } else {
                isDraggingOptionsScroll = false;
            }
        }
        if (isDraggingCategoryScroll) {
            if (MouseCompat.isButtonDown(0)) {
                int sw = GuiScreenUtils.getScaledWindowWidth();
                int sh = GuiScreenUtils.getScaledWindowHeight();
                int sf = GuiScreenUtils.getScaleFactor();
                int xSize = Math.min(sw - 100 / sf, MAX_WIDTH);
                int ySize = Math.min(sh - 100 / sf, 400);
                int x = (sw - xSize) / 2;
                int y = (sh - ySize) / 2;
                int adjScaleFactor = Math.max(2, sf);
                int innerPadding = 20 / adjScaleFactor;
                int innerTop = y + 49 + innerPadding;
                int innerBottom = y + ySize - 5 - innerPadding;
                int innerLeft = x + RIGHT_PANEL_OFFSET + innerPadding;
                keyboardScrollXCutoff = innerLeft - 10;
                int catBarStartX = x + innerPadding + 7;
                int catBarEndX = x + innerPadding + 12;

                if (mouseX >= catBarStartX && mouseX <= catBarEndX) {
                    int dist = innerBottom - innerTop - 12;
                    float visibleHeight = innerBottom - innerTop - 2;
                    float totalContent = visibleHeight / Math.max(0.01f, categoryBarSize);
                    float scrollDelta = (mouseY - dragMouseStartY) * totalContent / dist;
                    int maxScroll = Math.max(0, (int) (totalContent - visibleHeight));
                    int newScroll = Math.max(0, Math.min((int) (dragScrollStartValue + scrollDelta), maxScroll));
                    categoryScroll.setValue(newScroll);
                    categoryScroll.setTarget(newScroll);
                }
            } else {
                isDraggingCategoryScroll = false;
            }
        }
    }

    @FunctionalInterface
    private interface OptionCallback {
        boolean apply(GuiOptionEditor editor, int x, int y, int width);
    }
}
