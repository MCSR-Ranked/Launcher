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
package com.atlauncher.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlauncher.FileSystem;
import com.atlauncher.Gsons;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.multimc.MultiMCInstanceConfig;
import com.atlauncher.data.multimc.MultiMCManifest;
import com.atlauncher.gui.dialogs.InstanceInstallerDialog;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Download;

public class ImportPackUtils {
    public static boolean loadFromUrl(String url) {
        if (url.startsWith("https://modrinth.com/modpack")) {
            return loadFromModrinthUrl(url);
        }

        try {
            Path saveTo = FileSystem.TEMP.resolve(url.endsWith(".mrpack") ? "import.mrpack" : "import.zip");

            new Download().setUrl(url).downloadTo(saveTo).downloadFile();

            return loadFromFile(saveTo.toFile());
        } catch (IOException e) {
            LogManager.error("Failed to download modpack file");
            return false;
        }
    }

    public static boolean loadFromModrinthUrl(String url) {
        if (!url.startsWith("https://modrinth.com/modpack")) {
            LogManager.error("Cannot install as the url was not a Modrinth modpack url");
            return false;
        }

        Pattern pattern = Pattern
                .compile("modrinth\\.com\\/modpack\\/([\\w-]+)");
        Matcher matcher = pattern.matcher(url);

        if (!matcher.find() || matcher.groupCount() < 1) {
            LogManager.error("Cannot install as the url was not a valid Modrinth modpack url");
            return false;
        }

        String packSlug = matcher.group(1);

        LogManager.debug("Found pack with slug " + packSlug);

        try {
            ModrinthProject modrinthProject = ModrinthApi.getProject(packSlug);

            if (modrinthProject == null) {
                LogManager.info("Failed to get pack from Modrinth");
                return false;
            }

            new InstanceInstallerDialog(modrinthProject);
        } catch (Exception e) {
            LogManager.logStackTrace("Failed to install Modrinth pack from URL", e);
            return false;
        }

        return true;
    }

    public static boolean loadFromFile(File file) {
        try {
            Path tmpDir = FileSystem.TEMP.resolve("multimcimport" + file.getName().toString().toLowerCase());

            ArchiveUtils.extract(file.toPath(), tmpDir);

            if (ArchiveUtils.archiveContainsFile(file.toPath(), "mmc-pack.json")) {
                return loadMultiMCFormat(tmpDir);
            }

            if (tmpDir.toFile().list().length == 1
                    && ArchiveUtils.archiveContainsFile(file.toPath(), tmpDir.toFile().list()[0] + "/mmc-pack.json")) {
                return loadMultiMCFormat(tmpDir.resolve(tmpDir.toFile().list()[0]));
            }

            FileUtils.deleteDirectory(tmpDir);

            LogManager.error("Unknown format for importing");
        } catch (Throwable t) {
            LogManager.logStackTrace("Error in zip file for import", t);
        }

        return false;
    }

    public static boolean loadMultiMCFormat(Path extractedPath) {
        try (FileReader fileReader = new FileReader(extractedPath.resolve("mmc-pack.json").toFile());
                InputStream instanceCfgStream = new FileInputStream(extractedPath.resolve("instance.cfg").toFile())) {
            MultiMCManifest manifest = Gsons.MINECRAFT.fromJson(fileReader, MultiMCManifest.class);

            Properties props = new Properties();
            props.load(instanceCfgStream);
            manifest.config = new MultiMCInstanceConfig(props);

            if (manifest.formatVersion != 1) {
                LogManager.error("Cannot install as the format is version " + manifest.formatVersion
                        + " which I cannot install");
                return false;
            }

            new InstanceInstallerDialog(manifest, extractedPath);
        } catch (Exception e) {
            LogManager.logStackTrace("Failed to install MultiMC pack", e);
            return false;
        }

        return true;
    }
}
