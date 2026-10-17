package io.hamlook.aetheria.utils.compat

/**
 * Rewrites modern block strings to 1.8.9 shape: `<registryName>[name=value,...]`,
 * bracket omitted when the block has no properties.
 */
object LegacyBlockIds {

    @JvmStatic
    fun legacyStateString(s: String): String =
        //#if MC < 1.16
        s
        //#else
        //$$ convert(s, keepWrapper = false)
        //#endif

    @JvmStatic
    fun legacyBlockString(s: String): String =
        //#if MC < 1.16
        s
        //#else
        //$$ convert(s, keepWrapper = true)
        //#endif

    //#if MC < 1.16
    //#else
    //$$ private const val MODERN_ONLY_PROP = "waterlogged"
    //$$
    //$$ private val DYE_COLORS = listOf(
    //$$     "white", "orange", "magenta", "light_blue", "yellow", "lime",
    //$$     "pink", "gray", "light_gray", "cyan", "purple", "blue",
    //$$     "brown", "green", "red", "black"
    //$$ )
    //$$
    //$$ private val LEGACY_DYE_NAMES = mapOf(
    //$$     "light_gray" to "silver"
    //$$ )
    //$$
    //$$ private val COLOR_FAMILIES = listOf(
    //$$     "_wool" to "minecraft:wool",
    //$$     "_terracotta" to "minecraft:stained_hardened_clay",
    //$$     "_stained_glass" to "minecraft:stained_glass",
    //$$     "_stained_glass_pane" to "minecraft:stained_glass_pane",
    //$$     "_carpet" to "minecraft:carpet"
    //$$ )
    //$$
    //$$ private class LegacyBlock(val id: String, val props: List<String>)
    //$$
    //$$ private val TABLE: Map<String, LegacyBlock> = buildMap {
    //$$     for (c in DYE_COLORS) {
    //$$         for ((suffix, legacyId) in COLOR_FAMILIES) {
    //$$             put("minecraft:$c$suffix", LegacyBlock(legacyId, listOf("color=" + (LEGACY_DYE_NAMES[c] ?: c))))
    //$$         }
    //$$     }
    //$$     put("minecraft:stone", LegacyBlock("minecraft:stone", listOf("variant=stone")))
    //$$     put("minecraft:andesite", LegacyBlock("minecraft:stone", listOf("variant=andesite")))
    //$$     put("minecraft:granite", LegacyBlock("minecraft:stone", listOf("variant=granite")))
    //$$     put("minecraft:diorite", LegacyBlock("minecraft:stone", listOf("variant=diorite")))
    //$$     put("minecraft:polished_granite", LegacyBlock("minecraft:stone", listOf("variant=smooth_granite")))
    //$$     put("minecraft:polished_diorite", LegacyBlock("minecraft:stone", listOf("variant=smooth_diorite")))
    //$$     put("minecraft:polished_andesite", LegacyBlock("minecraft:stone", listOf("variant=smooth_andesite")))
    //$$     put("minecraft:stone_bricks", LegacyBlock("minecraft:stonebrick", listOf("variant=stonebrick")))
    //$$     put("minecraft:mossy_stone_bricks", LegacyBlock("minecraft:stonebrick", listOf("variant=mossy_stonebrick")))
    //$$     put("minecraft:cracked_stone_bricks", LegacyBlock("minecraft:stonebrick", listOf("variant=cracked_stonebrick")))
    //$$     put("minecraft:chiseled_stone_bricks", LegacyBlock("minecraft:stonebrick", listOf("variant=chiseled_stonebrick")))
    //$$     put("minecraft:bricks", LegacyBlock("minecraft:brick_block", emptyList()))
    //$$     put("minecraft:cut_sandstone", LegacyBlock("minecraft:sandstone", listOf("type=sandstone")))
    //$$     put("minecraft:chiseled_sandstone", LegacyBlock("minecraft:sandstone", listOf("type=chiseled_sandstone")))
    //$$     put("minecraft:smooth_sandstone", LegacyBlock("minecraft:sandstone", listOf("type=smooth_sandstone")))
    //$$     put("minecraft:sandstone", LegacyBlock("minecraft:sandstone", listOf("type=sandstone")))
    //$$     put("minecraft:red_sandstone", LegacyBlock("minecraft:red_sandstone", listOf("type=red_sandstone")))
    //$$     put("minecraft:cut_red_sandstone", LegacyBlock("minecraft:red_sandstone", listOf("type=red_sandstone")))
    //$$     put("minecraft:chiseled_red_sandstone", LegacyBlock("minecraft:red_sandstone", listOf("type=chiseled_red_sandstone")))
    //$$     put("minecraft:red_sand", LegacyBlock("minecraft:sand", listOf("variant=red_sand")))
    //$$     put("minecraft:quartz_block", LegacyBlock("minecraft:quartz_block", listOf("variant=default")))
    //$$     put("minecraft:chiseled_quartz_block", LegacyBlock("minecraft:quartz_block", listOf("variant=chiseled")))
    //$$     put("minecraft:quartz_pillar", LegacyBlock("minecraft:quartz_block", listOf("variant=lines_y")))
    //$$     put("minecraft:prismarine", LegacyBlock("minecraft:prismarine", listOf("variant=prismarine")))
    //$$     put("minecraft:prismarine_bricks", LegacyBlock("minecraft:prismarine", listOf("variant=prismarine_bricks")))
    //$$     put("minecraft:dark_prismarine", LegacyBlock("minecraft:prismarine", listOf("variant=dark_prismarine")))
    //$$     put("minecraft:dirt", LegacyBlock("minecraft:dirt", listOf("variant=dirt")))
    //$$     put("minecraft:coarse_dirt", LegacyBlock("minecraft:dirt", listOf("variant=coarse_dirt")))
    //$$     put("minecraft:podzol", LegacyBlock("minecraft:dirt", listOf("variant=podzol")))
    //$$     put("minecraft:grass_block", LegacyBlock("minecraft:grass", listOf("snowy=false")))
    //$$     put("minecraft:short_grass", LegacyBlock("minecraft:tallgrass", listOf("type=tall_grass")))
    //$$     put("minecraft:allium", LegacyBlock("minecraft:red_flower", listOf("type=allium")))
    //$$     put("minecraft:blue_orchid", LegacyBlock("minecraft:red_flower", listOf("type=blue_orchid")))
    //$$     put("minecraft:lava", LegacyBlock("minecraft:flowing_lava", listOf("level")))
    //$$     put("minecraft:cobblestone_stairs", LegacyBlock("minecraft:stone_stairs", listOf("facing", "half", "shape")))
    //$$     put("minecraft:oak_fence", LegacyBlock("minecraft:fence", listOf("north", "east", "south", "west")))
    //$$     put("minecraft:oak_log", LegacyBlock("minecraft:log", listOf("variant=oak", "axis")))
    //$$     put("minecraft:oak_planks", LegacyBlock("minecraft:planks", listOf("variant=oak")))
    //$$     put("minecraft:spruce_planks", LegacyBlock("minecraft:planks", listOf("variant=spruce")))
    //$$     put("minecraft:oak_leaves", LegacyBlock("minecraft:leaves", emptyList()))
    //$$     put("minecraft:spruce_leaves", LegacyBlock("minecraft:leaves", emptyList()))
    //$$     put("minecraft:birch_leaves", LegacyBlock("minecraft:leaves", emptyList()))
    //$$     put("minecraft:jungle_leaves", LegacyBlock("minecraft:leaves", emptyList()))
    //$$     put("minecraft:acacia_leaves", LegacyBlock("minecraft:leaves2", emptyList()))
    //$$     put("minecraft:dark_oak_leaves", LegacyBlock("minecraft:leaves2", emptyList()))
    //$$     put("minecraft:skeleton_skull", LegacyBlock("minecraft:skull", emptyList()))
    //$$     put("minecraft:melon", LegacyBlock("minecraft:melon_block", emptyList()))
    //$$     put("minecraft:end_portal_frame", LegacyBlock("minecraft:end_portal_frame", listOf("eye", "facing")))
    //$$     put("minecraft:command_block", LegacyBlock("minecraft:command_block", listOf("triggered=@conditional")))
    //$$ }
    //$$
    //$$ private val STONE_SLAB_VARIANTS = mapOf(
    //$$     "minecraft:smooth_stone_slab" to "stone",
    //$$     "minecraft:stone_slab" to "stone",
    //$$     "minecraft:cobblestone_slab" to "cobblestone",
    //$$     "minecraft:sandstone_slab" to "sandstone",
    //$$     "minecraft:brick_slab" to "brick",
    //$$     "minecraft:stone_brick_slab" to "stone_brick",
    //$$     "minecraft:nether_brick_slab" to "nether_brick",
    //$$     "minecraft:quartz_slab" to "quartz",
    //$$     "minecraft:red_sandstone_slab" to "red_sandstone"
    //$$ )
    //$$
    //$$ private fun stoneSlab(modernId: String, props: Map<String, String>): LegacyBlock? {
    //$$     val variant = STONE_SLAB_VARIANTS[modernId] ?: return null
    //$$     val red = variant == "red_sandstone"
    //$$     val type = props["type"]
    //$$         ?: return LegacyBlock(if (red) "minecraft:stone_slab2" else "minecraft:stone_slab", emptyList())
    //$$     return if (type == "double") {
    //$$         LegacyBlock(
    //$$             if (red) "minecraft:double_stone_slab2" else "minecraft:double_stone_slab",
    //$$             listOf("seamless=false", "variant=$variant")
    //$$         )
    //$$     } else {
    //$$         LegacyBlock(
    //$$             if (red) "minecraft:stone_slab2" else "minecraft:stone_slab",
    //$$             listOf("half=$type", "variant=$variant")
    //$$         )
    //$$     }
    //$$ }
    //$$
    //$$ private val WOOD_SLAB_VARIANTS = mapOf(
    //$$     "minecraft:oak_slab" to "oak",
    //$$     "minecraft:spruce_slab" to "spruce",
    //$$     "minecraft:birch_slab" to "birch",
    //$$     "minecraft:jungle_slab" to "jungle",
    //$$     "minecraft:acacia_slab" to "acacia",
    //$$     "minecraft:dark_oak_slab" to "dark_oak"
    //$$ )
    //$$
    //$$ private fun woodSlab(modernId: String, props: Map<String, String>): LegacyBlock? {
    //$$     val variant = WOOD_SLAB_VARIANTS[modernId] ?: return null
    //$$     val type = props["type"]
    //$$         ?: return LegacyBlock("minecraft:wooden_slab", emptyList())
    //$$     return if (type == "double") {
    //$$         LegacyBlock("minecraft:double_wooden_slab", listOf("variant=$variant"))
    //$$     } else {
    //$$         LegacyBlock("minecraft:wooden_slab", listOf("half=$type", "variant=$variant"))
    //$$     }
    //$$ }
    //$$
    //$$ private fun convert(s: String, keepWrapper: Boolean): String {
    //$$     val braceEnd = s.indexOf('}')
    //$$     if (!s.startsWith("Block{") || braceEnd < 0) return s
    //$$     val modernId = s.substring(6, braceEnd)
    //$$     val props = parseProps(s, braceEnd)
    //$$     val spec = stoneSlab(modernId, props) ?: woodSlab(modernId, props) ?: TABLE[modernId]
    //$$     if (keepWrapper) return "Block{${spec?.id ?: modernId}}"
    //$$     val legacyId = spec?.id ?: modernId
    //$$     val rendered = if (spec != null) renderSpec(spec.props, props) else passthrough(props)
    //$$     return if (rendered.isEmpty()) legacyId else "$legacyId[${rendered.joinToString(",")}]"
    //$$ }
    //$$
    //$$ private fun parseProps(s: String, from: Int): Map<String, String> {
    //$$     val open = s.indexOf('[', from)
    //$$     if (open < 0) return emptyMap()
    //$$     val close = s.lastIndexOf(']')
    //$$     if (close < open) return emptyMap()
    //$$     val out = LinkedHashMap<String, String>()
    //$$     for (entry in s.substring(open + 1, close).split(',')) {
    //$$         val eq = entry.indexOf('=')
    //$$         if (eq <= 0) continue
    //$$         out[entry.substring(0, eq).trim()] = entry.substring(eq + 1).trim()
    //$$     }
    //$$     return out
    //$$ }
    //$$
    //$$ private fun renderSpec(spec: List<String>, props: Map<String, String>): List<String> {
    //$$     val out = ArrayList<String>(spec.size)
    //$$     for (template in spec) {
    //$$         val eq = template.indexOf('=')
    //$$         if (eq < 0) {
    //$$             val value = props[template]
    //$$             if (value != null) out.add("$template=$value")
    //$$             continue
    //$$         }
    //$$         val name = template.substring(0, eq)
    //$$         val raw = template.substring(eq + 1)
    //$$         val value = if (raw.startsWith("@")) props[raw.substring(1)] else raw
    //$$         if (value != null) out.add("$name=$value")
    //$$     }
    //$$     out.sort()
    //$$     return out
    //$$ }
    //$$
    //$$ private fun passthrough(props: Map<String, String>): List<String> {
    //$$     val out = ArrayList<String>(props.size)
    //$$     for ((name, value) in props) {
    //$$         if (name == MODERN_ONLY_PROP) continue
    //$$         out.add("$name=$value")
    //$$     }
    //$$     out.sort()
    //$$     return out
    //$$ }
    //#endif
}