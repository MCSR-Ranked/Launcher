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
package com.atlauncher.data.installables;

import com.atlauncher.data.Pack;
import com.atlauncher.data.PackVersion;
import com.atlauncher.data.minecraft.loaders.LoaderVersion;

public abstract class ATLauncherFormatInstallable extends Installable {
    public Pack pack;
    public PackVersion packVersion;
    public LoaderVersion loaderVersion;

    public ATLauncherFormatInstallable() {
        this(null, null, null, null);
    }

    public ATLauncherFormatInstallable(Pack pack, PackVersion packVersion, LoaderVersion loaderVersion, String lwjglVersion) {
        this.pack = pack;
        this.packVersion = packVersion;
        this.loaderVersion = loaderVersion;
        this.lwjglVersion = lwjglVersion;
    }

    @Override
    public Pack getPack() {
        return this.pack;
    }

    @Override
    public PackVersion getPackVersion() {
        return this.packVersion;
    }

    @Override
    public LoaderVersion getLoaderVersion() {
        return this.loaderVersion;
    }

    @Override
    public String getLWJGLVersion() {
        return null;
    }
}
