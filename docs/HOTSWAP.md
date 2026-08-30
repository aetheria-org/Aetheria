# DCEVM Setup Guide for Aetheria Development

This guide explains how to set up DCEVM (Dynamic Code Evolution VM) for hot class reloading while working on Aetheria in IntelliJ IDEA.

## What is DCEVM

DCEVM is a patch applied to a Java 8 JVM that allows unlimited class redefinition at runtime. This means you can change method bodies, add or remove methods, and change class structure while the game is running, without restarting it. Combined with HotSwap Agent, this speeds up development significantly.

DCEVM does not install a new JVM. It patches an existing JDK's `jvm.dll` file in place.

## Plugins overview

| Plugin | Purpose | Required? |
|--------|---------|-----------|
| **DCEVM** (JDK patch) | Enables structural class redefinition at JVM level | Yes |
| **HotSwap Agent** (plugin 9552) | Attaches the agent, re-registers Forge event bus on reload | Yes |
| **Single Hotswap** (plugin 14832) | Compiles only the open file (50x faster than rebuild) | Recommended |

All three work together. DCEVM is the foundation. The HotSwap Agent plugin manages attaching the JVM agent automatically. Single Hotswap speeds up the compile step so hotswaps happen in under a second instead of the 5-15 seconds a full Gradle build takes.

## Step 1: Download the correct JDK version

DCEVM patches are tied to a specific Java update number. The installer used for this project requires **Java 8 update 181 (8u181)**. Using any other update (such as the latest 8u4xx builds) will cause the patch to fail or corrupt the JDK.

Download JDK 8u181 for Windows x64 from AdoptOpenJDK's official GitHub releases:

```
https://github.com/AdoptOpenJDK/openjdk8-binaries/releases/download/jdk8u181-b13/OpenJDK8U-jdk_x64_windows_hotspot_8u181b13.zip
```

## Step 2: Extract the JDK

1. Extract the zip somewhere on your machine, for example:
   ```
   C:\Program Files\Java\jdk8u181-b13
   ```
2. Verify the extraction worked by opening Command Prompt and running:
   ```
   "C:\Program Files\Java\jdk8u181-b13\bin\java" -version
   ```
   You should see:
   ```
   openjdk version "1.8.0_181"
   ```

Keep this JDK separate from any other Java installs on your machine. You do not need to remove or replace your existing JDKs.

## Step 3: Run the DCEVM installer

1. Download the DCEVM installer for 8u181 from the official GitHub releases:
   ```
   https://github.com/dcevm/dcevm/releases/download/light-jdk8u181%2B2/DCEVM-8u181-installer.jar
   ```
2. Since the JDK folder above is inside `Program Files`, the installer needs to write files there. Open Command Prompt **as Administrator**.
3. Navigate to the folder containing the installer jar and run:
   ```
   java -jar DCEVM-8u181-installer.jar
   ```
4. In the installer window, click **Add installation directory** and select:
   ```
   C:\Program Files\Java\jdk8u181-b13
   ```
5. Select that entry in the list. It should show as Java Version `1.8.0_181`.
6. Click **Replace by DCEVM**.
7. Confirm the "Replaced by DCEVM" column shows a version string like `Yes (25.71-b01-dcevmlight-26)`. If it instead shows an error such as "could not get dceversion", the Java version does not match what the installer expects. Do not proceed if you see this error, recheck that you used the exact 8u181 build.

## Step 4: Verify the patch worked

Run this from Command Prompt:

```
"C:\Program Files\Java\jdk8u181-b13\bin\java" -version
```

You should now see a line referencing Dynamic Code Evolution, for example:

```
Dynamic Code Evolution 64-Bit Server VM (AdoptOpenJDK)(build 25.71-b01-dcevmlight-26, mixed mode)
```

If this line is missing, the patch did not apply correctly. Repeat Step 3.

## Step 5: Add the JDK to IntelliJ

1. Open the project in IntelliJ.
2. Go to **File → Project Structure → Platform Settings → SDKs**.
3. Click **+ → Add JDK** and select:
   ```
   C:\Program Files\Java\jdk8u181-b13
   ```
   If IntelliJ already detected it automatically (shown under "Detected SDKs" in some dropdowns), you can select it directly instead of adding it manually.
4. Click Apply.

## Step 6: Configure the Minecraft Client run configuration

