package com.atlauncher.data.modcheck;

import java.util.List;
import java.util.Objects;

import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.PerformanceManager;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.resource.ModResource;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckUtils;

public class ModCheckManager {

    private static final List<ModData> AVAILABLE_MODS = Lists.newArrayList();

    public static void loadModList() {
        PerformanceManager.start();
        LogManager.debug("Loading ModCheck");

        try {
            JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://me.redlimerl.com/mcsr/modcheck/v2")));
            for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                try {
                    AVAILABLE_MODS.add(new ModData(jsonElement.getAsJsonObject()));
                } catch (Throwable e) {
                    LogManager.logStackTrace(e);
                }
            }
        } catch (Exception e) {
            LogManager.logStackTrace(e);
        }

        LogManager.debug("Done with load ModCheck");
        PerformanceManager.end();
    }

    public static List<ModCheckProject> getAvailableMods(String version) {
        ModVersion mcVersion = ModVersion.of(version);
        List<ModCheckProject> modResourceList = Lists.newArrayList();
        for (ModData availableMod : AVAILABLE_MODS) {
            ModCheckProject project = new ModCheckProject(availableMod, availableMod.getLatestVersionResource(mcVersion));
            if (project.isAvailable() && project.getModResource() != null) modResourceList.add(project);
        }
        return modResourceList;
    }

    public static ModCheckProject getUpdatedProject(String version, ModCheckProject project) {
        for (ModData availableMod : AVAILABLE_MODS) {
            ModVersion mcVersion = ModVersion.of(version);
            if (Objects.equals(project.getName(), availableMod.getName())) {
                ModCheckProject newProject = new ModCheckProject(availableMod, availableMod.getLatestVersionResource(mcVersion));
                if (project.isAvailable() && project.getModResource() != null) return newProject;
            }
        }
        return null;
    }
}
