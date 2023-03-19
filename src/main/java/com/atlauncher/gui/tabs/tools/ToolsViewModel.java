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
package com.atlauncher.gui.tabs.tools;

import java.io.File;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.atlauncher.App;
import com.atlauncher.Data;
import com.atlauncher.FileSystem;
import com.atlauncher.constants.Constants;
import com.atlauncher.evnt.listener.AccountListener;
import com.atlauncher.evnt.listener.SettingsListener;
import com.atlauncher.evnt.manager.AccountManager;
import com.atlauncher.evnt.manager.SettingsManager;
import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Download;
import com.atlauncher.utils.OS;
import com.atlauncher.utils.Utils;

/**
 * 15 / 06 / 2022
 */
public class ToolsViewModel implements IToolsViewModel, SettingsListener, AccountListener {
    private Consumer<Boolean> onCanRunNetworkCheckerChanged;
    private Consumer<Boolean> onSkinUpdaterEnabledChanged;

    public ToolsViewModel() {
        SettingsManager.addListener(this);
        AccountManager.addListener(this);
    }

    @Override
    public void onSettingsSaved() {
        onCanRunNetworkCheckerChanged.accept(canRunNetworkChecker());
    }

    @Override
    public void onAccountsChanged() {
        onSkinUpdaterEnabledChanged.accept(skinUpdaterEnabled());
    }

    private boolean skinUpdaterEnabled() {
        return Data.ACCOUNTS.size() != 0;
    }

    @Override
    public boolean isDebugEnabled() {
        return LogManager.showDebug;
    }

    @Override
    public boolean isLaunchInDebugEnabled() {
        return !OS.isUsingFlatpak() && !isDebugEnabled();
    }

    @Override
    public void launchInDebug() {
        OS.relaunchInDebugMode();
    }

    @Override
    public void clearDownloads() {
        for (File file : FileSystem.DOWNLOADS.toFile().listFiles()) {
            Utils.delete(file);
        }

        for (File file : FileSystem.FAILED_DOWNLOADS.toFile().listFiles()) {
            Utils.delete(file);
        }
    }

    @Override
    public void deleteLibraries() {
        for (File file : FileSystem.LIBRARIES.toFile().listFiles()) {
            Utils.delete(file);
        }
    }

    @Override
    public void clearLogs() {
        if (Files.exists(FileSystem.LOGS.resolve("old"))) {
            for (File file : FileSystem.LOGS.resolve("old").toFile().listFiles()) {
                Utils.delete(file);
            }
        }
    }

    private boolean canRunNetworkChecker() {
        return App.settings.enableLogs;
    }

    @Override
    public void onCanRunNetworkCheckerChanged(Consumer<Boolean> onChanged) {
        onChanged.accept(canRunNetworkChecker());
        onCanRunNetworkCheckerChanged = onChanged;
    }

    private final String[] HOSTS = { "authserver.mojang.com", "session.minecraft.net", "libraries.minecraft.net",
            "launchermeta.mojang.com", "launcher.mojang.com",
            Constants.DOWNLOAD_HOST, Constants.FABRIC_HOST, Constants.LEGACY_FABRIC_HOST,
            Constants.QUILT_HOST, Constants.MODRINTH_HOST };

    @Override
    public int hostsLength() {
        return HOSTS.length;
    }

    @Override
    public void onSkinUpdaterEnabledChanged(Consumer<Boolean> onChanged) {
        this.onSkinUpdaterEnabledChanged = onChanged;
        onChanged.accept(skinUpdaterEnabled());
    }

    @Override
    public int accountCount() {
        return Data.ACCOUNTS.size();
    }

    @Override
    public void updateSkins(Consumer<Void> onTaskComplete) {
        Data.ACCOUNTS.forEach(account -> {
            account.updateSkin();
            onTaskComplete.accept(null);
        });
    }
}