1. Open **Run → Edit Configurations**.
2. Select the **Minecraft Client** configuration.
3. Set the **module** dropdown to the correct project module (for example `Aetheria.main`). This must be set correctly or the classpath will not resolve.
4. At the top of the run configuration dialog there is a dropdown showing the current JDK (for example `java 8 SDK of 'Aetheria.main'`). Click it and select the DCEVM JDK from the list, for example `1.8 (2) Java 1.8.0_181`. This sets the JDK for this run configuration directly.
5. Click **Modify options** and enable **Shorten command line**.
6. Set **Shorten command line** to **None**. This is required because the default shortening method can prevent the classpath from being read correctly when using an external Java agent or hot reload tooling.
7. Click **Apply**, then **OK**.

## Step 7: Install the HotSwap Agent plugin

1. Go to **File → Settings → Plugins → Marketplace**.
2. Search for **HotSwap Agent** (plugin ID: 9552, by dmitry-zhuravlev) and install it.
3. **Restart IntelliJ completely.** The plugin needs to initialize at IDE startup to inject agent args into run configurations.
4. Go to **Settings → Tools → HotSwap Agent** and make sure the Minecraft Client run config is enabled. The settings page shows all run configs with checkboxes. Make sure the checkbox next to Minecraft Client is checked.

If you skip the restart, the plugin will not attach the agent and Ctrl+F9 will only build without triggering any reload.

## Step 8: Install Single Hotswap (recommended)

1. Go to **File → Settings → Plugins → Marketplace**.
2. Search for **Single Hotswap** (plugin ID: 14832, by LabyStudio) and install it.
3. Restart IntelliJ.
4. You will see a **blue hammer** button next to the green "Build Project" hammer in the toolbar.

Single Hotswap compiles only the file open in the editor using IntelliJ's internal compiler, bypassing Gradle entirely. This makes hotswaps take under a second instead of 5-15 seconds for a full project build. It still uses DCEVM underneath, so all the same reload rules apply.

Use the blue hammer instead of Ctrl+F9 for day-to-day development. Fall back to Ctrl+F9 or the green hammer when you need a full rebuild (new dependencies, build.gradle changes, etc).

## What we implemented

These files were added to the codebase to make hotswap work with Forge's event bus. Contributors do not need to create or modify them. This section is a reference for understanding how hotswap support is wired into the project.

### Dependencies and config

**`build.gradle.kts`**:
- Added `maven("https://repo.nea.moe/releases")` repository
- Added `implementation("moe.nea:hotswapagent-forge:1.0.1")` dependency
- Added `toolchain.vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM)` to prevent Gradle's auto-provisioned JDK from picking up the DCEVM-patched JDK (DCEVM JDKs can crash Gradle's test infrastructure)

**`src/main/resources/hotswap-agent.properties`**:
- `pluginPackages=moe.nea.hotswapagentforge.plugin`: tells HotSwap Agent to load the Forge integration plugin

### Runtime classes

**`io.hamlook.aetheria.core.hotswap.HotswapSupport`**: safe loader. Checks if `moe.nea.hotswapagentforge.forge.HotswapEvent` exists on the classpath before initializing. If HotSwap Agent is not installed (for example in production), this is a silent no-op.

**`io.hamlook.aetheria.core.hotswap.HotswapSupportImpl`**: registers on `MinecraftForge.EVENT_BUS`, listens for `ClassDefinitionEvent.Redefinition`. When a class is hotswapped, it does the following:
1. Finds the matching registered instance by class name in `EventRegistrar`
2. Unregisters it from the event bus
3. Reconstructs a new instance via its primary constructor
4. Re-injects the `INSTANCE` field for Kotlin objects (via reflection + `removeFinal`)
5. Re-registers the new instance on the event bus

**`io.hamlook.aetheria.core.hotswap.HotswapSupportHandle`**: interface for the above.

**`io.hamlook.aetheria.init.EventRegistrar`**: added `removeRegisteredInstance()` and `addRegisteredInstance()` methods so the hotswap implementation can swap instances.

**`io.hamlook.aetheria.Aetheria`**: calls `HotswapSupport.load()` in `preInit()` after config init.

## How it works

There are three layers:

1. **DCEVM** (JDK patch): enables structural hot swap at the JVM level, meaning it can add or remove methods, fields, classes, and enum values. Standard Java hot swap only allows changing method bodies.

2. **HotSwap Agent** (JVM agent): hooks into DCEVM's class redefinition events and dispatches them to framework plugins. The IntelliJ plugin manages attaching this agent automatically.

