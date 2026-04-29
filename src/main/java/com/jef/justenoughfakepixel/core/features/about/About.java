package com.jef.justenoughfakepixel.core.features.about;

import com.jef.justenoughfakepixel.core.config.gui.config.ConfigAnnotations.*;

public class About {

    @ConfigOption(name = "Current Version", desc = "The JEF version you are running")
    @ConfigEditorVersionDisplay
    public transient Void currentVersion = null;

    @ConfigOption(name = "Update Mod", desc = "Update the JEF Mod to latest version")
    @ConfigEditorButton(runnableId = "downloadMod",buttonText = "Download")
    public boolean downloadMod = false;

    @ConfigOption(name = "Update Mod & Shutdown", desc = "Update the JEF Mod to latest version and shutdown automatically, so you can start with latest version")
    @ConfigEditorButton(runnableId = "downloadModS",buttonText = "Download")
    public boolean downloadModS = false;

    @ConfigOption(name = "Website", desc = "Visit the JEF website")
    @ConfigEditorButton(runnableId = "openWebsite", buttonText = "Open")
    public boolean websiteButton = false;

    @ConfigOption(name = "Discord", desc = "Join the JEF Discord server")
    @ConfigEditorButton(runnableId = "openDiscord", buttonText = "Open")
    public boolean discordButton = false;

    @ConfigOption(name = "GitHub", desc = "View the JEF source code on GitHub")
    @ConfigEditorButton(runnableId = "openGithub", buttonText = "Open")
    public boolean githubButton = false;

    @ConfigOption(name = "Used Software / Libraries", desc = "Libraries and projects used by JEF and their licenses")
    @ConfigEditorAccordion(id = 0)
    public boolean licensesAccordion = false;

    @ConfigOption(name = "Forge", desc = "Forge is available under the LGPL 3.0 license")
    @ConfigEditorButton(runnableId = "openLicenseForge", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean forgeButton = false;

    @ConfigOption(name = "Mixin", desc = "Mixin is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseMixin", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean mixinButton = false;

    @ConfigOption(name = "MoulConfig", desc = "Config GUI based on MoulConfig, available under the LGPL 3.0 license")
    @ConfigEditorButton(runnableId = "openLicenseMoulConfig", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean moulConfigButton = false;

    @ConfigOption(name = "Lombok", desc = "Lombok is available under the MIT license")
    @ConfigEditorButton(runnableId = "openLicenseLombok", buttonText = "Website")
    @ConfigAccordionId(id = 0)
    public boolean lombokButton = false;

    @ConfigOption(name = "Reflections", desc = "Reflections is available under the WTFPL / Apache 2 license")
    @ConfigEditorButton(runnableId = "openLicenseReflections", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean reflectionsButton = false;

    @ConfigOption(name = "Javassist", desc = "Javassist is available under the Apache 2 / LGPL 2.1 / MPL 1.1 license")
    @ConfigEditorButton(runnableId = "openLicenseJavassist", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean javassistButton = false;

    @ConfigOption(name = "JB Annotations", desc = "JetBrains Annotations is available under the Apache 2 license")
    @ConfigEditorButton(runnableId = "openLicenseJbAnnotations", buttonText = "Source")
    @ConfigAccordionId(id = 0)
    public boolean jbAnnotationsButton = false;
}