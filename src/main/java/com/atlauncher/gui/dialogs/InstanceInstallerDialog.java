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
package com.atlauncher.gui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.Gsons;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.constants.UIConstants;
import com.atlauncher.data.Instance;
import com.atlauncher.data.Pack;
import com.atlauncher.data.PackVersion;
import com.atlauncher.data.installables.ATLauncherInstallable;
import com.atlauncher.data.installables.Installable;
import com.atlauncher.data.installables.ModrinthInstallable;
import com.atlauncher.data.installables.ModrinthManifestInstallable;
import com.atlauncher.data.installables.MultiMCInstallable;
import com.atlauncher.data.installables.VanillaInstallable;
import com.atlauncher.data.json.Version;
import com.atlauncher.data.minecraft.VersionManifestVersion;
import com.atlauncher.data.minecraft.VersionManifestVersionType;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;
import com.atlauncher.data.minecraft.loaders.fabric.FabricLoader;
import com.atlauncher.data.minecraft.loaders.legacyfabric.LegacyFabricLoader;
import com.atlauncher.data.minecraft.loaders.quilt.QuiltLoader;
import com.atlauncher.data.modrinth.ModrinthProject;
import com.atlauncher.data.modrinth.ModrinthSearchHit;
import com.atlauncher.data.modrinth.ModrinthVersion;
import com.atlauncher.data.multimc.MultiMCComponent;
import com.atlauncher.data.multimc.MultiMCManifest;
import com.atlauncher.exceptions.InvalidMinecraftVersion;
import com.atlauncher.gui.components.JLabelWithHover;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.MinecraftManager;
import com.atlauncher.utils.ComboItem;
import com.atlauncher.utils.ModrinthApi;
import com.atlauncher.utils.Utils;
import com.atlauncher.utils.WindowUtils;

public class InstanceInstallerDialog extends JDialog {
    private static final long serialVersionUID = -6984886874482721558L;
    private int versionLength = 0;
    private int loaderVersionLength = 0;
    private boolean isReinstall = false;
    private Pack pack;
    private Instance instance = null;
    private ModrinthProject modrinthProject = null;
    private ModrinthVersion preselectedModrinthVersion = null;
    private MultiMCManifest multiMCManifest = null;

    private final JPanel middle;
    private final JButton install;
    private final JTextField nameField;
    private JComboBox<PackVersion> versionsDropDown;
    private final JLabel loaderVersionLabel = new JLabel();
    private final JComboBox<ComboItem<LoaderVersion>> loaderVersionsDropDown = new JComboBox<>();
    private final List<LoaderVersion> loaderVersions = new ArrayList<>();

    private final JLabel showAllMinecraftVersionsLabel = new JLabel(GetText.tr("Show All"));
    private final JCheckBox showAllMinecraftVersionsCheckbox = new JCheckBox();

    private JLabel saveModsLabel;
    private JCheckBox saveModsCheckbox;
    private final boolean isUpdate;
    private final PackVersion autoInstallVersion;
    private final Path extractedPath;

