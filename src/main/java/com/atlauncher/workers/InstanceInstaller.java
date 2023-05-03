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
package com.atlauncher.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JDialog;
import javax.swing.SwingWorker;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.Data;
import com.atlauncher.FileSystem;
import com.atlauncher.Gsons;
import com.atlauncher.Network;
import com.atlauncher.constants.Constants;
import com.atlauncher.data.APIResponse;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.data.InstanceLauncher;
import com.atlauncher.data.Type;
import com.atlauncher.data.json.Delete;
import com.atlauncher.data.json.Deletes;
import com.atlauncher.data.json.DownloadType;
import com.atlauncher.data.json.Keep;
import com.atlauncher.data.json.Keeps;
import com.atlauncher.data.json.Mod;
import com.atlauncher.data.json.ModType;
import com.atlauncher.data.json.Version;
import com.atlauncher.data.minecraft.ArgumentRule;
import com.atlauncher.data.minecraft.Arguments;
import com.atlauncher.data.minecraft.AssetIndex;
import com.atlauncher.data.minecraft.Download;
import com.atlauncher.data.minecraft.Downloads;
import com.atlauncher.data.minecraft.FabricMod;
import com.atlauncher.data.minecraft.JavaRuntime;
import com.atlauncher.data.minecraft.JavaRuntimeManifest;
import com.atlauncher.data.minecraft.JavaRuntimeManifestFileType;
import com.atlauncher.data.minecraft.JavaRuntimes;
import com.atlauncher.data.minecraft.Library;
import com.atlauncher.data.minecraft.LoggingFile;
import com.atlauncher.data.minecraft.MCMod;
import com.atlauncher.data.minecraft.MinecraftVersion;
import com.atlauncher.data.minecraft.MojangAssetIndex;
import com.atlauncher.data.minecraft.MojangDownload;
import com.atlauncher.data.minecraft.MojangDownloads;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.loaders.Loader;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.modrinth.ModrinthFile;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.data.multimc.MultiMCComponent;
import com.atlauncher.data.multimc.MultiMCManifest;
import com.atlauncher.exceptions.LocalException;
import com.atlauncher.interfaces.NetworkProgressable;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.MinecraftManager;
import com.atlauncher.network.DownloadPool;
import com.atlauncher.utils.ArchiveUtils;
import com.atlauncher.utils.FileUtils;
import com.atlauncher.utils.Hashing;
import com.atlauncher.utils.Java;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;
import com.atlauncher.utils.walker.CaseFileVisitor;
import com.google.gson.reflect.TypeToken;

import okhttp3.OkHttpClient;

public class InstanceInstaller extends SwingWorker<Boolean, Void> implements NetworkProgressable {
    protected double percent = 0.0; // Percent done installing
    protected double subPercent = 0.0; // Percent done sub installing
    protected double totalBytes = 0; // Total number of bytes to download
    protected double downloadedBytes = 0; // Total number of bytes downloaded

    public Instance instance = null;
    public final String name;
    public final com.atlauncher.data.Pack pack;
    public final com.atlauncher.data.PackVersion version;
    public final String shareCode;
    public final boolean showModsChooser;
    public LoaderVersion loaderVersion;
    public Path modrinthExtractedPath;
    public final MultiMCManifest multiMCManifest;
    public final Path multiMCExtractedPath;

    public boolean isReinstall;
    public boolean changingLoader;
    public boolean instanceIsCorrupt;
    public boolean saveMods;

    public final Path root;
    public final Path temp;

    public Loader loader;
    public com.atlauncher.data.json.Version packVersion;
    public VersionManifestVersion minecraftVersionManifest = null;
    public MinecraftVersion minecraftVersion;

    public List<Mod> allMods;
    public List<Mod> selectedMods;
    public List<Mod> unselectedMods = new ArrayList<>();
    public List<DisableableMod> modsInstalled = new ArrayList<>();

    public boolean assetsMapToResources = false;

    private boolean savedReis = false; // If Reis Minimap stuff was found and saved
    private boolean savedZans = false; // If Zans Minimap stuff was found and saved
    private boolean savedNEICfg = false; // If NEI Config was found and saved
    private boolean savedOptionsTxt = false; // If options.txt was found and saved
    private boolean savedServersDat = false; // If servers.dat was found and saved
    private boolean savedPortalGunSounds = false; // If Portal Gun Sounds was found and saved

    public String mainClass;
    public Arguments arguments;
    public boolean success;
    private JDialog dialog;
    public Instance resultInstance;

    public InstanceInstaller(String name, com.atlauncher.data.Pack pack, com.atlauncher.data.PackVersion version,
            boolean isReinstall, boolean changingLoader, boolean saveMods, String shareCode,
            boolean showModsChooser, LoaderVersion loaderVersion,
            Path modrinthExtractedPath, MultiMCManifest multiMCManifest,
            Path multiMCExtractedPath, JDialog dialog) {
        this.name = name;
        this.pack = pack;
        this.version = version;
        this.isReinstall = isReinstall;
        this.changingLoader = changingLoader;
        this.saveMods = saveMods;
        this.shareCode = shareCode;
        this.showModsChooser = showModsChooser;
        this.dialog = dialog;

        this.root = FileSystem.INSTANCES.resolve(name.replaceAll("[^A-Za-z0-9]", ""));

        this.temp = FileSystem.TEMP.resolve(pack.getSafeName() + "_" + version.getSafeVersion());

        this.loaderVersion = loaderVersion;
        this.modrinthExtractedPath = modrinthExtractedPath;
        this.multiMCManifest = multiMCManifest;
        this.multiMCExtractedPath = multiMCExtractedPath;
    }

    public void setInstance(Instance instance) {
        this.instance = instance;
    }

    private boolean success(boolean success) {
        this.success = success;

        return success;
    }

    @Override
    protected Boolean doInBackground() throws Exception {
        LogManager.info("Started install of " + this.pack.name + " version " + this.version.version);

        if (this.loaderVersion != null) {
            LogManager.info("Using loader version " + this.loaderVersion.version);
        }

        try {

            if (changingLoader) {
                generatePackVersionForVanilla();

                downloadMinecraftVersionJson();

                this.loader = this.packVersion.getLoader().getLoader(this.temp.resolve("loader").toFile(), this,
                        this.loaderVersion);

                downloadLoader();
                if (isCancelled()) {
                    return success(false);
                }

                determineMainClass();
                determineArguments();

                downloadLibraries();
                if (isCancelled()) {
                    return success(false);
                }

                organiseLibraries();
                if (isCancelled()) {
                    return success(false);
                }

                installLoader();
                if (isCancelled()) {
                    return success(false);
                }

                saveInstanceJson();

                return success(true);
            }

            if (multiMCManifest != null) {
                generatePackVersionFromMultiMC();
            } else if (pack.vanillaInstance) {
                generatePackVersionForVanilla();
            } else {
                downloadPackVersionJson();
            }

            downloadMinecraftVersionJson();

            if (this.packVersion.messages != null) {
                showMessages();
            }

            determineModsToBeInstalled();

            if (isCancelled()) {
                return success(false);
            }

            backupSelectFiles();
            addPercent(5);

            prepareFilesystem();

            if (this.packVersion.loader != null && this.packVersion.loader.className != null) {
                this.loader = this.packVersion.getLoader().getLoader(this.temp.resolve("loader").toFile(), this,
                        this.loaderVersion);

                if (this.loaderVersion == null) {
                    this.loaderVersion = this.loader.getLoaderVersion();
                }

                downloadLoader();
            }

            install();

            if (isCancelled()) {
                return success(false);
            }

            saveInstanceJson();
            return success(true);
        } catch (Exception e) {
            success(false);
            cancel(true);
            LogManager.logStackTrace(e);
        }

        return success(false);
    }

    private String getAnalyticsCategory() {
        if (this.multiMCManifest != null) {
            return "MultiMCPack";
        } else if (this.pack.vanillaInstance) {
            return "Vanilla";
        }

        return "Pack";
    }

    private void downloadPackVersionJson() throws Exception {
        addPercent(5);
        // #. {0} is the platform the modpack is from
        fireTask(GetText.tr("Generating Pack Version From {0}", Constants.LAUNCHER_NAME));
        fireSubProgressUnknown();

        this.packVersion = com.atlauncher.network.Download.build().cached()
                .setUrl(this.pack.getJsonDownloadUrl(version.version)).asClass(com.atlauncher.data.json.Version.class);

        if (this.packVersion == null) {
            throw new Exception("Failed to download pack version definition");
        }

        this.packVersion.compileColours();

        hideSubProgressBar();
    }

