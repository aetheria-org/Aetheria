# Enchant Parser Chroma Fix — Plan

## Status
Implemented, built, and deployed. In-game verification pending (see Implementation order).

## Symptom
Enchant Parser colors enchants by level (Poor/Good/Great/Perfect/Ultimate) but the chroma animation never renders — every enchant shows a static color even though `enchantChroma = true` and all tier color strings have a chroma speed > 0 (e.g. `251:170:170:170:170`).

## Root cause

### Bug 1 — `@Redirect` intercepts the wrong `indexOf` (primary)
`MixinFontRenderer_EnchantChroma.java` redirects `String.indexOf(I)` inside `renderStringAtPos` WITHOUT an `ordinal`, so it matches **both** `indexOf(I)` call sites in the method (verified via `javap` on `forge-1.8.9-11.15.1.2318-1.8.9-srgBin.jar`, `func_78255_a`):

1. `"0123456789abcdefklmnor".indexOf(<format char>)` — the format-code lookup (first site, ~offset 51)
2. `"ÀÁÂ…abc…".indexOf(<glyph char>)` — the per-character font-table lookup (second site, ~offset 307)

For every glyph char, the redirect re-runs `indexOf` against the format-codes string → `-1` → since `-1 < 16`, it calls `EnchantChromaRenderer.onColorCode()` → `chromaOn = false` — right before every `renderChar`. So `changeTextColor()` (injected at `renderChar` HEAD) always sees chroma off, and the enchant renders in its static `nearestMcColor`.

### Bug 2 — `getStringWidth` redirect cannot resolve
`@Redirect(method = "getStringWidth", … String.indexOf(I))` targets a call that does not exist: `func_78256_a` (getStringWidth) has no `String.indexOf(I)` (the indexOf lives in `getCharWidth`, `func_78263_a`). Fails to find an injection point at apply time (Mixin 0.7.11 → soft error, that handler not applied). Also unnecessary — vanilla `getStringWidth` already skips `§z` (`getCharWidth('§')` returns `-1`, so the 2-char format sequence contributes 0 width).

## Fix (non-negotiable core)
1. `MixinFontRenderer_EnchantChroma.java`: add `ordinal = 0` to the `renderStringAtPos` @Redirect's `@At` so it only intercepts the **format-code** `indexOf`. Glyph lookups then proceed normally and can no longer reset `chromaOn`.
2. Delete the broken `getStringWidth` @Redirect and its `ATHR$interceptFormatCodeWidth` handler.

## Source verification (1.8.9 MCP sources, `forge-1.8.9-11.15.1.2318-1.8.9-sources.jar`, extracted to `%TEMP%\opencode\fontrenderer\net\minecraft\client\gui\FontRenderer.java`)

All claims below were confirmed line-by-line against the actual 1.8.9 source (and cross-checked with `javap` on the srgBin jar).

### `renderStringAtPos` (private, `func_78255_a`) — the two `indexOf(I)` sites
```java
int i1 = "0123456789abcdefklmnor".indexOf(text.toLowerCase(Locale.ENGLISH).charAt(i + 1));  // offset 51
...
int j = "ÀÁÂ…".indexOf(c0);                                                                    // offset 307
```
- **The format lookup LOWERCASES the char.** Both `§z` and `§Z` arrive at the redirect handler as `'z'` (lowercase). The handler's `c == 'z' || c == 'Z'` check covers both (second branch redundant but harmless).
- **Vanilla clamps unknown codes to white + resets styles:** `if (i1 < 0 || i1 > 15) { i1 = 15; }` (color branch) — a raw `§z` without our mod renders white AND clears styles/color. Our handler returning **22** makes `i1 = 22`: not `< 16`, not 16–21, so the entire format branch body is skipped with no `setColor`, no style reset, and `++i` consumes the `z`. Safe no-op that keeps GL color untouched for the next glyph.
- Per-format branch ordering (offsets): `i1 < 16` color → `setColor(colorCode[i1 + (shadow ? 16 : 0)])`; `i1 == 16` k/l bold (flag=true); 17 m strike; 18 n underline; 19 o italic; 20 r reset; 21 reset+color. Our handler only fires `onColorCode()` for `i1 < 16` and `i1 == 21` (chroma-off resets).

