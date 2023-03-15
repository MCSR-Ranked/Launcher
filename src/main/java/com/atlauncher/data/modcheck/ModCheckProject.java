package com.atlauncher.data.modcheck;

import java.util.Objects;

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

    public ModCheckProject(ModData modData, ModResource modResource) {
        this.name = modData.getName();
        this.description = modData.getDescription();
        this.available = Objects.isNull(modData.getWarningMessage()) || modData.getWarningMessage().isEmpty();
        this.modResource = modResource;
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