    private void generatePackVersionFromMultiMC() throws Exception {
        addPercent(5);
        // #. {0} is the platform the modpack is from
        fireTask(GetText.tr("Generating Pack Version From {0}", "MultiMC"));
        fireSubProgressUnknown();

        if (multiMCManifest.formatVersion != 1) {
            throw new Exception("Format is version " + multiMCManifest.formatVersion + " which I cannot install");
        }

        Optional<MultiMCComponent> minecraftVersionComponent = multiMCManifest.components.stream()
                .filter(c -> c.uid.equalsIgnoreCase("net.minecraft")).findFirst();

        if (!minecraftVersionComponent.isPresent()) {
            throw new Exception("No net.minecraft component present in manifest");
        }

        String minecraftVersion = minecraftVersionComponent.get().version;

        this.packVersion = new Version();
        packVersion.version = "1";
        packVersion.minecraft = minecraftVersion;
        packVersion.enableCurseForgeIntegration = true;
        packVersion.enableEditingMods = true;

        packVersion.loader = new com.atlauncher.data.json.Loader();

        MultiMCComponent fabricLoaderComponent = multiMCManifest.components.stream()
                .filter(c -> c.uid.equalsIgnoreCase("net.fabricmc.fabric-loader")).findFirst().orElse(null);
        MultiMCComponent quiltLoaderComponent = multiMCManifest.components.stream()
                .filter(c -> c.uid.equalsIgnoreCase("org.quiltmc.quilt-loader")).findFirst().orElse(null);

        if (fabricLoaderComponent != null) {
            String fabricVersionString = fabricLoaderComponent.version;

            Map<String, Object> loaderMeta = new HashMap<>();
            loaderMeta.put("minecraft", minecraftVersion);
            loaderMeta.put("loader", fabricVersionString);
            packVersion.loader.metadata = loaderMeta;

            // should probably look for the patch, but this is sound logic
            if (MinecraftManager.getMinecraftVersion(minecraftVersion).is1132OrOlder()) {
                packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.legacyfabric.LegacyFabricLoader";
            } else {
                packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.fabric.FabricLoader";
            }
        } else if (quiltLoaderComponent != null) {
            String quiltVersionString = quiltLoaderComponent.version;

            Map<String, Object> loaderMeta = new HashMap<>();
            loaderMeta.put("minecraft", minecraftVersion);
            loaderMeta.put("loader", quiltVersionString);
            packVersion.loader.metadata = loaderMeta;
            packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.quilt.QuiltLoader";
        }

        hideSubProgressBar();
    }

    private void generatePackVersionForVanilla() throws Exception {
        addPercent(5);
        // #. {0} is the platform the modpack is from
        fireTask(GetText.tr("Generating Pack Version From {0}", "Vanilla Minecraft"));
        fireSubProgressUnknown();

        this.packVersion = new Version();
        packVersion.version = version.minecraftVersion.id;
        packVersion.minecraft = version.minecraftVersion.id;
        packVersion.enableCurseForgeIntegration = true;
        packVersion.enableEditingMods = true;

        if (loaderVersion != null && loaderVersion.isFabric()) {
            packVersion.loader = new com.atlauncher.data.json.Loader();
            Map<String, Object> loaderMeta = new HashMap<>();
            loaderMeta.put("minecraft", version.minecraftVersion.id);
            loaderMeta.put("loader", loaderVersion.version);
            packVersion.loader.metadata = loaderMeta;
            packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.fabric.FabricLoader";
        } else if (loaderVersion != null && loaderVersion.isLegacyFabric()) {
            packVersion.loader = new com.atlauncher.data.json.Loader();
            Map<String, Object> loaderMeta = new HashMap<>();
            loaderMeta.put("minecraft", version.minecraftVersion.id);
            loaderMeta.put("loader", loaderVersion.version);
            packVersion.loader.metadata = loaderMeta;
            packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.legacyfabric.LegacyFabricLoader";
        } else if (loaderVersion != null && loaderVersion.isQuilt()) {
            packVersion.loader = new com.atlauncher.data.json.Loader();
            Map<String, Object> loaderMeta = new HashMap<>();
            loaderMeta.put("minecraft", version.minecraftVersion.id);
            loaderMeta.put("loader", loaderVersion.version);
            packVersion.loader.metadata = loaderMeta;
            packVersion.loader.className = "com.atlauncher.data.minecraft.loaders.quilt.QuiltLoader";
        }

        hideSubProgressBar();
    }

    private void downloadMinecraftVersionJson() throws Exception {
        addPercent(5);
        fireTask(GetText.tr("Downloading Minecraft Definition"));
        fireSubProgressUnknown();

        minecraftVersionManifest = MinecraftManager.getMinecraftVersion(this.packVersion.getMinecraft());

        com.atlauncher.network.Download download = com.atlauncher.network.Download.build()
                .setUrl(minecraftVersionManifest.url).hash(minecraftVersionManifest.sha1)
                .size(minecraftVersionManifest.size)
                .downloadTo(FileSystem.MINECRAFT_VERSIONS_JSON.resolve(minecraftVersionManifest.id + ".json"));

        this.minecraftVersion = download.asClass(MinecraftVersion.class);

        if (this.minecraftVersion == null) {
            LogManager.error("Failed to download Minecraft json.");
            this.cancel(true);
            return;
        }

        hideSubProgressBar();
    }

    private void downloadLoader() throws Exception {
        addPercent(5);
        fireTask(GetText.tr("Downloading Loader"));
        fireSubProgressUnknown();

        this.loader.downloadAndExtractInstaller();

        hideSubProgressBar();
    }

    private void showMessages() throws Exception {
        int ret = 0;

        if (this.isReinstall && this.packVersion.messages.update != null) {
            ret = this.packVersion.messages.showUpdateMessage(this.pack);
        } else if (!this.isReinstall && this.packVersion.messages.install != null) {
            ret = this.packVersion.messages.showInstallMessage(this.pack);
        }

        if (ret != 0) {
            throw new LocalException("Install cancelled after viewing message!");
        }
    }

