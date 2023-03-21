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

import org.apache.commons.compress.utils.Lists;

import com.atlauncher.data.json.DownloadType;
import com.atlauncher.data.json.Mod;
import com.atlauncher.data.json.ModType;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.resource.ModResource;

public class ModCheckProject {

    private final String name;
    private final ModResource modResource;
    private final boolean available;
    private final String description;

    public List<String> incompatibles;

    public ModCheckProject(ModData modData, ModResource modResource) {
        this.name = modData.getName();
        this.description = modData.getDescription();
        this.available = Objects.isNull(modData.getWarningMessage()) || modData.getWarningMessage().isEmpty();
        this.modResource = modResource;
        this.incompatibles = modData.getIncompatibleMods();
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

    public ModResource getModResource() {
        return modResource;
    }

    public List<String> getIncompatibleMods() {
        if (this.incompatibles == null) {
            ModCheckProject newResource = ModCheckManager.getUpdatedProject(this.getModResource().getModVersion().getVersionName(), this);
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
        mod.file = this.getModResource().getFileName();
        mod.name = this.getName();
        mod.type = ModType.mods;
        mod.url = this.getModResource().getDownloadUrl();
        mod.version = this.getModResource().getModVersion().getVersionName();

        return mod;
    }
}