### Shadow flow — the double call is in `drawString`, not a `renderStringShadow` method
```java
// drawString(text, x, y, color, shadow)  (func_175065_a) when shadow=true:
i = this.renderString(text, x + 1.0F, y + 1.0F, color, true);   // offset pass (darkened)
i = Math.max(i, this.renderString(text, x, y, color, false));   // main pass
```
- **No `renderStringShadow` method exists in 1.8.9.** `renderString` (`func_180455_b`) sets GL color from the color param and calls `renderStringAtPos` **exactly once**.
- Consequence for the mixin: `beginRenderString(text, shadow)` fires **twice** per `drawStringWithShadow` (shadow=true offset pass, then false main pass). The existing `renderingShadow` flag is therefore correct.
- **`renderString` darkens the base color** `(color & 0xFCFCFC) >> 2 | color & 0xFF000000` when shadow, and `setColor(red, blue, green, alpha)` (note arg order — field swap) from the color param before calling `renderStringAtPos`.
- **Chroma chars are NOT double-darkened.** The vanilla `/4` darkens the *base color* param, which chroma chars ignore entirely (we override GL color per glyph). Our `÷4` in `changeTextColor()` when `renderingShadow` is the *only* darkening chroma chars get — correct. Format-coded (non-chroma) chars use `colorCode[i1 + 16]` (vanilla's darker table) and never hit our path.

### `renderChar` — the private dispatcher is the correct injection target
```java
private float renderChar(char ch, boolean italic)   // func_181559_a — mixin targets THIS
{
    if (ch == 32) return 4.0F;                      // space: no draw, no texture bind
    int i = "ÀÁÂ…".indexOf(ch);                      // ANOTHER indexOf — but in renderChar, not renderStringAtPos
    return i != -1 && !this.unicodeFlag ? this.renderDefaultChar(i, italic)
                                        : this.renderUnicodeChar(ch, italic);
}
```
- `renderDefaultChar` (`func_78277_a`, protected) and `renderUnicodeChar` both only bind textures + draw quads — **neither sets its own GL color**, so the `GlStateManager.color` from our HEAD inject applies to the glyph. (The earlier SRG-method list confused `func_78277_a` = `renderDefaultChar`, not `renderChar`.)
- `func_181559_a` (the private `renderChar`) is a **private** method; the mixin's `@Inject(method = "renderChar", …)` already matches it. Renaming considerations: a name collision with the accessor/other render methods does not exist here.
- **HEAD inject also fires for space chars** (`ch == 32` early-return happens *after* HEAD). Chroma-style resolution per space is wasted work; the style map lookup is O(1) so it's negligible, but `changeTextColor` can cheaply skip `ch == 32`.

### `getStringWidth` (func_78256_a) — `§z` is 0-width
```java
int k = this.getCharWidth(c0);
if (k < 0 && j < text.length() - 1)      // getCharWidth('§') == -1 → format-sequence branch
{
    ++j;                                  // skip the code char ('z')
    c0 = text.charAt(j);
    if (c0 != 108 && c0 != 76) { if (c0 == 114 || c0 == 82) flag = false; }  // 'z' matches neither
    else { flag = true; }
    k = 0;                                // → 0 width, no bold flag
}
i += k;
```
- Confirmed: `§z` contributes 0 width and does not trip the bold (`l`/`L`) +1-per-char width. Deleting the unresolvable `getStringWidth` redirect is safe; layout width stays correct.

### Accessor correctness improvement
`changeTextColor()` currently reads `Minecraft.getMinecraft().fontRendererObj`. If the *rendering* `FontRenderer` is not `mc.fontRendererObj` (a custom font renderer, some mods), the mixin reads posX/posY from the wrong instance. Fix: the mixin passes the real instance — `ChromaTextRenderer.changeTextColor((FontRenderer)(Object)this, ch)` — since the mixin's `this` IS the rendering FontRenderer. Strict improvement, no downside (1.8.9 GUI text is effectively always `mc.fontRendererObj` today).

Post-fix trace of `§7§zSharpness V`:
- `§7` → color branch, `onColorCode()` → `chromaOn = false`, sets §7 color.
- `§z` → `onChromaCode()` → `chromaOn = true`.
- `Sharpness V` glyphs → `renderChar` HEAD → `changeTextColor()` applies chroma. Genuine color codes (0–15) and `r` (21) still reset; format codes k/l/m/n/o (16–20) leave chroma on.
- Ultimates emit `§<color>§z§l<name>` — `§l` (17) does not reset chroma. Correct.

## SkyblockAddons research (source: `Downloads\SkyblockAddons-main.zip`; features confirmed present in the 1.8.9 v1.7.4 jar via `Feature` enum constants)

### Enchant parsing / descriptions (`features/enchants/EnchantManager.java`)
- **`HIDE_ENCHANT_DESCRIPTION`** is SBA's "compact" toggle — strips the grey enchant-description lore lines. Descriptions are captured per-enchant (`lastEnchant.addLore(...)`), re-emitted only when the feature is *disabled*.
- **`hasLore` gate:** the 2-per-line (`NORMAL`) layout is only used when `!hasLore`; if any enchant has a description, SBA silently falls back to one-per-line. Aetheria already mirrors this (`buildLayout`, `LAYOUT_TWO_COLUMN && !hasLore`).
- **`HIDE_GREY_ENCHANTS`** (vanilla Respiration/Aqua Affinity/Depth Strider/Efficiency lines): SBA makes removal configurable; when disabled it still **accounts** for the grey range during parsing (`accountForAndRemoveGreyEnchants` returns the last grey index; the enchant scan starts at `lastGrey + 1`). Aetheria currently removes unconditionally and always returns -1.
- **Cache invalidation:** SBA's `loreCache` has a `configChanged` flag + `markCacheDirty()` (called on config save) so color/layout changes immediately invalidate cached tooltips. **Aetheria's `Cache` has no equivalent** — in-game config changes to colors/layout/roman-numerals won't refresh hovered tooltips until the item's lore changes.
- **Data-driven registry:** enchants come from `enchants.json` (NBT name, lore name, goodLevel, maxLevel, stacking thresholds) — Aetheria already does the same via `ATHRRepo.KEY_ENCHANTS`.
- **Compatibility mode:** when `ENCHANTMENTS_HIGHLIGHT` is off SBA reconstructs the original format codes from the input line (`getInputEnchantFormat`). Aetheria's simpler "don't touch the tooltip at all when off" is fine.
- **Ordering:** SBA sorts Ultimates → Stacking → Normal (each alphabetical) via `compareTo`. Aetheria's `sortType` ordering matches.
- `correctTooltipWidth()` is functionally identical to Aetheria's.

### Chroma system (`asm/hooks/FontRendererHook.java`, `core/chroma/*`, `ManualChromaManager`)
- **Chroma text is a generic engine, not a feature.** Keyed purely on `§z`: any string containing it gets per-char chroma via `changeTextColor()` reading `fontRenderer.posX/posY` (same accessor Aetheria uses). Shadow pass uses a separate darkened color; alpha from `fontRenderer.alpha`.
- **Global chroma model:** one shared animation (mode/size/speed/saturation/brightness) + a per-feature opt-in list + `TURN_ALL_FEATURES_CHROMA`. `ManualChromaManager.renderingText(feature)` → `getChromaColor(x, y, alpha)` → `doneRenderingText()` is the hook for features drawing text themselves.
- **Fade math:** `hue = ((x + y)/size − timeOffset) % 1`; size scaled to `displayWidth`, text positions scaled by feature scale. Aetheria's `applyMode` is equivalent but adds the position shift on top of the animated base rather than folding time in.
- **Bounded cache:** `MaxSizeHashMap(1000)` memoizes per-string `hasChroma` to avoid rescanning every frame.
- **Shaders:** `MulticolorShaderManager` + `shader/chroma/*` (GLSL) for GUI/3D elements — not applicable to 1.8.9 per-char text here.

### Patcher (source: `Sk1erLLC/Patcher`, `src/main/java/club/sk1er/patcher/hooks/FontRendererHook.java`)
- Patcher's **Optimized Font Renderer replaces the entire `renderStringAtPos`**: it reimplements format-code handling, renders glyphs in immediate mode, and caches a display list per `StringHash(text, r, g, b, a, shadow)`.
- `§z` is parsed by Patcher's own loop as a **white color code** (`styleIndex < 0 → styleIndex = 15`), and Aetheria's `renderChar`-HEAD injection never fires on Patcher's path → per-char chroma cannot work under Patcher's optimized renderer.
- Modern Patcher has **no `shouldOverridePatcher` hook** for third-party mods (SBA's comment is stale — the master hook has zero mod integration). The one lever Aetheria has: Patcher's hook falls back to vanilla when `PatcherConfig.optimizedFontRenderer` is false → could be toggled reflectively. **Decision: document only, no runtime toggle.**
- Patcher's own `getStringWidth` (`getUncachedWidth`) also skips `§z` (width 0), so removing Aetheria's broken width redirect stays correct under Patcher.

## Decisions (locked)
- **Chroma base hue: WHITE (SBA model).** All `§z` text animates the same pure rainbow (hue sweep, sat=1, bright=1) via `ChromaStyle`/`ChromaColour.animatedRainbow`. No preceding-color-code snapshot, no per-tier registry. User confirmed "one hue sweep is fine". (The per-tier-hue idea from earlier discussion is dropped.)
- **Patcher: document only.** No runtime toggle (user decision). Users running Patcher with Optimized Font Renderer enabled get static white enchants; documented caveat. Modern Patcher has no `shouldOverridePatcher` hook; the only lever (reflective `PatcherConfig.optimizedFontRenderer = false`) is deferred.
- **`enchantChromaSpeed` stays dead** — the slider (10–5000) is incompatible with `getSecondsForSpeed` (sane 0–255); speed comes from the color string's first component (per-tier, via `formatColor`). Documented, not wired.
- **`hideEnchantDescriptions` toggle (default ON)** — strips enchant description lines at parse time (never attached → `hasLore` stays false → Normal renders true 2-per-line, Compress packs, Expand shows names only). Early testing showed the "server already omits descriptions" assumption only held on very large stacks (24-enchant Valkyrie); normal 3–5-enchant items ship descriptions, so the toggle was re-added. In `configSignature()` for cache invalidation.

## Abstraction design (make chroma reusable by any feature)

### `ChromaStyle` (new, `core/moulconfig/editors/`)
A **pure-rainbow animation spec** — NOT a color-string wrapper. White base (SBA model), so RGB is dropped entirely:
```java
final class ChromaStyle {
    private final int speed;    // ms per full hue rotation
    private final int alpha;    // 0-255
    private final int mode;     // 0 = All Same, 1 = Fade
    private final float size;   // Fade wavelength in px
    // everything precomputed in the constructor (per-char perf: no string re-decomposition per glyph)
    static ChromaStyle of(String colorString, int mode, float size); // extract speed+alpha
    static ChromaStyle of(String colorString);                        // mode 0, size 120
    int getMode(); float getSize();
    int toArgb();              // animated base, no position gradient
    int toArgb(float x, float y); // base + position shift (applyChromaShift)
}
```
- `ChromaColour` gains `animatedRainbow(int speed, int alpha)` (time-based hue, saturation=1, brightness=1; reuses `startTime` + `getSecondsForSpeed` — the speed>255 negative-rotation quirk stays consistent with the existing color-string path) and `applyChromaShift(int argb, float x, float y, int mode, float size)` (hoisted from `EnchantChromaRenderer`).
- `toArgb(x, y)` = `applyChromaShift(animatedRainbow(speed, alpha), x, y, mode, size)`.

### `ChromaTextRenderer` (rename/generalize `EnchantChromaRenderer`, same package `features/qol/helpers/`)
Generic per-char text chroma engine, callable by anything:

**Direct-call API** (features drawing their own text — e.g. CustomScoreboard):
```java
int w = ChromaTextRenderer.drawString(fr, ChromaStyle.of(color, mode, size), text, x, y, shadow);
int w = ChromaTextRenderer.drawStringWithShadow(fr, style, text, x, y);
```
Implementable as: push style → `fr.drawString(text, x, y, 0xFFFFFFFF, shadow)` → pop style.

**Text path** (`§z` via FontRenderer): the mixin state machine stays, but:
- A style stack (`Deque<ChromaStyle>`) with a **global default** (`setDefaultStyle`). `beginRenderString`/mixin entry points use the top of the stack.
- `changeTextColor(FontRenderer fr, char ch)` applies `style.toArgb(fr.posX, fr.posY)` + shadow darkening (÷4) + `fr.alpha`; skips `ch == 32`.
- **No `ATHRConfig` dependency.** The old `enchantChroma` gate in `beginRenderString` is dropped — the engine no longer reads config. §z emission is per-feature opt-in: the enchant parser already gates in `formatColor()` (`enchantChroma && speed > 0`).
- Optional micro-opt: bounded cache of per-string `contains("§z")` (SBA `MaxSizeHashMap(1000)` style).

### `MixinFontRenderer_Chroma` (rename `MixinFontRenderer_EnchantChroma`)
Same four injections, `ordinal = 0` fix applied, `getStringWidth` redirect removed, `changeTextColor` now receives `(FontRenderer)(Object)this`. Rename touches: `mixins.aetheria.json` (nothing — auto-discovery), refmap (regenerated at build), `MixinGuiChest_BetterContainers` import, `EnchantChromaRenderer` usages.

### Watermark migration (important correction)
`MixinGuiChest_BetterContainers.java:53-55` calls `EnchantChromaRenderer.applyChromaShift(ChromaColour.specialToChromaRGB(watermarkColor), …)`. Migrate it to `ChromaColour.applyChromaShift(...)` — the watermark stays **color-string-driven** (its own speed/alpha from the color string). Do **NOT** convert it to `ChromaStyle`/rainbow — that would change its animation behavior. (`ChromaStyle` is only for the §z text path and direct-call features.)

## Enchant additions (final — `EnchantProcessor`)

1. **Per-tier chroma style.** `formatColor()` appends `§z` when `enchantChroma && speed > 0` AND sets the global default style from *that tier's* color: `ChromaTextRenderer.setDefaultStyle(ChromaStyle.of(color, enchantChromaMode, enchantChromaSize))`. Speed/alpha therefore come from the emitting tier's color string, never from `enchantPerfectColor` (which can be speed 0 → `getSecondsForSpeed(0)` = 60s = effectively frozen). Removes the perfect-color coupling. Last §z-emitter wins — all tiers typically share one speed, matching the "one hue sweep" decision.
2. **Grey-enchant machinery removed entirely.** `GREY_ENCHANT_PATTERN` + `accountForAndRemoveGreyEnchants` deleted — the server no longer emits grey `§7` lines (and the `^(...)` pattern could never match the colored `§9` lines anyway). The enchant scan starts at index 0. No `hideGreyEnchants` config.
3. **Description toggle (default ON).** `hideEnchantDescriptions` strips the grey description lines at parse time (`!containsEnchant && lastEnchant != null && !hideEnchantDescriptions`). The server only omits descriptions for very large enchant stacks, so the toggle is on by default. No parser-side auto-hide needed.
4. **Cache invalidation** — SBA `configChanged` equivalent without new listener infra: `Cache` stores a `configSignature` (concatenation of the fields that affect output: `enchantLayout`, `enchantChroma` toggle, `enchantChromaMode`, `enchantChromaSize`, the 5 tier colors, `romanNumerals`). In `onTooltip`, recompute the signature; if it differs from the cached one, force a rebuild (skip the `isCached` fast path) and store the new signature.

## Implementation order
1. `ChromaColour`: add `animatedRainbow(speed, alpha)` + `applyChromaShift(argb, x, y, mode, size)`.
2. Create `ChromaStyle` (precomputed rainbow spec).
3. Rename + generalize `EnchantChromaRenderer` → `ChromaTextRenderer` (style stack, white base, direct-call `drawString`, `changeTextColor(FontRenderer, char)`).
4. Fix/rename `MixinFontRenderer_Chroma` (ordinal=0, delete width redirect, pass `this` to `changeTextColor`).
5. Migrate watermark to `ChromaColour.applyChromaShift` (color-string-driven, NOT ChromaStyle).
6. Enchant: per-tier `setDefaultStyle` in `formatColor`, grey-machinery removal, cache signature invalidation.
7. Build (`.\gradlew.bat build`), redeploy to `%APPDATA%\.minecraft\mods\aetheria-1.1.3-alpha.jar`, test: chroma animates, per-tier static colors intact when chroma off, config changes refresh tooltips immediately.

All implemented and deployed. Test status: pending in-game verification of animated enchant chroma (previous "same as before" reports were against the pre-fix jar — `compileJava` alone produced no deployable jar).

## Reference material
- SBA source: `Downloads\SkyblockAddons-main.zip`
  - `features/enchants/EnchantManager.java`, `features/enchants/EnchantListLayout.java`
  - `asm/hooks/FontRendererHook.java` (chroma text engine), `asm/FontRendererTransformer.java`
  - `core/chroma/ManualChromaManager.java`, `core/chroma/MulticolorShaderManager.java`
  - `shader/chroma/ChromaShader.java` (+3D/screen/textured variants)
- SBA 1.8.9 v1.7.4 jar (`.minecraft\mods\SkyblockAddons-1.7.4-for-MC-1.8.9.jar`): `Feature` enum confirms `HIDE_ENCHANT_DESCRIPTION`, `HIDE_ENCHANTMENT_LORE`, `HIDE_GREY_ENCHANTS`, `ENCHANT_LAYOUT`, `ENCHANTMENT_LORE_PARSING`, `TURN_ALL_FEATURES_CHROMA`, `USE_NEW_CHROMA_EFFECT`.
- Patcher source: `Sk1erLLC/Patcher` — `src/main/java/club/sk1er/patcher/hooks/FontRendererHook.java` (replaces `renderStringAtPos`, `§z`→white, display-list cache).

## Optional refinements (not planned)
- **Per-tier chroma base hue** (snapshot preceding color code at `§z` time): gives each tier its own starting rainbow, works on combined lines; rejected in favor of white base for simplicity.
- **`enchantChromaSpeed` wiring:** currently dead; `ChromaColour.getSecondsForSpeed` is designed for speed 0–255 but the slider allows 10–5000 (negative seconds → reverse rotation above 255).
- **Patcher runtime toggle** (`PatcherConfig.optimizedFontRenderer = false` reflectively): would restore chroma under Patcher; deferred.