    private void determineModsToBeInstalled() {
        this.allMods = sortMods(this.packVersion.getClientInstallMods(this));

        boolean hasOptional = this.allMods.stream().anyMatch(Mod::isOptional);

        if (this.allMods.size() != 0 && hasOptional) {
            com.atlauncher.gui.dialogs.ModsChooser modsChooser = new com.atlauncher.gui.dialogs.ModsChooser(this);

            if (this.showModsChooser) {
                modsChooser.setVisible(true);
            }

            if (modsChooser.wasClosed()) {
                this.cancel(true);
                return;
            }

            this.selectedMods = modsChooser.getSelectedMods();
            this.unselectedMods = modsChooser.getUnselectedMods();
        }

        if (!hasOptional) {
            this.selectedMods = this.allMods;
        }

        for (com.atlauncher.data.json.Mod mod : this.selectedMods) {
            String file = mod.getFile();

            if (mod.type == ModType.mods
                    && this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.upper) {
                file = file.substring(0, file.lastIndexOf(".")).toUpperCase() + file.substring(file.lastIndexOf("."));
            } else if (mod.type == ModType.mods
                    && this.packVersion.getCaseAllFiles() == com.atlauncher.data.json.CaseType.lower) {
                file = file.substring(0, file.lastIndexOf(".")).toLowerCase() + file.substring(file.lastIndexOf("."));
            }

            this.modsInstalled.add(new com.atlauncher.data.DisableableMod(mod.getName(), mod.getVersion(),
                    mod.isOptional(), file, mod.path,
                    com.atlauncher.data.Type.valueOf(com.atlauncher.data.Type.class, mod.getType().toString()),
                    this.packVersion.getColour(mod.getColour()), mod.getDescription(), false, false, true, false,
                    mod.modrinthProject, mod.modrinthVersion, mod.modCheckProject));
        }

        if (this.isReinstall && instance.hasCustomMods()) {
            // user chose to save mods even though Minecraft version changed, so add them to
            // the installed list
            if (this.saveMods || instance.id.equalsIgnoreCase(version.minecraftVersion.id)) {
                modsInstalled.addAll(instance.getCustomDisableableMods());
            }

            // user choosing to not save mods and Minecraft version changed, so delete
            // custom mods
            if (!this.saveMods && !instance.id.equalsIgnoreCase(version.minecraftVersion.id)) {
                for (com.atlauncher.data.DisableableMod mod : instance.getCustomDisableableMods()) {
                    this.instance.launcher.mods.remove(mod);
                    Utils.delete((mod.isDisabled() ? mod.getDisabledFile(this.instance) : mod.getFile(this.instance)));
                }
            }
        }

        if (this.multiMCManifest != null) {
            String minecraftFolder = Files.exists(multiMCExtractedPath.resolve(".minecraft")) ? ".minecraft"
                    : "minecraft";

            if (Files.exists(multiMCExtractedPath.resolve(minecraftFolder + "/mods"))) {
                try (Stream<Path> list = Files.list(multiMCExtractedPath.resolve(minecraftFolder + "/mods"))) {
                    this.modsInstalled.addAll(list.filter(p -> !Files.isDirectory(p))
                            .filter(p -> p.toString().toLowerCase().endsWith(".jar")
                                    || p.toString().toLowerCase().endsWith(".zip"))
                            .map(p -> convertPathToDisableableMod(p, Type.mods)).collect(Collectors.toList()));
                } catch (IOException e) {
                    LogManager.logStackTrace(e);
                }
            }

            if (Files.exists(multiMCExtractedPath.resolve(minecraftFolder + "/mods/" + packVersion.minecraft))) {
                try (Stream<Path> list = Files
                        .list(multiMCExtractedPath.resolve(minecraftFolder + "/mods/" + packVersion.minecraft))) {
                    this.modsInstalled.addAll(list.filter(p -> !Files.isDirectory(p))
                            .filter(p -> p.toString().toLowerCase().endsWith(".jar")
                                    || p.toString().toLowerCase().endsWith(".zip"))
                            .map(p -> convertPathToDisableableMod(p, Type.dependency)).collect(Collectors.toList()));
                } catch (IOException e) {
                    LogManager.logStackTrace(e);
                }
            }

            if (Files.exists(multiMCExtractedPath.resolve(minecraftFolder + "/mods/ic2"))) {
                try (Stream<Path> list = Files.list(multiMCExtractedPath.resolve(minecraftFolder + "/mods/ic2"))) {
                    this.modsInstalled.addAll(list.filter(p -> !Files.isDirectory(p))
                            .filter(p -> p.toString().toLowerCase().endsWith(".jar")
                                    || p.toString().toLowerCase().endsWith(".zip"))
                            .map(p -> convertPathToDisableableMod(p, Type.ic2lib)).collect(Collectors.toList()));
                } catch (IOException e) {
                    LogManager.logStackTrace(e);
                }
            }
        }
    }

    private DisableableMod convertPathToDisableableMod(Path p, Type t) {
        DisableableMod mod = new DisableableMod();

        mod.optional = true;
        mod.name = p.getFileName().toString();
        mod.version = "Unknown";
        mod.description = null;

        MCMod mcMod = Utils.getMCModForFile(p.toFile());
        if (mcMod != null) {
            mod.name = Optional.ofNullable(mcMod.name).orElse(p.getFileName().toString());
            mod.version = Optional.ofNullable(mcMod.version).orElse("Unknown");
            mod.description = Optional.ofNullable(mcMod.description).orElse(null);
        } else {
            FabricMod fabricMod = Utils.getFabricModForFile(p.toFile());
            if (fabricMod != null) {
                mod.name = Optional.ofNullable(fabricMod.name).orElse(p.getFileName().toString());
                mod.version = Optional.ofNullable(fabricMod.version).orElse("Unknown");
                mod.description = Optional.ofNullable(fabricMod.description).orElse(null);
            }
        }

        mod.file = p.getFileName().toString();
        mod.type = t;

        return mod;
    }

    private Boolean install() throws Exception {
        this.instanceIsCorrupt = true; // From this point on the instance has become corrupt

        determineMainClass();
        determineArguments();

        downloadResources();
        if (isCancelled()) {
            return false;
        }

        downloadMinecraft();
        if (isCancelled()) {
            return false;
        }

        downloadLoggingClient();
        if (isCancelled()) {
            return false;
        }

        downloadLibraries();
        if (isCancelled()) {
            return false;
        }

        organiseLibraries();
        if (isCancelled()) {
            return false;
        }

        downloadRuntime();
        if (isCancelled()) {
            return false;
        }

        installLoader();
        if (isCancelled()) {
            return false;
        }

        downloadMods();
        if (isCancelled()) {
            return false;
        }

        installMods();
        if (isCancelled()) {
            return false;
        }

        installLegacyJavaFixer();
        if (isCancelled()) {
            return false;
        }

        runCaseConversion();
        if (isCancelled()) {
            return false;
        }

        runActions();
        if (isCancelled()) {
            return false;
        }

        installConfigs();
        if (isCancelled()) {
            return false;
        }

        downloadInstanceImage();
        if (isCancelled()) {
            return false;
        }

        checkModsOnModrinth();
        if (isCancelled()) {
            return false;
        }

        // Copy over common configs if any
        if (FileSystem.COMMON.toFile().listFiles().length != 0) {
            Utils.copyDirectory(FileSystem.COMMON.toFile(), this.root.toFile());
        }

        restoreSelectFiles();

        writeLog4ShellExploitArgumentsForForgeScripts();

        return true;
    }

    private void saveInstanceJson() {
        Instance instance = new Instance(this.minecraftVersion);
        instance.ROOT = this.root;
        InstanceLauncher instanceLauncher;

        if (!this.isReinstall) {
            instanceLauncher = new InstanceLauncher();
        } else {
            instanceLauncher = this.instance.launcher;
        }

        instance.libraries = this.getLibraries();
        instance.mainClass = this.mainClass;
        instance.arguments = this.arguments;

        instanceLauncher.loaderVersion = this.loaderVersion;

        if (!changingLoader) {
            instanceLauncher.name = this.name;
            instanceLauncher.pack = this.pack.name;
            instanceLauncher.description = this.pack.description;
            instanceLauncher.packId = this.pack.id;
            instanceLauncher.externalPackId = this.pack.externalId;
            instanceLauncher.version = this.packVersion.version;
            instanceLauncher.java = this.packVersion.java;
            instanceLauncher.enableEditingMods = this.packVersion.enableEditingMods;
            instanceLauncher.isDev = this.version.isDev;
            instanceLauncher.isPlayable = true;
            instanceLauncher.mods = this.modsInstalled;
            instanceLauncher.requiredMemory = this.packVersion.memory;
            instanceLauncher.requiredPermGen = this.packVersion.permGen;
            instanceLauncher.assetsMapToResources = this.assetsMapToResources;
            instanceLauncher.multiMCManifest = multiMCManifest;
            instanceLauncher.modrinthProject = this.pack.modrinthProject;
            instanceLauncher.modrinthVersion = this.version._modrinthVersion;
            instanceLauncher.vanillaInstance = this.pack.vanillaInstance;

            if (multiMCManifest != null) {
                if (multiMCManifest.config.preLaunchCommand != null
                        && !multiMCManifest.config.preLaunchCommand.isEmpty()) {
                    instanceLauncher.preLaunchCommand = multiMCManifest.config.preLaunchCommand;
                    instanceLauncher.enableCommands = true;
                }

                if (multiMCManifest.config.postExitCommand != null
                        && !multiMCManifest.config.postExitCommand.isEmpty()) {
                    instanceLauncher.postExitCommand = multiMCManifest.config.postExitCommand;
                    instanceLauncher.enableCommands = true;
                }
                if (multiMCManifest.config.wrapperCommand != null && !multiMCManifest.config.wrapperCommand.isEmpty()) {
                    instanceLauncher.wrapperCommand = multiMCManifest.config.wrapperCommand;
                    instanceLauncher.enableCommands = true;
                }
            }

            if (this.version.isDev) {
                instanceLauncher.hash = this.version.hash;
            }
        }

        instance.launcher = instanceLauncher;

        instance.save();

        if (this.instance != null) {
            InstanceManager.getInstances().remove(this.instance);
        }

        InstanceManager.getInstances().add(instance);

        App.launcher.reloadInstancesPanel();

        this.resultInstance = instance;
    }