    public InstanceInstallerDialog(MultiMCManifest manifest, Path multiMCExtractedPath) {
        this(manifest, false, false, null, null, false, multiMCExtractedPath, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object) {
        this(object, false, false, null, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(ModrinthProject modrinthProject, ModrinthVersion preselectedModrinthVersion) {
        this(modrinthProject, false, false, null, null, true, null, App.launcher.getParent(),
                preselectedModrinthVersion);
    }

    public InstanceInstallerDialog(Object object, boolean isServer) {
        this(object, false, isServer, null, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Window parent, Object object) {
        this(object, false, false, null, null, true, null, parent, null);
    }

    public InstanceInstallerDialog(Pack pack, PackVersion version, String shareCode, boolean showModsChooser) {
        this(pack, false, false, version, shareCode, showModsChooser, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Pack pack, boolean isServer) {
        this(pack, false, true, null, null, true, null, App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object, boolean isUpdate, boolean isServer, PackVersion autoInstallVersion,
            String shareCode, boolean showModsChooser, Path extractedPath) {
        this(object, isUpdate, isServer, autoInstallVersion, shareCode, showModsChooser, extractedPath,
                App.launcher.getParent(), null);
    }

    public InstanceInstallerDialog(Object object, final boolean isUpdate, final boolean isServer,
            final PackVersion autoInstallVersion, final String shareCode, final boolean showModsChooser,
            Path extractedPathCon, Window parent, ModrinthVersion preselectedModrinthVersion) {
        super(parent, ModalityType.DOCUMENT_MODAL);

        setName("instanceInstallerDialog");
        this.isUpdate = isUpdate;
        this.autoInstallVersion = autoInstallVersion;
        this.extractedPath = extractedPathCon;
        this.preselectedModrinthVersion = preselectedModrinthVersion;

        if (object instanceof Pack) {
            handlePackInstall(object);
        } else if (object instanceof ModrinthSearchHit || object instanceof ModrinthProject) {
            handleModrinthInstall(object);
        } else if (object instanceof MultiMCManifest) {
            handleMultiMcImport(object);
        } else {
            handleInstanceInstall(object);
        }

        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        setResizable(false);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        install = new JButton(
                ((isReinstall) ? (isUpdate ? GetText.tr("Update") : GetText.tr("Reinstall")) : GetText.tr("Install")));

        // Top Panel Stuff
        JPanel top = new JPanel();
        top.add(new JLabel(((isReinstall) ? (isUpdate ? GetText.tr("Updating") : GetText.tr("Reinstalling"))
                : GetText.tr("Installing")) + " " + pack.getName()
                + (isReinstall ? GetText.tr(" (Current Version: {0})", instance.getVersionOfPack()) : "")));

        // Middle Panel Stuff
        middle = new JPanel();
        middle.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        middle.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        JLabel instanceNameLabel = new JLabel(GetText.tr("Name") + ": ");
        middle.add(instanceNameLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.FIELD_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        nameField = new JTextField(17);
        nameField.setText(((isReinstall) ? instance.launcher.name : pack.getName()));
        if (isReinstall) {
            nameField.setEnabled(false);
        }
        nameField.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent ce) {
                nameField.requestFocusInWindow();
            }
        });
        nameField.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(final FocusEvent pE) {
            }

            @Override
            public void focusGained(final FocusEvent pE) {
                nameField.selectAll();
            }
        });
        middle.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;

        gbc = this.setupVersionsDropdown(gbc);

        if (isReinstall && instance.launcher.vanillaInstance) {
            gbc.gridx++;
            middle.add(showAllMinecraftVersionsCheckbox, gbc);

            showAllMinecraftVersionsCheckbox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED || e.getStateChange() == ItemEvent.DESELECTED) {
                        setVanillaPackVersions(e.getStateChange() == ItemEvent.SELECTED);
                        setVersionsDropdown();
                    }
                }
            });

