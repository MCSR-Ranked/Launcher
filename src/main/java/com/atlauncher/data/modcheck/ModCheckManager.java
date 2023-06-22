/*
 * MCSR Ranked Launcher - https://github.com/RedLime/MCSR-Ranked-Launcher
 * Copyright (C) 2023 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.data.modcheck;

import java.util.List;
import java.util.Objects;

import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.PerformanceManager;
import com.atlauncher.utils.OS;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.MCVersion;
import com.pistacium.modcheck.mod.ModInfo;
import com.pistacium.modcheck.mod.RuleIndicator;
import com.pistacium.modcheck.util.ModCheckUtils;

public class ModCheckManager {

    private static final List<ModInfo> AVAILABLE_MODS = Lists.newArrayList();
    public static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final RuleIndicator DEFAULT_RULE = new RuleIndicator(OS.getLWJGLClassifier().split("-")[0], "rsg", true);

    public static void loadModList() {
        PerformanceManager.start();
        LogManager.debug("Loading ModCheck");

        try {
            JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/meta/v4/files.json")));
            for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                try {
                    ModInfo modInfo = GSON.fromJson(jsonElement, ModInfo.class);
                    if (modInfo.getType().equals("fabric_mod")) AVAILABLE_MODS.add(modInfo);
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
        List<ModCheckProject> modResourceList = Lists.newArrayList();
        MCVersion mcVersion = GSON.fromJson("{\"name\":\"" + version + "\",\"value\":\"" + version + "\"}", MCVersion.class);
        for (ModInfo modInfo : AVAILABLE_MODS) {
            ModCheckProject project = new ModCheckProject(modInfo, modInfo.getFileFromVersion(mcVersion, DEFAULT_RULE));
            if (project.isAvailable() && project.getModFile() != null) modResourceList.add(project);
        }
        return modResourceList;
    }

    public static ModCheckProject getUpdatedProject(String version, ModCheckProject project) {
        MCVersion mcVersion = GSON.fromJson("{\"name\":\"" + version + "\",\"value\":\"" + version + "\"}", MCVersion.class);
        for (ModInfo modInfo : AVAILABLE_MODS) {
            if (Objects.equals(project.getName(), modInfo.getName())) {
                ModCheckProject newProject = new ModCheckProject(modInfo, modInfo.getFileFromVersion(mcVersion, DEFAULT_RULE));
                if (project.isAvailable() && project.getModFile() != null) return newProject;
            }
        }
        return null;
    }
}