    private void initServerSettings() {
        new Thread(() -> {
            try {
                Path javaPath = Paths.get(OS.getJavaHome());
                if (minecraftVersion.javaVersion != null && App.settings.useJavaProvidedByMinecraft) {
                    Path runtimeDirectory = FileSystem.MINECRAFT_RUNTIMES
                            .resolve(minecraftVersion.javaVersion.component)
                            .resolve(JavaRuntimes.getSystem()).resolve(minecraftVersion.javaVersion.component);

                    if (OS.isMac()) {
                        runtimeDirectory = runtimeDirectory.resolve("jre.bundle/Contents/Home");
                    }

                    if (Files.isDirectory(runtimeDirectory)) {
                        javaPath = runtimeDirectory.toAbsolutePath();
                    }
                }

                String output = Utils.runProcess(root, Java.getPathToJavaExecutable(javaPath),
                        "-jar", getMinecraftJar().getAbsolutePath(), "--initSettings");
                LogManager.debug("initServerSettings output");
                LogManager.debug(output);
            } catch (Throwable ignored) {
            }
        }).start();
    }

    private void determineMainClass() {
        if (this.packVersion.mainClass != null) {
            if (this.packVersion.mainClass.depends == null && this.packVersion.mainClass.dependsGroup == null) {
                this.mainClass = this.packVersion.mainClass.mainClass;
            } else if (this.packVersion.mainClass.depends != null) {
                String depends = this.packVersion.mainClass.depends;

                if (this.selectedMods.stream().anyMatch(mod -> mod.name.equalsIgnoreCase(depends))) {
                    this.mainClass = this.packVersion.mainClass.mainClass;
                }
            } else if (this.packVersion.getMainClass().hasDependsGroup()) {
                String dependsGroup = this.packVersion.mainClass.dependsGroup;

                if (this.selectedMods.stream().anyMatch(mod -> mod.group.equalsIgnoreCase(dependsGroup))) {
                    this.mainClass = this.packVersion.mainClass.mainClass;
                }
            }
        }

        // use the loader provided main class if there is a loader
        if (this.loader != null) {
            this.mainClass = this.loader.getMainClass();
        }

        // if none set by pack, then use the minecraft one
        if (this.mainClass == null) {
            this.mainClass = this.minecraftVersion.mainClass;
        }
    }

    private void determineArguments() {
        this.arguments = new Arguments();

        if (this.loader != null) {
            if (this.loader.useMinecraftArguments()) {
                addMinecraftArguments();
            }

            Arguments loaderArguments = this.loader.getArguments();

            if (loaderArguments != null) {
                if (loaderArguments.game != null && loaderArguments.game.size() != 0) {
                    this.arguments.game.addAll(loaderArguments.game);
                }

                if (loaderArguments.jvm != null && loaderArguments.jvm.size() != 0) {
                    this.arguments.jvm.addAll(loaderArguments.jvm);
                }
            }
        } else {
            addMinecraftArguments();
        }

        if (this.packVersion.extraArguments != null) {
            boolean add = false;

            if (this.packVersion.extraArguments.depends == null
                    && this.packVersion.extraArguments.dependsGroup == null) {
                add = true;
            } else if (this.packVersion.extraArguments.depends == null) {
                String depends = this.packVersion.extraArguments.depends;

                if (this.selectedMods.stream().anyMatch(mod -> mod.name.equalsIgnoreCase(depends))) {
                    add = true;
                }
            } else if (this.packVersion.extraArguments.dependsGroup == null) {
                String dependsGroup = this.packVersion.extraArguments.dependsGroup;

                if (this.selectedMods.stream().anyMatch(mod -> mod.group.equalsIgnoreCase(dependsGroup))) {
                    add = true;
                }
            }

            if (add) {
                this.arguments.game.addAll(Arrays.stream(this.packVersion.extraArguments.arguments.split(" "))
                        .map(ArgumentRule::new).collect(Collectors.toList()));
            }
        }
    }

    private void addMinecraftArguments() {
        // older MC versions
        if (this.minecraftVersion.minecraftArguments != null) {
            this.arguments.game.addAll(Arrays.stream(this.minecraftVersion.minecraftArguments.split(" "))
                    .map(arg -> new ArgumentRule(null, arg)).collect(Collectors.toList()));
        }

        // newer MC versions
        if (this.minecraftVersion.arguments != null) {
            if (this.minecraftVersion.arguments.game != null && this.minecraftVersion.arguments.game.size() != 0) {
                this.arguments.game.addAll(this.minecraftVersion.arguments.game);
            }

            if (this.minecraftVersion.arguments.jvm != null && this.minecraftVersion.arguments.jvm.size() != 0) {
                this.arguments.jvm.addAll(this.minecraftVersion.arguments.jvm);
            }
        }
    }

    protected void downloadResources() throws Exception {
        addPercent(5);

        if (this.minecraftVersion.assetIndex == null) {
            return;
        }

        fireTask(GetText.tr("Organising Resources"));
        fireSubProgressUnknown();
        this.totalBytes = this.downloadedBytes = 0;

        MojangAssetIndex assetIndex = this.minecraftVersion.assetIndex;

        AssetIndex index = com.atlauncher.network.Download.build().setUrl(assetIndex.url).hash(assetIndex.sha1)
                .size(assetIndex.size).downloadTo(FileSystem.RESOURCES_INDEXES.resolve(assetIndex.id + ".json"))
                .asClass(AssetIndex.class);

        if (index.mapToResources) {
            this.assetsMapToResources = true;
        }

        OkHttpClient httpClient = Network.createProgressClient(this);
        DownloadPool pool = new DownloadPool();

        index.objects.forEach((key, object) -> {
            String filename = object.hash.substring(0, 2) + "/" + object.hash;
            String url = String.format("%s/%s", Constants.MINECRAFT_RESOURCES, filename);

            com.atlauncher.network.Download download = new com.atlauncher.network.Download().setUrl(url)
                    .downloadTo(FileSystem.RESOURCES_OBJECTS.resolve(filename)).hash(object.hash).size(object.size)
                    .withInstanceInstaller(this).withHttpClient(httpClient).withFriendlyFileName(key);

            pool.add(download);
        });

        DownloadPool smallPool = pool.downsize();

        if (smallPool.size() != 0) {
            fireTask(GetText.tr("Downloading Resources"));
            this.setTotalBytes(smallPool.totalSize());
            this.fireSubProgress(0);
            smallPool.downloadAll();
        }

        // copy resources to instance
        if (index.mapToResources || assetIndex.id.equalsIgnoreCase("legacy")) {
            fireTask(GetText.tr("Organising Resources"));
            fireSubProgressUnknown();

            index.objects.forEach((key, object) -> {
                String filename = object.hash.substring(0, 2) + "/" + object.hash;

                Path downloadedFile = FileSystem.RESOURCES_OBJECTS.resolve(filename);
                Path assetPath = index.mapToResources ? this.root.resolve("resources/" + key)
                        : FileSystem.RESOURCES_VIRTUAL_LEGACY.resolve(key);

                if (!Files.exists(assetPath)) {
                    FileUtils.copyFile(downloadedFile, assetPath, true);
                }
            });
        }

        hideSubProgressBar();
    }

    private void downloadMinecraft() throws Exception {
        addPercent(5);
        fireTask(GetText.tr("Downloading Minecraft"));
        fireSubProgressUnknown();
        totalBytes = 0;
        downloadedBytes = 0;

        MojangDownload mojangDownload = this.minecraftVersion.downloads.client;

        setTotalBytes(mojangDownload.size);

        com.atlauncher.network.Download.build().setUrl(mojangDownload.url).hash(mojangDownload.sha1)
                .size(mojangDownload.size).downloadTo(getMinecraftJarLibrary().toPath())
                .withInstanceInstaller(this)
                .withHttpClient(Network.createProgressClient(this)).downloadFile();

        hideSubProgressBar();
    }

    public File getMinecraftJar() {
        return new File(this.root.toFile(), String.format("%s.jar", this.minecraftVersion.id));
    }

    public File getMinecraftJarLibrary() {
        return getMinecraftJarLibrary("client");
    }

    public File getMinecraftJarLibrary(String type) {
        return FileSystem.LIBRARIES.resolve(getMinecraftJarLibraryPath(type)).toFile();
    }

    private void downloadLoggingClient() throws Exception {
        addPercent(5);

        if (this.minecraftVersion.logging == null) {
            return;
        }

        fireTask(GetText.tr("Downloading Logging Client"));
        fireSubProgressUnknown();

        LoggingFile loggingFile = this.minecraftVersion.logging.client.file;
        setTotalBytes(loggingFile.size);

        com.atlauncher.network.Download.build().setUrl(loggingFile.url).hash(loggingFile.sha1)
                .size(loggingFile.size).downloadTo(FileSystem.RESOURCES_LOG_CONFIGS.resolve(loggingFile.id))
                .withInstanceInstaller(this).withHttpClient(Network.createProgressClient(this)).downloadFile();

        hideSubProgressBar();
    }

