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
package com.atlauncher.gui.card;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.builders.HTMLBuilder;
import com.atlauncher.data.DisableableMod;
import com.atlauncher.data.Instance;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.gui.components.CollapsiblePanel;
import com.atlauncher.gui.components.DropDownButton;
import com.atlauncher.gui.components.ImagePanel;
import com.atlauncher.gui.dialogs.AddModsDialog;
import com.atlauncher.gui.dialogs.EditModsDialog;
import com.atlauncher.gui.dialogs.InstanceSettingsDialog;
import com.atlauncher.gui.dialogs.ProgressDialog;
import com.atlauncher.managers.AccountManager;
import com.atlauncher.managers.ConfigManager;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.utils.OS;

/**
 * <p/>
 * Class for displaying instances in the Instance Tab
 */
public class InstanceCard extends CollapsiblePanel implements RelocalizationListener {
    private final Instance instance;
    private final JTextArea descArea = new JTextArea();
    private final ImagePanel image;
    private final JButton deleteButton = new JButton(GetText.tr("Delete"));
    private final JButton addButton = new JButton(GetText.tr("Add Mods"));
    private final JButton editButton = new JButton(GetText.tr("Edit Mods"));
    private final JButton openButton = new JButton(GetText.tr("Open Folder"));
    private final JButton settingsButton = new JButton(GetText.tr("Settings"));

