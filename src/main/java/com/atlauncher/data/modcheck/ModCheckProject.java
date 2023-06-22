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

import org.apache.commons.compress.utils.Lists;

import com.atlauncher.data.json.DownloadType;
import com.atlauncher.data.json.Mod;
import com.atlauncher.data.json.ModType;
import com.pistacium.modcheck.mod.ModFile;
import com.pistacium.modcheck.mod.ModInfo;

public class ModCheckProject {

    private final String name;
    private final ModFile modFile;
    private final boolean available;
    private final String description;

    public List<String> incompatibles;

    public ModCheckProject(ModInfo modInfo, ModFile modFile) {
        this.name = modInfo.getName();
        this.description = modInfo.getDescription();
        this.available = modInfo.isRecommended();
        this.modFile = modFile;
        this.incompatibles = modInfo.getIncompatible();
    }

    public String getDescription() {
        return description;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getName() {
        return name;
    }

    public ModFile getModFile() {
        return modFile;
    }

    public List<String> getIncompatibleMods() {
        if (this.incompatibles == null) {
            ModCheckProject newResource = ModCheckManager.getUpdatedProject(this.getModFile().getVersion(), this);
            if (newResource != null) {
                this.incompatibles = Lists.newArrayList(newResource.getIncompatibleMods().listIterator());
            }
        }
        return this.incompatibles == null ? Lists.newArrayList() : this.incompatibles;
    }

    public Mod convertToMod() {
        Mod mod = new Mod();

        mod.client = true;
        mod.download = DownloadType.direct;
        mod.file = this.getModFile().getName();
        mod.name = this.getName();
        mod.type = ModType.mods;
        mod.url = this.getModFile().getUrl();
        mod.version = this.getModFile().getVersion();

        return mod;
    }
}