    private List<Library> getLibraries() {
        List<Library> libraries = new ArrayList<>();

        List<Library> packVersionLibraries = getPackVersionLibraries();

        if (packVersionLibraries.size() != 0) {
            libraries.addAll(packVersionLibraries);
        }

        // Now read in the library jars needed from the loader
        if (this.loader != null) {
            libraries.addAll(this.loader.getLibraries());
        }

        // lastly the Minecraft libraries if not on server
        if (this.loader == null || this.loader.useMinecraftLibraries()) {
            libraries.addAll(this.minecraftVersion.libraries);
        }

        // fix Log4J exploits if not on server (servers seem sensitive to this process)
        libraries = libraries.stream().peek(Library::fixLog4jVersion).collect(Collectors.toList());

        return libraries;
    }

    public String getMinecraftJarLibraryPath() {
        return getMinecraftJarLibraryPath("client");
    }

    public String getMinecraftJarLibraryPath(String type) {
        return "net/minecraft/" + type + "/" + this.minecraftVersion.id + "/" + type + "-" + this.minecraftVersion.id
                + ".jar".replace("/", File.separatorChar + "");
    }

    public List<String> getLibrariesForLaunch() {
        List<String> libraries = new ArrayList<>();

        libraries.add(this.getMinecraftJarLibraryPath());

        libraries.addAll(this.getLibraries().stream()
                .filter(library -> library.downloads.artifact != null && library.downloads.artifact.path != null)
                .map(library -> library.downloads.artifact.path).collect(Collectors.toList()));

        return libraries;
    }

    public String getMinecraftArguments() {
        return this.arguments.asString();
    }

    public boolean doAssetsMapToResources() {
        return this.assetsMapToResources;
    }

    private List<Library> getPackVersionLibraries() {
        List<Library> libraries = new ArrayList<>();

        // Now read in the library jars needed from the pack
        for (com.atlauncher.data.json.Library library : this.packVersion.getLibraries()) {
            if (library.depends != null) {
                if (this.selectedMods.stream().noneMatch(mod -> mod.name.equalsIgnoreCase(library.depends))) {
                    continue;
                }
            } else if (library.hasDependsGroup()) {
                if (this.selectedMods.stream().noneMatch(mod -> mod.group.equalsIgnoreCase(library.dependsGroup))) {
                    continue;
                }
            }

            Library minecraftLibrary = new Library();

            minecraftLibrary.name = library.file;

            Download download = new Download();
            download.path = library.path != null ? library.path
                    : (library.server != null ? library.server : library.file);
            download.sha1 = library.md5;
            download.size = library.filesize;
            download.url = String.format("%s/%s", Constants.DOWNLOAD_SERVER, library.url);

            Downloads downloads = new Downloads();
            downloads.artifact = download;

            minecraftLibrary.downloads = downloads;

            libraries.add(minecraftLibrary);
        }

        return libraries;
    }

    private void downloadLibraries() {
        addPercent(5);
        fireTask(GetText.tr("Downloading Libraries"));
        fireSubProgressUnknown();

        OkHttpClient httpClient = Network.createProgressClient(this);
        DownloadPool pool = new DownloadPool();

        // get non native libraries otherwise we double up
        this.getLibraries().stream().filter(
                library -> library.shouldInstall() && library.downloads.artifact != null && !library.hasNativeForOS())
                .forEach(library -> {
                    com.atlauncher.network.Download download = new com.atlauncher.network.Download()
                            .setUrl(library.downloads.artifact.url)
                            .downloadTo(FileSystem.LIBRARIES.resolve(library.downloads.artifact.path))
                            .hash(library.downloads.artifact.sha1).size(library.downloads.artifact.size)
                            .withInstanceInstaller(this).withHttpClient(httpClient);

                    pool.add(download);
                });

        if (this.loader != null && this.loader.getInstallLibraries() != null) {
            this.loader.getInstallLibraries().stream().filter(library -> library.downloads.artifact != null).forEach(
                    library -> pool.add(new com.atlauncher.network.Download().setUrl(library.downloads.artifact.url)
                            .downloadTo(FileSystem.LIBRARIES.resolve(library.downloads.artifact.path))
                            .hash(library.downloads.artifact.sha1).size(library.downloads.artifact.size)
                            .withInstanceInstaller(this).withHttpClient(httpClient)));
        }

        this.getLibraries().stream().filter(Library::hasNativeForOS).forEach(library -> {
            Download download = library.getNativeDownloadForOS();

            pool.add(new com.atlauncher.network.Download().setUrl(download.url)
                .downloadTo(FileSystem.LIBRARIES.resolve(download.path)).hash(download.sha1).size(download.size)
                .withInstanceInstaller(this).withHttpClient(httpClient));
        });

        DownloadPool smallPool = pool.downsize();

        this.setTotalBytes(smallPool.totalSize());
        this.fireSubProgress(0);

        smallPool.downloadAll();

        hideSubProgressBar();
    }

    private void organiseLibraries() {
        addPercent(5);
        fireTask(GetText.tr("Organising Libraries"));
        fireSubProgressUnknown();

        hideSubProgressBar();
    }

    private void downloadRuntime() {
        addPercent(5);

        if (minecraftVersion.javaVersion == null || Data.JAVA_RUNTIMES == null
                || !App.settings.useJavaProvidedByMinecraft) {
            return;
        }

        Map<String, List<JavaRuntime>> runtimesForSystem = Data.JAVA_RUNTIMES.getForSystem();
        String runtimeSystemString = JavaRuntimes.getSystem();

        if (runtimesForSystem.containsKey(minecraftVersion.javaVersion.component)
                && runtimesForSystem.get(minecraftVersion.javaVersion.component).size() != 0) {
            fireTask(GetText.tr("Downloading Java Runtime {0}", minecraftVersion.javaVersion.majorVersion));
            fireSubProgressUnknown();

            JavaRuntime runtimeToDownload = runtimesForSystem.get(minecraftVersion.javaVersion.component).get(0);

            try {
                JavaRuntimeManifest javaRuntimeManifest = com.atlauncher.network.Download.build()
                        .setUrl(runtimeToDownload.manifest.url).size(runtimeToDownload.manifest.size)
                        .hash(runtimeToDownload.manifest.sha1).downloadTo(FileSystem.MINECRAFT_RUNTIMES
                                .resolve(minecraftVersion.javaVersion.component).resolve("manifest.json"))
                        .asClassWithThrow(JavaRuntimeManifest.class);

                OkHttpClient httpClient = Network.createProgressClient(this);
                DownloadPool pool = new DownloadPool();

                // create root directory
                Path runtimeSystemDirectory = FileSystem.MINECRAFT_RUNTIMES
                        .resolve(minecraftVersion.javaVersion.component).resolve(runtimeSystemString);
                Path runtimeDirectory = runtimeSystemDirectory.resolve(minecraftVersion.javaVersion.component);
                FileUtils.createDirectory(runtimeDirectory);

                // create all the directories
                javaRuntimeManifest.files.forEach((key, file) -> {
                    if (file.type == JavaRuntimeManifestFileType.DIRECTORY) {
                        FileUtils.createDirectory(runtimeDirectory.resolve(key));
                    }
                });

                // collect the files we need to download
                javaRuntimeManifest.files.forEach((key, file) -> {
                    if (file.type == JavaRuntimeManifestFileType.FILE) {
                        com.atlauncher.network.Download download = new com.atlauncher.network.Download()
                                .setUrl(file.downloads.raw.url).downloadTo(runtimeDirectory.resolve(key))
                                .hash(file.downloads.raw.sha1).size(file.downloads.raw.size).executable(file.executable)
                                .withInstanceInstaller(this).withHttpClient(httpClient);

                        pool.add(download);
                    }
                });

                DownloadPool smallPool = pool.downsize();

                this.setTotalBytes(smallPool.totalSize());
                this.fireSubProgress(0);

                smallPool.downloadAll();

                // write out the version file (theres also a .sha1 file created, but we're not
                // doing that)
                Files.write(runtimeSystemDirectory.resolve(".version"),
                        runtimeToDownload.version.name.getBytes(StandardCharsets.UTF_8));
                // Files.write(runtimeSystemDirectory.resolve(minecraftVersion.javaVersion.component
                // + ".sha1"), runtimeToDownload.version.name.getBytes(StandardCharsets.UTF_8));

                hideSubProgressBar();
            } catch (IOException e) {
                LogManager.logStackTrace("Failed to download Java runtime", e);
            }
        }
    }