    private final JPopupMenu playPopupMenu = new JPopupMenu();
    private final JMenuItem playOnlinePlayMenuItem = new JMenuItem(GetText.tr("Play Online"));
    private final JMenuItem playOfflinePlayMenuItem = new JMenuItem(GetText.tr("Play Offline"));
    private final DropDownButton playButton = new DropDownButton(GetText.tr("Play"), playPopupMenu, true,
            new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    play(false);
                }
            });

    private final JPopupMenu editInstancePopupMenu = new JPopupMenu();
    private final JMenuItem cloneMenuItem = new JMenuItem(GetText.tr("Clone"));
    private final JMenuItem renameMenuItem = new JMenuItem(GetText.tr("Rename"));
    private final JMenuItem changeDescriptionMenuItem = new JMenuItem(GetText.tr("Change Description"));
    private final JMenuItem changeImageMenuItem = new JMenuItem(GetText.tr("Change Image"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeFabricVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "Fabric"));
    // #. {0} is the loader (Forge/LegacyFabric/Quilt)
    private final JMenuItem changeLegacyFabricVersionMenuItem = new JMenuItem(
            GetText.tr("Change {0} Version", "Legacy Fabric"));
    // #. {0} is the loader (Forge/Fabric/Quilt)
    private final JMenuItem changeQuiltVersionMenuItem = new JMenuItem(GetText.tr("Change {0} Version", "Quilt"));
    private final DropDownButton editInstanceButton = new DropDownButton(GetText.tr("Edit Instance"),
            editInstancePopupMenu);

    public InstanceCard(Instance instance) {
        super(instance);
        this.instance = instance;
        this.image = new ImagePanel(instance.getImage().getImage());
        JSplitPane splitter = new JSplitPane();
        splitter.setLeftComponent(this.image);
        JPanel rightPanel = new JPanel();
        splitter.setRightComponent(rightPanel);
        splitter.setEnabled(false);

        this.descArea.setText(instance.getPackDescription());
        this.descArea.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        this.descArea.setEditable(false);
        this.descArea.setHighlighter(null);
        this.descArea.setLineWrap(true);
        this.descArea.setWrapStyleWord(true);
        this.descArea.setEditable(false);

        if (instance.canChangeDescription()) {
            this.descArea.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        instance.startChangeDescription();
                        descArea.setText(instance.launcher.description);
                    }
                }
            });
        }

        JPanel top = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));

        JSplitPane as = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        as.setEnabled(false);
        as.setTopComponent(top);
        as.setBottomComponent(bottom);
        as.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        top.add(this.playButton);
        top.add(this.editInstanceButton);
        top.add(this.settingsButton);

        bottom.add(this.deleteButton);

        setupPlayPopupMenus();
        setupButtonPopupMenus();

        bottom.add(this.addButton);

        if (instance.launcher.enableEditingMods) {
            bottom.add(this.editButton);
        }

        bottom.add(this.openButton);

        rightPanel.setLayout(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(rightPanel.getPreferredSize().width, 155));
        rightPanel.add(new JScrollPane(this.descArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        rightPanel.add(as, BorderLayout.SOUTH);

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(splitter, BorderLayout.CENTER);

        RelocalizationManager.addListener(this);

        this.addActionListeners();
        this.addMouseListeners();
    }

    private void setupPlayPopupMenus() {
        playOnlinePlayMenuItem.addActionListener(e -> {
            play(false);
        });
        playPopupMenu.add(playOnlinePlayMenuItem);

        playOfflinePlayMenuItem.addActionListener(e -> {
            play(true);
        });
        playPopupMenu.add(playOfflinePlayMenuItem);
    }

    private void setupButtonPopupMenus() {
        setupEditInstanceButton();
    }

    private void setupEditInstanceButton() {
        editInstancePopupMenu.add(cloneMenuItem);
        editInstancePopupMenu.add(renameMenuItem);
        editInstancePopupMenu.add(changeDescriptionMenuItem);
        editInstancePopupMenu.add(changeImageMenuItem);
        editInstancePopupMenu.addSeparator();

        if (ConfigManager.getConfigItem("loaders.fabric.enabled", true)
                && !ConfigManager.getConfigItem("loaders.fabric.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(changeFabricVersionMenuItem);
        }

        if (ConfigManager.getConfigItem("loaders.legacyfabric.enabled", true)
                && !ConfigManager
                        .getConfigItem("loaders.legacyfabric.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(changeLegacyFabricVersionMenuItem);
        }

        if (ConfigManager.getConfigItem("loaders.quilt.enabled", false)
                && !ConfigManager.getConfigItem("loaders.quilt.disabledMinecraftVersions", new ArrayList<String>())
                        .contains(instance.id)) {
            editInstancePopupMenu.add(changeQuiltVersionMenuItem);
        }

        setEditInstanceMenuItemVisbility();

        cloneMenuItem.addActionListener(e -> instance.startClone());
        renameMenuItem.addActionListener(e -> instance.startRename());
        changeDescriptionMenuItem.addActionListener(e -> {
            instance.startChangeDescription();
            descArea.setText(instance.launcher.description);
        });
        changeImageMenuItem.addActionListener(e -> {
            instance.startChangeImage();
            image.setImage(instance.getImage().getImage());
        });

        changeFabricVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeLegacyFabricVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
        changeQuiltVersionMenuItem.addActionListener(e -> {
            instance.changeLoaderVersion();
            setEditInstanceMenuItemVisbility();
        });
    }

    private void setEditInstanceMenuItemVisbility() {
        changeFabricVersionMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isFabric());
        changeLegacyFabricVersionMenuItem
                .setVisible(
                        instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isLegacyFabric());
        changeQuiltVersionMenuItem
                .setVisible(instance.launcher.loaderVersion != null && instance.launcher.loaderVersion.isQuilt());
    }

    private void addActionListeners() {
        this.addButton.addActionListener(e -> new AddModsDialog(instance));
        this.editButton.addActionListener(e -> new EditModsDialog(instance));
        this.openButton.addActionListener(e -> OS.openFileExplorer(instance.getRoot()));
        this.settingsButton.addActionListener(e -> {
            new InstanceSettingsDialog(instance);
        });
        this.deleteButton.addActionListener(e -> {
            int ret = DialogManager.yesNoDialog(false).setTitle(GetText.tr("Delete Instance"))
                    .setContent(
                            GetText.tr("Are you sure you want to delete the instance \"{0}\"?", instance.launcher.name))
                    .setType(DialogManager.ERROR).show();

            if (ret == DialogManager.YES_OPTION) {
                final ProgressDialog dialog = new ProgressDialog(GetText.tr("Deleting Instance"), 0,
                        GetText.tr("Deleting Instance. Please wait..."), null, App.launcher.getParent());
                dialog.addThread(new Thread(() -> {
                    InstanceManager.removeInstance(instance);
                    dialog.close();
                    App.TOASTER.pop(GetText.tr("Deleted Instance Successfully"));
                }));
                dialog.start();
            }
        });
    }

    private void play(boolean offline) {
        if (!instance.launcher.isPlayable) {
            DialogManager.okDialog().setTitle(GetText.tr("Instance Corrupt"))
                .setContent(GetText
                    .tr("Cannot play instance as it's corrupted. Please reinstall, update or delete it."))
                .setType(DialogManager.ERROR).show();
            return;
        }

        if (offline) {
            DialogManager.okDialog().setTitle(GetText.tr("Offline unavailable"))
                .setContent(GetText.tr("Cannot play instance with offline mode."))
                .setType(DialogManager.ERROR).show();
            return;
        }

        if (!App.settings.ignoreJavaOnInstanceLaunch && instance.shouldShowWrongJavaWarning()) {
            DialogManager.okDialog().setTitle(GetText.tr("Cannot launch instance due to your Java version"))
                    .setContent(new HTMLBuilder().center().text(GetText.tr(
                            "There was an issue launching this instance.<br/><br/>This version of the pack requires a Java version which you are not using.<br/><br/>Please install that version of Java and try again.<br/><br/>Java version needed: {0}",
                            instance.launcher.java.getVersionString())).build())
                    .setType(DialogManager.ERROR).show();
            return;
        }

        ProgressDialog<?> rankedDialog = new ProgressDialog<>(GetText.tr("Update Checking MCSR Ranked"));
        rankedDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        for (DisableableMod customDisableableMod : instance.getCustomDisableableMods()) {
            if (Objects.equals(customDisableableMod.getName(), "Project: MCSR Ranked")) {
                customDisableableMod.checkForUpdate(rankedDialog, instance);
            }
        }

        if (instance.hasUpdate() && !instance.hasLatestUpdateBeenIgnored()) {
            int ret = DialogManager.yesNoDialog().setTitle(GetText.tr("Update Available"))
                    .setContent(new HTMLBuilder().center()
                            .text(GetText.tr(
                                    "An update is available for this instance.<br/><br/>Do you want to update now?"))
                            .build())
                    .addOption(GetText.tr("Ignore This Update"))
                    .addOption(GetText.tr("Don't Remind Me Again")).setType(DialogManager.INFO)
                    .show();

            if (ret == 0) {
                if (AccountManager.getSelectedAccount() == null) {
                    DialogManager.okDialog().setTitle(GetText.tr("No Account Selected"))
                            .setContent(GetText.tr("Cannot update pack as you have no account selected."))
                            .setType(DialogManager.ERROR).show();
                }
            } else if (ret == 1 || ret == DialogManager.CLOSED_OPTION || ret == 2 || ret == 3) {
                if (ret == 2) {
                    instance.ignoreUpdate();
                } else if (ret == 3) {
                    instance.ignoreAllUpdates();
                }

                if (!App.launcher.minecraftLaunched) {
                    if (instance.launch()) {
                        App.launcher.setMinecraftLaunched(true);
                    }
                }
            }
        } else {
            if (!App.launcher.minecraftLaunched) {
                if (instance.launch(offline)) {
                    App.launcher.setMinecraftLaunched(true);
                }
            }
        }
    }

    private void addMouseListeners() {
        this.image.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() >= 2) {
                    play(false);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    JPopupMenu rightClickMenu = new JPopupMenu();

                    JMenuItem playOnlineButton = new JMenuItem(GetText.tr("Play Online"));
                    playOnlineButton.addActionListener(l -> {
                        play(false);
                    });
                    rightClickMenu.add(playOnlineButton);

                    JMenuItem playOfflineButton = new JMenuItem(GetText.tr("Play Offline"));
                    playOfflineButton.addActionListener(l -> {
                        play(true);
                    });
                    rightClickMenu.add(playOnlineButton);

                    JMenuItem reinstallItem = new JMenuItem(GetText.tr("Reinstall"));
                    rightClickMenu.add(reinstallItem);

                    JMenuItem updateItem = new JMenuItem(GetText.tr("Update"));
                    updateItem.setEnabled(instance.hasUpdate() && instance.launcher.isPlayable);
                    rightClickMenu.add(updateItem);

                    rightClickMenu.addSeparator();

                    JMenuItem renameItem = new JMenuItem(GetText.tr("Rename"));
                    renameMenuItem.addActionListener(l -> instance.startRename());
                    rightClickMenu.add(renameItem);

                    JMenuItem changeDescriptionItem = new JMenuItem(GetText.tr("Change Description"));
                    changeDescriptionItem.addActionListener(l -> {
                        instance.startChangeDescription();
                        descArea.setText(instance.launcher.description);
                    });
                    changeDescriptionItem.setVisible(instance.canChangeDescription());
                    rightClickMenu.add(changeDescriptionItem);

                    JMenuItem changeImageItem = new JMenuItem(GetText.tr("Change Image"));
                    changeImageItem.addActionListener(l -> {
                        instance.startChangeImage();
                        image.setImage(instance.getImage().getImage());
                    });
                    rightClickMenu.add(changeImageItem);

                    JMenuItem cloneItem = new JMenuItem(GetText.tr("Clone"));
                    cloneItem.addActionListener(l -> {
                        instance.startClone();
                    });
                    rightClickMenu.add(cloneItem);

                    rightClickMenu.show(image, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                super.mouseEntered(e);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                super.mouseExited(e);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public void onRelocalization() {
        this.playButton.setText(GetText.tr("Play"));
        this.deleteButton.setText(GetText.tr("Delete"));
        this.addButton.setText(GetText.tr("Add Mods"));
        this.editButton.setText(GetText.tr("Edit Mods"));
        this.openButton.setText(GetText.tr("Open Folder"));
        this.settingsButton.setText(GetText.tr("Settings"));
    }
}