            gbc.gridx++;
            middle.add(showAllMinecraftVersionsLabel, gbc);
        }

        gbc = this.setupLoaderVersionsDropdown(gbc);

        if (this.isReinstall) {
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.insets = UIConstants.LABEL_INSETS;
            gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
            saveModsLabel = new JLabelWithHover(GetText.tr("Save Mods") + "? ",
                    Utils.getIconImage(App.THEME.getIconPath("question")),
                    new HTMLBuilder().center().text(GetText.tr(
                            "Since this update changes the Minecraft version, your custom mods may no longer work.<br/><br/>Checking this box will keep your custom mods, otherwise they'll be removed."))
                            .build());
            middle.add(saveModsLabel, gbc);

            gbc.gridx++;
            gbc.insets = UIConstants.FIELD_INSETS;
            gbc.anchor = GridBagConstraints.BASELINE_LEADING;
            saveModsCheckbox = new JCheckBox();

            PackVersion packVersion = ((PackVersion) versionsDropDown.getSelectedItem());
            Optional<VersionManifestVersion> minecraftVersion = Optional.ofNullable(packVersion.minecraftVersion);

            saveModsLabel.setVisible(
                    minecraftVersion.isPresent() && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));
            saveModsCheckbox.setVisible(
                    minecraftVersion.isPresent() && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));

            middle.add(saveModsCheckbox, gbc);
        }

        // Bottom Panel Stuff
        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout());
        install.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Installable installable = null;

                PackVersion packVersion = ((PackVersion) versionsDropDown.getSelectedItem());
                LoaderVersion loaderVersion = (packVersion.hasLoader() && packVersion.hasChoosableLoader())
                        ? ((ComboItem<LoaderVersion>) loaderVersionsDropDown.getSelectedItem()).getValue()
                        : null;

                if (modrinthProject != null) {
                    installable = new ModrinthInstallable(pack, packVersion, loaderVersion);

                    installable.modrinthProject = modrinthProject;
                } else if (multiMCManifest != null) {
                    installable = new MultiMCInstallable(pack, packVersion, loaderVersion);

                    installable.multiMCManifest = multiMCManifest;
                    installable.multiMCExtractedPath = extractedPath;
                } else if (instance != null && instance.launcher.vanillaInstance) {
                    installable = new VanillaInstallable(packVersion.minecraftVersion, loaderVersion,
                            instance.launcher.description);
                } else {
                    installable = new ATLauncherInstallable(pack, packVersion, loaderVersion);
                }

                if (instance != null) {
                    installable.instance = instance;
                }

                installable.instanceName = nameField.getText();
                installable.isReinstall = isReinstall;
                installable.isUpdate = isUpdate;
                installable.saveMods = !isServer && isReinstall && saveModsCheckbox.isSelected();

                setVisible(false);

                boolean success = installable.startInstall();

                if (success) {
                    dispose();
                }
            }
        });
        JButton cancel = new JButton(GetText.tr("Cancel"));
        cancel.addActionListener(e -> dispose());
        bottom.add(install);
        bottom.add(cancel);

        add(top, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        WindowUtils.resizeForContent(this);

        setVisible(true);
    }

    private void handlePackInstall(Object object) {
        pack = (Pack) object;
        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", pack.getName()));
    }

    private void handleVanillaInstall() {
        pack = new Pack();
        pack.vanillaInstance = true;
        pack.name = instance.launcher.pack;
        pack.description = instance.launcher.description;

        setVanillaPackVersions(false);
    }

    private void setVanillaPackVersions(boolean showAll) {
        pack.versions = MinecraftManager.getMinecraftVersions().stream()
                .filter(mv -> showAll || mv.type == instance.type).filter(mv -> {
                    if (mv.type == VersionManifestVersionType.EXPERIMENT
                            && !ConfigManager.getConfigItem("minecraft.experiment.enabled", true)) {
                        return false;
                    }

                    if (mv.type == VersionManifestVersionType.SNAPSHOT
                            && !ConfigManager.getConfigItem("minecraft.snapshot.enabled", true)) {
                        return false;
                    }

                    if (mv.type == VersionManifestVersionType.RELEASE
                            && !ConfigManager.getConfigItem("minecraft.release.enabled", true)) {
                        return false;
                    }

                    if (mv.type == VersionManifestVersionType.OLD_BETA
                            && !ConfigManager.getConfigItem("minecraft.old_beta.enabled", true)) {
                        return false;
                    }

                return mv.type != VersionManifestVersionType.OLD_ALPHA
                    || ConfigManager.getConfigItem("minecraft.old_alpha.enabled", true);
            }).map(v -> {
                    PackVersion packVersion = new PackVersion();
                    packVersion.version = v.id;
                    packVersion.minecraftVersion = v;

                    if (instance.launcher.loaderVersion != null) {
                        packVersion.hasLoader = true;
                        packVersion.hasChoosableLoader = true;
                        packVersion.loaderType = instance.launcher.loaderVersion.type;
                    }

                    return packVersion;
                }).collect(Collectors.toList());
    }

    private void handleModrinthInstall(Object object) {
        if (object instanceof ModrinthSearchHit) {
            ModrinthSearchHit modrinthSearchHit = (ModrinthSearchHit) object;

            final ProgressDialog<ModrinthProject> modrinthProjectLookupDialog = new ProgressDialog<>(
                    GetText.tr("Getting Modpack Details"), 0, GetText.tr("Getting Modpack Details"),
                    "Aborting Getting Modpack Details");

            modrinthProjectLookupDialog.addThread(new Thread(() -> {
                modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getProject(modrinthSearchHit.projectId));

                modrinthProjectLookupDialog.close();
            }));

            modrinthProjectLookupDialog.start();
            modrinthProject = modrinthProjectLookupDialog.getReturnValue();
        } else {
            modrinthProject = (ModrinthProject) object;
        }

        pack = new Pack();
        pack.name = modrinthProject.title;

        // pack.externalId = modrinthProject.id; // TODO: Fuck me we got a String here
        pack.description = modrinthProject.description;
        pack.websiteURL = String.format("https://modrinth.com/modpack/%s", modrinthProject.slug);
        pack.modrinthProject = modrinthProject;

        List<ModrinthVersion> versions = new ArrayList<>();
        final ProgressDialog<List<ModrinthVersion>> modrinthProjectLookupDialog = new ProgressDialog<>(
                GetText.tr("Getting Modpack Versions"), 0, GetText.tr("Getting Modpack Versions"),
                "Aborting Getting Modpack Versions");

        modrinthProjectLookupDialog.addThread(new Thread(() -> {
            modrinthProjectLookupDialog.setReturnValue(ModrinthApi.getVersions(modrinthProject.id));

            modrinthProjectLookupDialog.close();
        }));

        modrinthProjectLookupDialog.start();
        versions = modrinthProjectLookupDialog.getReturnValue();

        pack.versions = versions.stream()
                .sorted(Comparator.comparing((ModrinthVersion version) -> version.datePublished).reversed())
                .map(version -> {
                    PackVersion packVersion = new PackVersion();
                    packVersion.version = String.format("%s (%s)", version.name, version.versionNumber);
                    packVersion.hasLoader = version.loaders.size() != 0;
                    packVersion._modrinthVersion = version;

                    try {
                        packVersion.minecraftVersion = MinecraftManager
                                .getMinecraftVersion(version.gameVersions.get(0));
                    } catch (InvalidMinecraftVersion e) {
                        LogManager.error(e.getMessage());
                        packVersion.minecraftVersion = null;
                    }

                    return packVersion;
                }).filter(pv -> pv != null).collect(Collectors.toList());

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", modrinthProject.title));
    }

    private void handleMultiMcImport(Object object) {
        multiMCManifest = (MultiMCManifest) object;

        pack = new Pack();
        pack.name = multiMCManifest.config.name;

        PackVersion packVersion = new PackVersion();
        packVersion.version = "1";

        try {
            Optional<MultiMCComponent> minecraftVersionComponent = multiMCManifest.components.stream()
                    .filter(c -> c.uid.equalsIgnoreCase("net.minecraft")).findFirst();

            if (!minecraftVersionComponent.isPresent()) {
                LogManager.error("No net.minecraft component present in manifest");
                return;
            }

            packVersion.minecraftVersion = MinecraftManager
                    .getMinecraftVersion(minecraftVersionComponent.get().version);
        } catch (InvalidMinecraftVersion e) {
            LogManager.error(e.getMessage());
            return;
        }

        packVersion.hasLoader = multiMCManifest.components.stream()
                .anyMatch(c -> c.uid.equalsIgnoreCase("net.minecraftforge")
                        || c.uid.equalsIgnoreCase("net.fabricmc.hashed"));

        pack.versions = Collections.singletonList(packVersion);

        isReinstall = false;

        // #. {0} is the name of the pack the user is installing
        setTitle(GetText.tr("Installing {0}", multiMCManifest.config.name));
    }

    private void handleInstanceInstall(Object object) {
        instance = (Instance) object;

        if (instance.launcher.vanillaInstance) {
            handleVanillaInstall();
        } else {
            pack = instance.getPack();
        }

        isReinstall = true; // We're reinstalling

        if (isUpdate) {
            // #. {0} is the name of the instance the user is updating
            setTitle(GetText.tr("Updating {0}", instance.launcher.name));
        } else {
            // #. {0} is the name of the instance the user is reinstalling
            setTitle(GetText.tr("Reinstalling {0}", instance.launcher.name));
        }
    }

    private GridBagConstraints setupVersionsDropdown(GridBagConstraints gbc) {
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;
        JLabel versionLabel = new JLabel(GetText.tr("Version To Install") + ": ");
        middle.add(versionLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.FIELD_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;

        versionsDropDown = new JComboBox<>();
        setVersionsDropdown();
        middle.add(versionsDropDown, gbc);

        versionsDropDown.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateLoaderVersions((PackVersion) e.getItem());

                if (isReinstall) {
                    PackVersion packVersion = ((PackVersion) e.getItem());
                    Optional<VersionManifestVersion> minecraftVersion = Optional
                            .ofNullable(packVersion.minecraftVersion);

                    saveModsLabel.setVisible(minecraftVersion.isPresent()
                            && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));
                    saveModsCheckbox.setVisible(minecraftVersion.isPresent()
                            && !minecraftVersion.get().id.equalsIgnoreCase(this.instance.id));
                }
            }
        });

        if (autoInstallVersion != null) {
            versionsDropDown.setSelectedItem(autoInstallVersion);
            versionsDropDown.setEnabled(false);
        }

        if (preselectedModrinthVersion != null) {
            Optional<PackVersion> versionToSelect = this.pack.versions.stream()
                    .filter(pv -> pv._modrinthVersion.id.equals(this.preselectedModrinthVersion.id)).findFirst();

            if (versionToSelect.isPresent()) {
                versionsDropDown.setSelectedItem(versionToSelect.get());
            }
        }

        if (multiMCManifest != null) {
            gbc.gridx--;
            versionLabel.setVisible(false);
            versionsDropDown.setVisible(false);
        }

        return gbc;
    }

    private void setVersionsDropdown() {
        List<PackVersion> versions = new ArrayList<>();
        versionsDropDown.removeAllItems();

        if (pack.isTester()) {
            versions.addAll(pack.getDevVersions());
        }
        versions.addAll(pack.getVersions());
        PackVersion forUpdate = null;
        for (PackVersion version : versions) {
            if ((!version.isDev) && (forUpdate == null)) {
                forUpdate = version;
            }
            versionsDropDown.addItem(version);
        }
        if (isUpdate && forUpdate != null) {
            versionsDropDown.setSelectedItem(forUpdate);
        } else if (isReinstall) {
            for (PackVersion version : versions) {
                if (version.versionMatches(instance)) {
                    versionsDropDown.setSelectedItem(version);
                }
            }
        } else {
            for (PackVersion version : versions) {
                if (!version.isRecommended || version.isDev) {
                    continue;
                }
                versionsDropDown.setSelectedItem(version);
                break;
            }
        }

        // ensures that font width is taken into account
        for (PackVersion version : versions) {
            versionLength = Math.max(versionLength,
                    getFontMetrics(App.THEME.getNormalFont()).stringWidth(version.toString()) + 25);
        }

        // ensures that the dropdown is at least 200 px wide
        versionLength = Math.max(200, versionLength);

        // ensures that there is a maximum width of 250 px to prevent overflow
        versionLength = Math.min(250, versionLength);

        versionsDropDown.setPreferredSize(new Dimension(versionLength, 23));
    }

    protected void updateLoaderVersions(PackVersion item) {
        if (!item.hasLoader() || !item.hasChoosableLoader()) {
            loaderVersionLabel.setVisible(false);
            loaderVersionsDropDown.setVisible(false);
            return;
        }

        if (item.loaderType != null && item.loaderType.equalsIgnoreCase("fabric")) {
            if (!ConfigManager.getConfigItem("loaders.fabric.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionLabel.setText(GetText.tr("{0} Version", "Fabric") + ": ");
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("forge")) {
            if (!ConfigManager.getConfigItem("loaders.forge.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionLabel.setText(GetText.tr("{0} Version", "Forge") + ": ");
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("legacyfabric")) {
            if (!ConfigManager.getConfigItem("loaders.legacyfabric.enabled", true)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionLabel.setText(GetText.tr("{0} Version", "Legacy Fabric") + ": ");
        } else if (item.loaderType != null && item.loaderType.equalsIgnoreCase("quilt")) {
            if (!ConfigManager.getConfigItem("loaders.quilt.enabled", false)) {
                return;
            }

            // #. {0} is the loader (Fabric/Forge/Quilt)
            loaderVersionLabel.setText(GetText.tr("{0} Version", "Quilt") + ": ");
        } else {
            loaderVersionLabel.setText(GetText.tr("Loader Version") + ": ");
        }

        loaderVersionsDropDown.setEnabled(false);
        loaderVersions.clear();

        loaderVersionsDropDown.removeAllItems();
        loaderVersionsDropDown.addItem(new ComboItem<LoaderVersion>(null, GetText.tr("Getting Loader Versions")));

        loaderVersionLabel.setVisible(true);
        loaderVersionsDropDown.setVisible(true);

        install.setEnabled(false);
        versionsDropDown.setEnabled(false);

        Runnable r = () -> {
            loaderVersions.clear();

            if (this.instance != null && this.instance.launcher.vanillaInstance) {
                if (this.instance.launcher.loaderVersion.isFabric()) {
                    loaderVersions.addAll(FabricLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isLegacyFabric()) {
                    loaderVersions.addAll(LegacyFabricLoader.getChoosableVersions(item.minecraftVersion.id));
                } else if (this.instance.launcher.loaderVersion.isQuilt()) {
                    loaderVersions.addAll(QuiltLoader.getChoosableVersions(item.minecraftVersion.id));
                } else {
                    return;
                }
            } else {
                Version jsonVersion = Gsons.DEFAULT.fromJson(pack.getJSON(item.version), Version.class);

                if (jsonVersion == null) {
                    return;
                }

                loaderVersions.addAll(jsonVersion.getLoader().getChoosableVersions(jsonVersion.getMinecraft()));
            }

            if (loaderVersions.size() == 0) {
                loaderVersionsDropDown.removeAllItems();
                loaderVersionsDropDown.addItem(new ComboItem<LoaderVersion>(null, GetText.tr("No Versions Found")));
                loaderVersionLabel.setVisible(true);
                loaderVersionsDropDown.setVisible(true);
                versionsDropDown.setEnabled(true);
                return;
            }

            // ensures that font width is taken into account
            for (LoaderVersion version : loaderVersions) {
                loaderVersionLength = Math.max(loaderVersionLength,
                        getFontMetrics(App.THEME.getNormalFont()).stringWidth(version.toString()) + 25);
            }

            loaderVersionsDropDown.removeAllItems();

            loaderVersions.forEach(version -> loaderVersionsDropDown
                    .addItem(new ComboItem<LoaderVersion>(version, version.toStringWithCurrent(instance))));

            if (isReinstall && instance.launcher.loaderVersion != null) {
                String loaderVersionString = instance.launcher.loaderVersion.version;

                for (int i = 0; i < loaderVersionsDropDown.getItemCount(); i++) {
                    LoaderVersion loaderVersion = loaderVersionsDropDown.getItemAt(i)
                            .getValue();

                    if (loaderVersion.version.equals(loaderVersionString)) {
                        loaderVersionsDropDown.setSelectedIndex(i);
                        break;
                    }
                }
            }

            // ensures that the dropdown is at least 200 px wide
            loaderVersionLength = Math.max(200, loaderVersionLength);

            // ensures that there is a maximum width of 250 px to prevent overflow
            loaderVersionLength = Math.min(250, loaderVersionLength);

            loaderVersionsDropDown.setPreferredSize(new Dimension(loaderVersionLength, 23));

            loaderVersionsDropDown.setEnabled(true);
            loaderVersionLabel.setVisible(true);
            loaderVersionsDropDown.setVisible(true);
            install.setEnabled(true);
            versionsDropDown.setEnabled(true);
        };

        new Thread(r).start();
    }

    private GridBagConstraints setupLoaderVersionsDropdown(GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = UIConstants.LABEL_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_TRAILING;

        middle.add(loaderVersionLabel, gbc);

        gbc.gridx++;
        gbc.insets = UIConstants.FIELD_INSETS;
        gbc.anchor = GridBagConstraints.BASELINE_LEADING;
        this.updateLoaderVersions((PackVersion) this.versionsDropDown.getSelectedItem());
        middle.add(loaderVersionsDropDown, gbc);

        return gbc;
    }
}