    private void installLoader() {
        addPercent(5);

        if (this.loader == null) {
            return;
        }

        fireTask(GetText.tr("Installing Loader (May Take Some Time)"));
        fireSubProgressUnknown();

        hideSubProgressBar();
    }

    private void downloadMods() throws Exception {
        addPercent(25);

        if (multiMCManifest != null || selectedMods.size() == 0) {
            return;
        }

        fireTask(GetText.tr("Downloading Mods"));
        fireSubProgressUnknown();

        OkHttpClient httpClient = Network.createProgressClient(this);
        DownloadPool pool = new DownloadPool();

        this.selectedMods.stream().filter(mod -> mod.download != DownloadType.browser).forEach(mod -> {
            com.atlauncher.network.Download download = new com.atlauncher.network.Download()
                    .setUrl(mod.getDownloadUrl()).downloadTo(FileSystem.DOWNLOADS.resolve(mod.getFile()))
                    .size(mod.filesize).withInstanceInstaller(this).withHttpClient(httpClient);

            if (mod.ignoreFailures) {
                download = download.ignoreFailures();
            }

            if (mod.md5 != null) {
                download = download.hash(mod.md5);
            } else if (mod.sha1 != null) {
                download = download.hash(mod.sha1);
            } else if (mod.sha512 != null) {
                download = download.hash(mod.sha512);
            } else if (mod.fingerprint != null) {
                download = download.fingerprint(mod.fingerprint);
            }

            pool.add(download);
        });

        DownloadPool smallPool = pool.downsize();

        this.setTotalBytes(smallPool.totalSize());
        this.fireSubProgress(0);

        smallPool.downloadAll();

        fireSubProgressUnknown();

        List<Mod> browserDownloadMods = this.selectedMods.stream().filter(mod -> mod.download == DownloadType.browser)
                .collect(Collectors.toList());
        if (browserDownloadMods.size() != 0) {
            int browserDownloadModsDownloaded = 0;

            for (Mod mod : browserDownloadMods) {
                if (!isCancelled()) {
                    fireTask(GetText.tr("Downloading Browser Mods"));

                    if (!mod.download(this)) {
                        LogManager.info("Browser download mod " + mod.name + " was skipped");
                        Optional<DisableableMod> disableableMod = this.modsInstalled.stream()
                                .filter(m -> Objects.equals(m.file, mod.file)).findFirst();
                        disableableMod.ifPresent(value -> value.skipped = true);
                    }

                    browserDownloadModsDownloaded++;

                    fireSubProgress((browserDownloadModsDownloaded / (browserDownloadMods.size() * 1.0)) * 100.0,
                            String.format("%d/%d", browserDownloadModsDownloaded,
                                    browserDownloadMods.size()));
                }
            }
        }

        hideSubProgressBar();
    }

    private void installMods() {
        addPercent(25);

        if (multiMCManifest != null || this.selectedMods.size() == 0) {
            return;
        }

        fireTask(GetText.tr("Installing Mods"));
        fireSubProgressUnknown();

        double subPercentPerMod = 100.0 / this.selectedMods.size();

        this.selectedMods.parallelStream().forEach(mod -> {
            mod.install(this);
            addSubPercent(subPercentPerMod);
        });

        hideSubProgressBar();
    }

    private void installLegacyJavaFixer() {
        addPercent(5);

        if (this.allMods.size() == 0 || !Utils.matchVersion(minecraftVersion.id, "1.6", true, true)) {
            return;
        }

        // #. {0} is the name of a mod we're installing
        fireTask(GetText.tr("Installing {0}", "Legacy Java Fixer"));
        fireSubProgressUnknown();

        com.atlauncher.network.Download download = com.atlauncher.network.Download.build()
                .setUrl(Constants.LEGACY_JAVA_FIXER_URL).hash(Constants.LEGACY_JAVA_FIXER_MD5)
                .downloadTo(FileSystem.DOWNLOADS.resolve("legacyjavafixer-1.0.jar"))
                .copyTo(root.resolve("mods/legacyjavafixer-1.0.jar"));

        if (download.needToDownload()) {
            try {
                download.downloadFile();
            } catch (IOException e) {
                LogManager.logStackTrace("Failed to download Legacy Java Fixer", e);
            }
        } else {
            download.copy();
        }

        DisableableMod mod = new DisableableMod();
        mod.disabled = false;
        mod.userAdded = false;
        mod.wasSelected = true;
        mod.skipped = false;
        mod.file = "legacyjavafixer-1.0.jar";
        mod.type = Type.mods;
        mod.optional = false;
        mod.name = "Legacy Java Fixer";
        mod.version = "1.0";
        mod.description = "Fixes issues with newer Java versions on Minecraft 1.6 and below";

        this.modsInstalled.add(mod);

        hideSubProgressBar();
    }

    private void runCaseConversion() throws Exception {
        addPercent(5);

        if (this.packVersion.caseAllFiles == null) {
            return;
        }

        Files.walkFileTree(this.root.resolve("mods"), new CaseFileVisitor(this.packVersion.caseAllFiles,
                this.selectedMods.stream().filter(m -> m.type == ModType.mods).collect(Collectors.toList())));
    }

    private void runActions() {
        addPercent(5);

        if (this.packVersion.actions == null || this.packVersion.actions.size() == 0) {
            return;
        }

        for (com.atlauncher.data.json.Action action : this.packVersion.actions) {
            action.execute(this);
        }
    }

    private void installConfigs() throws Exception {
        addPercent(5);

        if (this.packVersion.noConfigs) {
            return;
        }

        if (multiMCManifest != null) {
            fireSubProgressUnknown();
            String minecraftFolder = Files.exists(multiMCExtractedPath.resolve(".minecraft")) ? ".minecraft"
                    : "minecraft";

            fireTask(GetText.tr("Copying minecraft folder"));
            Utils.copyDirectory(this.multiMCExtractedPath.resolve(minecraftFolder + "/").toFile(), this.root.toFile(),
                    false);
        } else if (!pack.vanillaInstance) {
            fireTask(GetText.tr("Downloading Configs"));

            File configs = this.temp.resolve("Configs.zip").toFile();
            String path = "packs/" + pack.getSafeName() + "/versions/" + version.version + "/Configs.zip";

            com.atlauncher.network.Download configsDownload = com.atlauncher.network.Download.build()
                    .setUrl(String.format("%s/%s", Constants.DOWNLOAD_SERVER, path)).downloadTo(configs.toPath())
                    .size(this.packVersion.configs.filesize).hash(this.packVersion.configs.sha1)
                    .withInstanceInstaller(this).withHttpClient(Network.createProgressClient(this));

            this.setTotalBytes(configsDownload.getFilesize());
            configsDownload.downloadFile();

            if (!configs.exists()) {
                throw new Exception("Failed to download configs for pack!");
            }

            // file is empty, so don't try to extract
            if (configs.length() == 0L) {
                return;
            }

            fireSubProgressUnknown();
            fireTask(GetText.tr("Extracting Configs"));

            ArchiveUtils.extract(configs.toPath(), this.root);
            Utils.delete(configs);
        }
    }

    private void downloadInstanceImage() throws Exception {
        addPercent(5);

        if (this.pack.modrinthProject != null) {
            fireTask(GetText.tr("Downloading Instance Image"));
            if (this.pack.modrinthProject.iconUrl != null) {
                com.atlauncher.network.Download imageDownload = com.atlauncher.network.Download.build()
                        .setUrl(this.pack.modrinthProject.iconUrl).downloadTo(root.resolve("instance.png"))
                        .withInstanceInstaller(this).ignoreFailures()
                        .withHttpClient(Network.createProgressClient(this));

                this.setTotalBytes(imageDownload.getFilesize());
                imageDownload.downloadFile();
            }
        }
    }

