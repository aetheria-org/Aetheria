# Aetheria Multi-Version IntelliJ Plugin

Standalone IntelliJ IDEA plugin project for editing the EGT comment directives used by Aetheria.

## Features

- **Open File Variant** lists a **Raw File** entry followed by the oldest and newest configured EGT targets; intermediate mapping nodes are omitted. Generated files come from each target's `build/preprocessed/main` directory. EGT does not materialize that directory for the oldest base node, so the plugin resolves its `//#if` / `//#elseif` blocks on demand. Edits are synchronized back to the corresponding shared or target-specific source file, preserving `//$$` markers and directives. Unconditional code remains present in every target variant.
- **Generate from Variant** is available from the editor context menu when a block is selected in a raw source file. Choose the target whose generated code should replace the selection; the selected raw code is retained as the fallback for other targets, wrapped in Minecraft and loader checks.
- The **Aetheria Versions** sidebar also has **Switch Variant...** for the active editor file and **Generate from Variant...** for a selected block in the raw source.
- `src/main/resources/compat-class-mappings.json` inventories the Kotlin compatibility directory and lists raw-class-to-compat-class mappings for autocomplete integrations. Only direct equivalents are mapped; wrappers without a matching raw class are inventoried as compatibility sources instead.
- **Variant edit reminder** appears after a substantial burst of edits inside a preprocessor conditional that has alternate branches.
- **Check All Minecraft Versions** reads `createNode(...)` targets from `root.gradle.kts` and runs each target's Gradle `build` task. The Aetheria Versions tool window streams batched Gradle output with timestamps; enable verbose mode for `--info` output. Compiler diagnostics are listed in the **Global Problems** tab and an orange flag with the problem count appears in the editor's top-right inspection widget; clicking it opens that tab. Source edits are checked after a 30-second quiet period, with a five-minute cooldown. Use the tool window checkbox to disable auto-checks.

## Build

The plugin uses its own Gradle 9 wrapper because current IntelliJ Platform Gradle Plugin releases require Gradle 9. With network access for the Gradle distribution and IntelliJ SDK, run:

```powershell
./gradlew.bat buildPlugin
```

The installable ZIP is written to `build/distributions/`. Use `runIde` to launch a development IDE with the plugin.

The plugin targets the Aetheria project's Java and Kotlin sources and its configured Minecraft/loader targets. Global Problems entries link to the reported source location.
