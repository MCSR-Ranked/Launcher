/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
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
package com.atlauncher.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.SystemTray;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import org.mini2Dx.gettext.GetText;

import com.atlauncher.App;
import com.atlauncher.constants.Constants;
import com.atlauncher.evnt.listener.RelocalizationListener;
import com.atlauncher.evnt.manager.RelocalizationManager;
import com.atlauncher.evnt.manager.TabChangeManager;
import com.atlauncher.gui.components.LauncherBottomBar;
import com.atlauncher.gui.tabs.InstancesTab;
import com.atlauncher.gui.tabs.SettingsTab;
import com.atlauncher.gui.tabs.Tab;
import com.atlauncher.gui.tabs.VanillaPacksTab;
import com.atlauncher.gui.tabs.accounts.AccountsTab;
import com.atlauncher.gui.tabs.news.NewsTab;
import com.atlauncher.gui.tabs.tools.ToolsTab;
import com.atlauncher.managers.DialogManager;
import com.atlauncher.managers.InstanceManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.managers.PerformanceManager;
import com.atlauncher.utils.Utils;

public final class LauncherFrame extends JFrame implements RelocalizationListener {

    private static LauncherFrame instance = null;
    public static LauncherFrame getInstance() {
        return instance;
    }

    private JTabbedPane tabbedPane;

    private List<Tab> tabs;

    public LauncherFrame(boolean show) {
        LogManager.info("Launcher opening");
        LogManager.info("Made By Bob*");
        LogManager.info("*(Not Actually)");

        App.launcher.setParentFrame(this);
        setTitle(Constants.LAUNCHER_NAME);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(true);
        setLayout(new BorderLayout());
        setIconImage(Utils.getImage("/assets/image/icon.png"));

        setMinimumSize(new Dimension(1200, 700));
        setLocationRelativeTo(null);

        try {
            if (App.settings.rememberWindowSizePosition && App.settings.launcherSize != null) {
                setSize(App.settings.launcherSize);
            }

            if (App.settings.rememberWindowSizePosition && App.settings.launcherPosition != null) {
                setLocation(App.settings.launcherPosition);
            }
        } catch (Exception e) {
            LogManager.logStackTrace("Error setting custom remembered window size settings", e);
        }

        LogManager.info("Setting up Bottom Bar");
        LauncherBottomBar bottomBar = new LauncherBottomBar();
        LogManager.info("Finished Setting up Bottom Bar");

        LogManager.info("Setting up Tabs");
        setupTabs(); // Setup the JTabbedPane
        LogManager.info("Finished Setting up Tabs");

        this.add(tabbedPane, BorderLayout.CENTER);
        this.add(bottomBar, BorderLayout.SOUTH);

        if (show) {
            LogManager.info("Showing Launcher");
            setVisible(true);

            if (InstanceManager.getInstances().size() == 0) {
                int ret = DialogManager.yesNoDialog(false).setTitle(GetText.tr("Setup Instance"))
                    .setContent(GetText.tr("Hi, it looks like this is your first time, do you want to create an instance right away?"))
                    .setType(DialogManager.INFO).show();

                if (ret == DialogManager.YES_OPTION) {
                    this.tabbedPane.setSelectedIndex(1);
                }
            }

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent windowEvent) {
                    try {
                        if (SystemTray.isSupported()) {
                            SystemTray.getSystemTray().remove(App.trayIcon);
                        }
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        RelocalizationManager.addListener(this);

        addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.launcherSize = c.getSize();
                    App.settings.save();
                }
            }

            public void componentMoved(ComponentEvent evt) {
                Component c = (Component) evt.getSource();

                if (App.settings.rememberWindowSizePosition) {
                    App.settings.launcherPosition = c.getLocation();
                    App.settings.save();
                }
            }
        });

        instance = this;
    }

    /**
     * Setup the individual tabs used in the Launcher sidebar
     */
    private void setupTabs() {
        tabbedPane = new JTabbedPane(JTabbedPane.RIGHT);
        tabbedPane.setName("mainTabs");

        PerformanceManager.start("newsTab");
        NewsTab newsTab = new NewsTab();
        App.launcher.setNewsPanel(newsTab);
        PerformanceManager.end("newsTab");

        PerformanceManager.start("vanillaPacksTab");
        VanillaPacksTab vanillaPacksTab = new VanillaPacksTab();
        PerformanceManager.end("vanillaPacksTab");

        PerformanceManager.start("instancesTab");
        InstancesTab instancesTab = new InstancesTab();
        App.launcher.setInstancesPanel(instancesTab);
        PerformanceManager.end("instancesTab");

        PerformanceManager.start("accountsTab");
        AccountsTab accountsTab = new AccountsTab();
        PerformanceManager.end("accountsTab");

        PerformanceManager.start("toolsTab");
        ToolsTab toolsTab = new ToolsTab();
        PerformanceManager.end("toolsTab");

        PerformanceManager.start("settingsTab");
        SettingsTab settingsTab = new SettingsTab();
        PerformanceManager.end("settingsTab");

        this.tabs = Arrays.asList(new Tab[] { newsTab, instancesTab, vanillaPacksTab,
                accountsTab, toolsTab, settingsTab });

        tabbedPane.setFont(App.THEME.getTabFont());
        for (Tab tab : this.tabs) {
            this.tabbedPane.addTab(tab.getTitle(), (JPanel) tab);
        }
        tabbedPane.setOpaque(true);
        tabbedPane.setSelectedIndex(App.settings.selectedTabOnStartup);

        tabbedPane.addChangeListener(e -> TabChangeManager.post());
    }

    @Override
    public void onRelocalization() {
        for (int i = 0; i < this.tabbedPane.getTabCount(); i++) {
            this.tabbedPane.setTitleAt(i, this.tabs.get(i).getTitle());
        }

        tabbedPane.setFont(App.THEME.getTabFont());
    }

    public void openTab(int i) {
        tabbedPane.setSelectedIndex(i);
    }
}