    private void checkModsOnModrinth() {
        if (App.settings.dontCheckModsOnModrinth || this.modsInstalled.size() == 0) {
            return;
        }

        // #. {0} is the platform we're checking mods on (e.g. CurseForge/Modrinth)
        fireTask(GetText.tr("Checking Mods On {0}", "Modrinth"));
        fireSubProgressUnknown();

        Map<String, DisableableMod> sha1Hashes = new HashMap<>();

        this.modsInstalled.stream().filter(dm -> dm.modrinthProject == null && dm.modrinthVersion == null)
                .filter(dm -> dm.getFile(root, this.packVersion.minecraft) != null).forEach(dm -> {
                    try {
                        sha1Hashes.put(Hashing.sha1(dm.getFile(root, this.packVersion.minecraft).toPath()).toString(),
                                dm);
                    } catch (Throwable t) {
                        LogManager.logStackTrace(t);
                    }
                });

        if (sha1Hashes.size() != 0) {
            Set<String> keys = sha1Hashes.keySet();
            Map<String, ModrinthVersion> modrinthVersions = ModrinthApi
                    .getVersionsFromSha1Hashes(keys.toArray(new String[keys.size()]));

            if (modrinthVersions != null && modrinthVersions.size() != 0) {
                String[] projectIdsFound = modrinthVersions.values().stream().map(mv -> mv.projectId)
                        .toArray(String[]::new);

                if (projectIdsFound.length != 0) {
                    Map<String, ModrinthProject> foundProjects = ModrinthApi.getProjectsAsMap(projectIdsFound);

                    if (foundProjects != null) {
                        for (Map.Entry<String, ModrinthVersion> entry : modrinthVersions.entrySet()) {
                            ModrinthVersion version = entry.getValue();
                            ModrinthProject project = foundProjects.get(version.projectId);

                            if (project != null) {
                                DisableableMod dm = sha1Hashes.get(entry.getKey());

                                // add Modrinth information
                                dm.modrinthProject = project;
                                dm.modrinthVersion = version;
                                dm.name = project.title;
                                dm.description = project.description;

                                LogManager
                                        .debug(String.format("Found matching mod from Modrinth called %s with file %s",
                                                project.title, version.name));
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Mod> sortMods(List<Mod> original) {
        List<Mod> mods = new ArrayList<>(original);

        for (Mod mod : original) {
            if (mod.isOptional()) {
                if (mod.hasLinked()) {
                    for (Mod mod1 : original) {
                        if (mod1.getName().equalsIgnoreCase(mod.getLinked())) {
                            mods.remove(mod);
                            int index = mods.indexOf(mod1) + 1;
                            mods.add(index, mod);
                        }
                    }

                }
            }
        }

        List<Mod> modss = new ArrayList<>();

        for (Mod mod : mods) {
            if (!mod.isOptional()) {
                modss.add(mod); // Add all non optional mods
            }
        }

        for (Mod mod : mods) {
            if (!modss.contains(mod)) {
                modss.add(mod); // Add the rest
            }
        }

        return modss;
    }

    private void backupSelectFiles() {
        File reis = new File(this.root.resolve("mods").toFile(), "rei_minimap");
        if (reis.exists() && reis.isDirectory()) {
            if (Utils.copyDirectory(reis, this.temp.toFile(), true)) {
                savedReis = true;
            }
        }

        File zans = new File(this.root.resolve("mods").toFile(), "VoxelMods");
        if (zans.exists() && zans.isDirectory()) {
            if (Utils.copyDirectory(zans, this.temp.toFile(), true)) {
                savedZans = true;
            }
        }

        File neiCfg = new File(this.root.resolve("config").toFile(), "NEI.cfg");
        if (neiCfg.exists() && neiCfg.isFile()) {
            if (Utils.copyFile(neiCfg, this.temp.toFile())) {
                savedNEICfg = true;
            }
        }

        File optionsTXT = new File(this.root.toFile(), "options.txt");
        if (optionsTXT.exists() && optionsTXT.isFile()) {
            if (Utils.copyFile(optionsTXT, this.temp.toFile())) {
                savedOptionsTxt = true;
            }
        }

        File serversDAT = new File(this.root.toFile(), "servers.dat");
        if (serversDAT.exists() && serversDAT.isFile()) {
            if (Utils.copyFile(serversDAT, this.temp.toFile())) {
                savedServersDat = true;
            }
        }

        File portalGunSounds = new File(this.root.resolve("mods").toFile(), "PortalGunSounds.pak");
        if (portalGunSounds.exists() && portalGunSounds.isFile()) {
            savedPortalGunSounds = true;
            Utils.copyFile(portalGunSounds, this.temp.toFile());
        }

        if (isReinstall && this.packVersion.keeps != null) {
            Keeps keeps = this.packVersion.keeps;

            if (keeps.hasFileKeeps()) {
                for (Keep keep : keeps.getFiles()) {
                    if (keep.isAllowed()) {
                        File file = keep.getFile(
                                keep.getBase().equalsIgnoreCase("config") ? this.root.resolve("config").toFile()
                                        : this.root.toFile());
                        if (file.exists()) {
                            Utils.copyFile(file, this.temp.toFile());
                        }
                    }
                }
            }

            if (keeps.hasFolderKeeps()) {
                for (Keep keep : keeps.getFolders()) {
                    if (keep.isAllowed()) {
                        File file = keep.getFile(
                                keep.getBase().equalsIgnoreCase("config") ? this.root.resolve("config").toFile()
                                        : this.root.toFile());
                        if (file.exists() && file.isDirectory()) {
                            Utils.copyDirectory(file, this.temp.toFile(), true);
                        }
                    }
                }
            }
        }
    }

    protected void prepareFilesystem() throws Exception {
        if (isReinstall) {
            if (Files.isDirectory(this.root.resolve("bin"))) {
                FileUtils.deleteDirectory(this.root.resolve("bin"));
            }

            if (Files.isDirectory(this.root.resolve("config")) && (instance == null
                    || !instance.launcher.vanillaInstance)) {
                FileUtils.deleteDirectory(this.root.resolve("config"));
            }

            if (isReinstall) {
                if (Files.isDirectory(this.root.resolve("mods"))) {
                    Utils.deleteWithFilter(this.root.resolve("mods").toFile(),
                            instance.getPackMods(com.atlauncher.data.Type.mods), true);
                }

                if (Files.isDirectory(this.root.resolve("coremods"))) {
                    Utils.deleteWithFilter(this.root.resolve("coremods").toFile(),
                            instance.getPackMods(com.atlauncher.data.Type.coremods), true);
                }

                if (Files.isDirectory(this.root.resolve("jarmods"))) {
                    Utils.deleteWithFilter(this.root.resolve("jarmods").toFile(),
                            instance.getPackMods(com.atlauncher.data.Type.jar), true);

                    Utils.deleteWithFilter(this.root.resolve("jarmods").toFile(),
                            instance.getPackMods(com.atlauncher.data.Type.forge), true);
                }
            } else {
                if (Files.isDirectory(this.root.resolve("mods"))) {
                    FileUtils.deleteDirectory(this.root.resolve("mods"));
                }

                if (isReinstall && Files.isDirectory(this.root.resolve("jarmods"))) {
                    FileUtils.deleteDirectory(this.root.resolve("jarmods"));
                }
            }

            if (isReinstall) {
                if (Files.exists(this.root.resolve("texturepacks/TexturePack.zip"))) {
                    FileUtils.delete(this.root.resolve("texturepacks/TexturePack.zip"));
                }

                if (Files.exists(this.root.resolve("resourcepacks/ResourcePack.zip"))) {
                    FileUtils.delete(this.root.resolve("resourcepacks/ResourcePack.zip"));
                }
            } else {
                if (Files.isDirectory(this.root.resolve("libraries"))) {
                    FileUtils.deleteDirectory(this.root.resolve("libraries"));
                }
            }

            if (isReinstall && this.packVersion.deletes != null) {
                Deletes deletes = this.packVersion.deletes;

                if (deletes.hasFileDeletes()) {
                    for (Delete delete : deletes.getFiles()) {
                        if (delete.isAllowed()) {
                            File file = delete.getFile(this.root.toFile());
                            if (file.exists()) {
                                Utils.delete(file);
                            }
                        }
                    }
                }

                if (deletes.hasFolderDeletes()) {
                    for (Delete delete : deletes.getFolders()) {
                        if (delete.isAllowed()) {
                            File file = delete.getFile(this.root.toFile());
                            if (file.exists()) {
                                Utils.delete(file);
                            }
                        }
                    }
                }
            }
        }

        // make some new directories
        Path[] directories;
        directories = new Path[] { this.root, this.root.resolve("mods"), this.root.resolve("disabledmods"),
            this.temp, this.temp.resolve("loader"), this.root.resolve("jarmods") };

        for (Path directory : directories) {
            if (!Files.exists(directory)) {
                FileUtils.createDirectory(directory);
            }
        }
    }

    private void restoreSelectFiles() {
        if (savedReis) {
            Utils.copyDirectory(new File(this.temp.toFile(), "rei_minimap"),
                    new File(this.root.resolve("mods").toFile(), "rei_minimap"));
        }

        if (savedZans) {
            Utils.copyDirectory(new File(this.temp.toFile(), "VoxelMods"),
                    new File(this.root.resolve("mods").toFile(), "VoxelMods"));
        }

        if (savedNEICfg) {
            Utils.copyFile(new File(this.temp.toFile(), "NEI.cfg"),
                    new File(this.root.resolve("config").toFile(), "NEI.cfg"), true);
        }

        if (savedOptionsTxt) {
            Utils.copyFile(new File(this.temp.toFile(), "options.txt"), new File(this.root.toFile(), "options.txt"),
                    true);
        }

        if (savedServersDat) {
            Utils.copyFile(new File(this.temp.toFile(), "servers.dat"), new File(this.root.toFile(), "servers.dat"),
                    true);
        }

        if (savedPortalGunSounds) {
            Utils.copyFile(new File(this.temp.toFile(), "PortalGunSounds.pak"),
                    new File(this.root.resolve("mods").toFile(), "PortalGunSounds.pak"), true);
        }

        if (isReinstall && this.packVersion.keeps != null) {
            Keeps keeps = this.packVersion.keeps;

            if (keeps.hasFileKeeps()) {
                for (Keep keep : keeps.getFiles()) {
                    if (keep.isAllowed()) {
                        File from = keep.getFile(this.temp.toFile());
                        File to = keep.getFile(
                                keep.getBase().equalsIgnoreCase("config") ? this.root.resolve("config").toFile()
                                        : this.root.toFile());
                        if (from.exists()) {
                            to.getParentFile().mkdirs();
                            Utils.copyFile(from, to, true);
                        }
                    }
                }
            }

            if (keeps.hasFolderKeeps()) {
                for (Keep keep : keeps.getFolders()) {
                    if (keep.isAllowed()) {
                        File from = keep.getFile(this.temp.toFile());
                        File to = keep.getFile(
                                keep.getBase().equalsIgnoreCase("config") ? this.root.resolve("config").toFile()
                                        : this.root.toFile());
                        if (from.exists()) {
                            to.getParentFile().mkdirs();
                            Utils.copyDirectory(from, to);
                        }
                    }
                }
            }
        }
    }

    private void writeLog4ShellExploitArgumentsForForgeScripts() throws Exception {
        if (this.loaderVersion != null && this.loaderVersion.shouldInstallServerScripts()) {
            return;
        }

        if (Files.exists(root.resolve("user_jvm_args.txt")) && minecraftVersionManifest.isLog4ShellExploitable()) {
            Files.write(root.resolve("user_jvm_args.txt"),
                    (System.lineSeparator() + this.getLog4ShellArguments()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        }
    }

    private String getLog4ShellArguments() throws Exception {
        if (minecraftVersionManifest.isLog4ShellExploitable()) {
            return "-Dlog4j2.formatMsgNoLookups=true -Dlog4j.configurationFile=log4j2.xml";
        }

        return "";
    }

    public boolean hasRecommendedMods() {
        for (Mod mod : allMods) {
            if (mod.isRecommended()) {
                return true; // One of the mods is marked as recommended, so return true
            }
        }
        return false; // No non recommended mods found
    }

    public boolean isOnlyRecommendedInGroup(Mod mod) {
        for (Mod modd : allMods) {
            if (modd == mod || !modd.hasGroup()) {
                continue;
            }
            if (modd.getGroup().equalsIgnoreCase(mod.getGroup()) && modd.isRecommended()) {
                return false; // Another mod is recommended. Don't check anything
            }
        }
        return true; // No other recommended mods found in the group
    }

    public Mod getModByName(String name) {
        for (Mod mod : allMods) {
            if (mod.getName().equalsIgnoreCase(name)) {
                return mod;
            }
        }
        return null;
    }

    public List<Mod> getLinkedMods(Mod mod) {
        List<Mod> linkedMods = new ArrayList<>();
        for (Mod modd : allMods) {
            if (!modd.hasLinked()) {
                continue;
            }
            if (modd.getLinked().equalsIgnoreCase(mod.getName())) {
                linkedMods.add(modd);
            }
        }
        return linkedMods;
    }

    public List<Mod> getGroupedMods(Mod mod) {
        List<Mod> groupedMods = new ArrayList<>();
        for (Mod modd : allMods) {
            if (!modd.hasGroup()) {
                continue;
            }
            if (modd.getGroup().equalsIgnoreCase(mod.getGroup())) {
                if (modd != mod) {
                    groupedMods.add(modd);
                }
            }
        }
        return groupedMods;
    }

    public List<Mod> getModsDependancies(Mod mod) {
        List<Mod> dependsMods = new ArrayList<>();
        for (String name : mod.getDepends()) {
            inner: {
                for (Mod modd : allMods) {
                    if (modd.getName().equalsIgnoreCase(name)) {
                        dependsMods.add(modd);
                        break inner;
                    }
                }
            }
        }
        return dependsMods;
    }

    public List<Mod> dependedMods(Mod mod) {
        List<Mod> dependedMods = new ArrayList<>();
        for (Mod modd : allMods) {
            if (!modd.hasDepends()) {
                continue;
            }
            if (modd.isADependancy(mod)) {
                dependedMods.add(modd);
            }
        }
        return dependedMods;
    }

    public boolean hasADependancy(Mod mod) {
        for (Mod modd : allMods) {
            if (!modd.hasDepends()) {
                continue;
            }
            if (modd.isADependancy(mod)) {
                return true;
            }
        }
        return false;
    }

    public boolean wasModInstalled(String mod) {
        return instance.wasModInstalled(mod);
    }

    public boolean wasModSelected(String mod) {
        return instance.wasModSelected(mod);
    }

    public void fireTask(String name) {
        LogManager.debug("Instance Installer: " + name);
        firePropertyChange("doing", null, name);
    }

    protected void fireProgress(double percent) {
        if (percent > 100.0) {
            percent = 100.0;
        }
        firePropertyChange("progress", null, percent);
    }

    protected void fireSubProgress(double percent) {
        if (percent > 100.0) {
            percent = 100.0;
        }
        firePropertyChange("subprogress", null, percent);
    }

    protected void fireSubProgress(double percent, String paint) {
        if (percent > 100.0) {
            percent = 100.0;
        }
        String[] info = new String[2];
        info[0] = "" + percent;
        info[1] = paint;
        firePropertyChange("subprogress", null, info);
    }

    public void fireSubProgressUnknown() {
        firePropertyChange("subprogressint", null, null);
    }

    protected void addPercent(double percent) {
        this.percent = this.percent + percent;
        if (this.percent > 100.0) {
            this.percent = 100.0;
        }
        fireProgress(this.percent);
    }

    public void setSubPercent(double percent) {
        this.subPercent = percent;
        if (this.subPercent > 100.0) {
            this.subPercent = 100.0;
        }
        fireSubProgress(this.subPercent);
    }

    public void addSubPercent(double percent) {
        this.subPercent = this.subPercent + percent;
        if (this.subPercent > 100.0) {
            this.subPercent = 100.0;
        }

        if (this.subPercent > 100.0) {
            this.subPercent = 100.0;
        }
        fireSubProgress(this.subPercent);
    }

    @Override
    public void setTotalBytes(long bytes) {
        this.downloadedBytes = 0L;
        this.totalBytes = bytes;
        this.updateProgressBar();
    }

    @Override
    public void addDownloadedBytes(long bytes) {
        this.downloadedBytes += bytes;
        this.updateProgressBar();
    }

    @Override
    public void addBytesToDownload(long bytes) {
        this.totalBytes += bytes;
        this.updateProgressBar();
    }

    private void updateProgressBar() {
        double progress;
        if (this.totalBytes > 0) {
            progress = (this.downloadedBytes / this.totalBytes) * 100.0;
        } else {
            progress = 0.0;
        }
        double done = this.downloadedBytes / 1024.0 / 1024.0;
        double toDo = this.totalBytes / 1024.0 / 1024.0;
        if (done > toDo) {
            fireSubProgress(100.0, String.format("%.2f MB", done));
        } else {
            fireSubProgress(progress, String.format("%.2f MB / %.2f MB", done, toDo));
        }
    }

    private void hideSubProgressBar() {
        fireSubProgress(-1);
    }
}
