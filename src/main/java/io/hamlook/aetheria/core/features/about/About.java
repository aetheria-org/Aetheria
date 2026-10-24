package io.hamlook.aetheria.core.features.about;

import io.hamlook.aetheria.core.moulconfig.gui.config.ConfigAnnotations.*;

public class About {

    @ConfigOption(name = "Current Version", desc = "The ATHR version you are running")
    @ConfigEditorVersionDisplay
    public transient Void currentVersion = null;

    @ConfigOption(name = "Website", desc = "Visit the ATHR website")
    @ConfigEditorButton(runnableId = "openWebsite", buttonText = "Open")
    public boolean websiteButton = false;

    @ConfigOption(name = "Discord", desc = "Join the ATHR Discord server")
    @ConfigEditorButton(runnableId = "openDiscord", buttonText = "Open")
    public boolean discordButton = false;

    @ConfigOption(name = "GitHub", desc = "View the ATHR source code on GitHub")
    @ConfigEditorButton(runnableId = "openGithub", buttonText = "Open")
    public boolean githubButton = false;

    @ConfigOption(name = "Modrinth", desc = "View ATHR on Modrinth")
    @ConfigEditorButton(runnableId = "openModrinth", buttonText = "Open")
    public boolean modrinthButton = false;

    @ConfigOption(name = "SkyAtlas", desc = "Sync your gear and stats on SkyAtlas")
    @ConfigEditorButton(runnableId = "openSkyAtlas", buttonText = "Open")
    public boolean skyatlasButton = false;

    @ConfigOption(name = "Used Software / Libraries", desc = "Libraries and projects used by ATHR and their licenses")
    @ConfigEditorAccordion(id = 0)
    public boolean licensesAccordion = false;

    //#if MC < 1.21
    @ConfigOption(name = "Forge", desc = "Forge is available under the LGPL 2.1 license")
    @ConfigEditorButton(runnableId = "openLicenseForge", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean forgeButton = false;
    //#else
    //$$ @ConfigOption(name = "Fabric Loader", desc = "Fabric Loader is available under the Apache 2.0 license")
    //$$ @ConfigEditorButton(runnableId = "openLicenseFabricLoader", buttonText = "Source")
    //$$ @ConfigAccordionId(id = 0)
    //$$ public boolean fabricLoaderButton = false;

    //$$ @ConfigOption(name = "Fabric API", desc = "Fabric API is available under the Apache 2.0 license")
    //$$ @ConfigEditorButton(runnableId = "openLicenseFabricApi", buttonText = "Source")
    //$$ @ConfigAccordionId(id = 0)
    //$$ public boolean fabricApiButton = false;

    //$$ @ConfigOption(name = "Fabric Language Kotlin", desc = "Fabric Language Kotlin is available under the Apache 2.0 license")
    //$$ @ConfigEditorButton(runnableId = "openLicenseFabricLanguageKotlin", buttonText = "Source")
    //$$ @ConfigAccordionId(id = 0)
    //$$ public boolean fabricLanguageKotlinButton = false;

    //$$ @ConfigOption(name = "MixinExtras", desc = "MixinExtras is available under the MIT license")
    //$$ @ConfigEditorButton(runnableId = "openLicenseMixinExtras", buttonText = "Source")
    //$$ @ConfigAccordionId(id = 0)
    //$$ public boolean mixinExtrasButton = false;
    //#endif

    @ConfigOption(name = "Mixin", desc = "Mixin is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseMixin", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean mixinButton = false;

    @ConfigOption(name = "MoulConfig", desc = "Config GUI based on MoulConfig, available under the LGPL 3.0 license")
    @ConfigEditorButton(runnableId = "openLicenseMoulConfig", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean moulConfigButton = false;

    @ConfigOption(name = "Kotlin", desc = "Kotlin is available under the Apache 2.0 license")
    @ConfigEditorButton(runnableId = "openLicenseKotlin", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean kotlinButton = false;

    @ConfigOption(name = "Gson", desc = "Gson is available under the Apache 2.0 license")
    @ConfigEditorButton(runnableId = "openLicenseGson", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean gsonButton = false;

    @ConfigOption(name = "RSyntaxTextArea", desc = "RSyntaxTextArea is available under the BSD 3-Clause license")
    @ConfigEditorButton(runnableId = "openLicenseRsyntaxtextarea", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean rsyntaxTextAreaButton = false;

    @ConfigOption(name = "Java-WebSocket", desc = "Java-WebSocket is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseJavaWebsocket", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean javaWebSocketButton = false;

    @ConfigOption(name = "SLF4J", desc = "SLF4J is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseSlf4j", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean slf4jButton = false;

    @ConfigOption(name = "ImageIO APNG", desc = "ImageIO APNG is available under the GPL 2.0 license with the Classpath Exception")
    @ConfigEditorButton(runnableId = "openLicenseImageioApng", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean imageioApngButton = false;

    @ConfigOption(name = "WebP4J", desc = "WebP4J is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseWebp4j", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean webp4jButton = false;

    @ConfigOption(name = "Lombok", desc = "Lombok is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseLombok", buttonText = "Website")
    @ConfigAccordionId(id = 0)
    public boolean lombokButton = false;

    @ConfigOption(name = "JB Annotations", desc = "JetBrains Annotations is available under the Apache 2 license")
    @ConfigEditorButton(runnableId = "openLicenseJbAnnotations", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean jbAnnotationsButton = false;
}