3. **hotswapagent-forge** (this project's dependency): a HotSwap Agent plugin that fires Forge events (`ClassDefinitionEvent`, `HotswapFinishedEvent`) on the Minecraft event bus when classes are reloaded. Our `HotswapSupportImpl` listens for these events and re-registers event subscribers so the new code actually runs.

Without `hotswapagent-forge`, DCEVM would reload the class bytes, but Forge's event bus would still reference the old subscriber instance. The new method bodies would never be called.

## Verify it's working

1. Launch the game via the `Minecraft Client` run config in **Debug mode** (beetle icon, not the green Run arrow). DCEVM hotswaps only work during debug sessions.
2. Check the console for:
   ```
   [ATHR] HotSwap support loaded
   ```
3. Look for the `HOTSWAP AGENT:` line in the early log. This is the definitive indicator that the agent attached successfully. It appears when the agent loads its first transformer.
4. Make a code change (e.g. add a log line to any `@RegisterEvents` class).
5. Press **Ctrl+Shift+F9** (compile current file) or click the **blue hammer** (Single Hotswap). Do NOT use Ctrl+F9 (full project build) unless you need to.
6. Check the console for:
   ```
   [ATHR] HotSwap: refreshing ClassName
   [ATHR] HotSwap: reconstructed and re-registered ClassName
   ```

If you see these lines, hot reload is working. The change applies immediately without restarting.

### What "Reload classes after compilation" must be set to

Go to **Settings → Build, Execution, Deployment → Debugger → Hot Swap**. Set **Reload classes after compilation** to **Always**. If it is set to "Ask" or "Never", IntelliJ will not trigger the reload after compiling.

### Compile speed

The single biggest factor in hotswap speed is which compile action you use:

| Action | What it compiles | Speed |
|--------|-----------------|-------|
| **Blue hammer** (Single Hotswap) | Only the open file | Under 1 second |
| **Ctrl+Shift+F9** | Current file + direct dependencies | 1-3 seconds |
| **Ctrl+F9** (Build Project) | Everything | 5-15 seconds |

Use the blue hammer or Ctrl+Shift+F9 for day-to-day development. The full build is only needed when dependencies change.

## Troubleshooting

### Debug mode crash: "ClassNotFoundException: com.google.common.collect.ForwardingSet"

**Symptom:** Game crashes immediately on launch in debug mode with a `ClassNotFoundException` for a Guava class, even though Guava is on the classpath.

**Cause:** IntelliJ's "Instrumenting agent" in the debugger is interfering with DCEVM. This is a known conflict between IntelliJ's bytecode instrumentation and the DCEVM patch.

**Fix:**
1. Go to **Settings → Build, Execution, Deployment → Debugger → Async Stack Traces**
2. Uncheck **Instrumenting agent** (or set it to "Off")
3. Apply and restart IntelliJ

### "JVM Flags: 0 total" in the log

**Symptom:** The log shows `JVM Flags: 0 total` even though the HotSwap Agent plugin is installed.

**Cause:** The agent JVM argument was not injected into the run configuration. This happens when the plugin was installed but IntelliJ was not restarted, or the run config was regenerated by Gradle after the plugin was enabled.

**Fix:**
1. Restart IntelliJ after installing the plugin
2. Go to **Run → Edit Configurations → Minecraft Client**
3. Check **Shorten command line** is set to **None**
4. Check the JDK dropdown at the top of the run configuration points to the DCEVM JDK
5. If using the HotSwap Agent plugin: go to **Settings → Tools → HotSwap Agent** and make sure Minecraft Client is checked
6. If the plugin still does not attach, see the manual fallback section below

### Hotswap does not trigger after compile

**Symptom:** You press Ctrl+F9 or Ctrl+Shift+F9, the console shows the build completing, but no `[ATHR] HotSwap: refreshing` lines appear.

**Cause:** "Reload classes after compilation" is not set to "Always".

**Fix:**
1. Go to **Settings → Build, Execution, Deployment → Debugger → Hot Swap**
2. Set **Reload classes after compilation** to **Always**

### Gradle sync resets the run configuration

**Symptom:** After syncing Gradle, your run config loses the DCEVM JDK selection, Shorten command line, or other settings.

**Cause:** Architectury Loom regenerates run configurations on sync.

**Fix:** Reapply Step 6. This is expected behavior. The DCEVM JDK and Shorten command line settings must be re-applied after every Gradle sync.

### HotSwap Agent shows "DCEVM installation not found"

**Symptom:** The HotSwap Agent plugin's settings page shows a warning that DCEVM is not installed, even though `java -version` confirms it is.

**Cause:** The plugin's DCEVM detection logic does not recognize the DCEVM version string format from newer DCEVM builds.

**Fix:** Add `-XX:HotswapAgent=fatjar` to the run configuration's VM options. This tells the agent to load itself as a fat JAR without relying on the DCEVM detection. This bypasses the check entirely.

### "schema change not implemented" error on hotswap

**Symptom:** The console shows `Hot Swap failed: schema change not implemented` or `Operation not supported by VM`.

**Cause:** DCEVM is not installed or the game is not running in debug mode. Standard Java hotswap only supports changing method bodies, not adding/removing methods or fields.

**Fix:**
1. Verify DCEVM is installed by running `java -version` on the DCEVM JDK and confirming the "Dynamic Code Evolution" line
2. Make sure you are launching in **Debug mode** (beetle icon), not Run mode
3. Make sure the run configuration's JDK dropdown points to the DCEVM JDK
4. If all the above are correct, try adding `-XX:HotswapAgent=fatjar` to VM options

### Compile is too slow

**Symptom:** Each hotswap takes 5-15 seconds because Gradle rebuilds the entire project.

**Fix:** Install **Single Hotswap** (plugin 14832) and use the blue hammer instead of Ctrl+F9. See Step 8.

## Alternative: Manual agent attachment

If the HotSwap Agent plugin does not work or you prefer manual control, you can attach the agent yourself:

1. Download `hotswap-agent.jar` from the [HotSwap Agent releases page](https://github.com/HotswapProjects/HotswapAgent/releases). Unzip the release zip and place `hotswap-agent.jar` somewhere on your machine (for example `C:\java\hotswap-agent.jar`).

2. Open **Run → Edit Configurations → Minecraft Client**.

3. Click **Modify options → Add VM options** and add:
   ```
   -javaagent:"C:\java\hotswap-agent.jar"
   ```
   Adjust the path to where you placed the jar.

4. Make sure **Shorten command line** is still set to **None**.

5. Launch in Debug mode. You should see the `HOTSWAP AGENT:` line in the console.

This achieves the same result as the plugin but without requiring the plugin or its settings. The downside is you must manage the agent jar path manually if it moves.

## Limitations

- **Mixin changes always require a restart.** Mixins are applied at class load time via LaunchWrapper, before DCEVM can intercept them.
- **Class hierarchy changes** (adding/removing superclasses) may fail even with DCEVM
- **Config structure changes** may require restart if Gson deserialization depends on the old field layout
- **Event registration** (the `@RegisterEvents` annotation itself) requires a restart. Only the subscriber instances are reconstructed, not re-scanned.

## Notes

- Do not replace your default JDK (used for other projects) with DCEVM. Keep the 8u181 install separate and only select it for this specific run configuration.
- Architectury Loom based projects (such as this one) generate run configurations automatically, and a Gradle sync may reset manual changes to the run configuration. If your JDK or VM option settings disappear after syncing Gradle, reapply Step 6.
- Never run the DCEVM installer against a JDK version that does not match the installer's target update number. Doing so can corrupt that JDK's `jvm.dll` and make Java unusable until restored from the `.dll.backup` file the installer creates automatically.
- The Gradle toolchain is pinned to Adoptium (`JvmVendorSpec.ADOPTIUM`) to prevent DCEVM from being auto-selected for compilation. DCEVM is only used in the IntelliJ run configuration. Do not remove this pin.
- The `hotswapagent-forge` dependency is an `implementation` dependency, but the `shadowJar` task only shadows `shadowImpl` dependencies, so it is not included in the final mod JAR. This is intentional. HotSwap Agent is a development-time tool only.
- There are two different IntelliJ plugins with similar names: **HotSwap Agent** (plugin 9552, by dmitry-zhuravlev) manages the JVM agent attachment, and **HotSwapHelper** (plugin 25171, by gejun123456) is an alternative that provides "Debug with HotSwap" buttons. We use plugin 9552. Plugin 25171 is a separate option if 9552 does not work for your IntelliJ version.
- **Single Hotswap** (plugin 14832, by LabyStudio) is complementary. It does not replace DCEVM or the agent. It just compiles faster by using IntelliJ's internal compiler on a single file instead of Gradle on the whole project.