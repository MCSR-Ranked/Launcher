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
package com.atlauncher.data.minecraft;

import java.util.List;
import java.util.Map;

import com.atlauncher.annot.Json;
import com.atlauncher.utils.OS;
import com.google.gson.annotations.SerializedName;

@Json
public class JavaRuntimes {
    public Map<String, List<JavaRuntime>> gamecore;

    public Map<String, List<JavaRuntime>> linux;
    @SerializedName("linux-i386")
    public Map<String, List<JavaRuntime>> linuxI386;
    @SerializedName("linux-arm")
    public Map<String, List<JavaRuntime>> linuxArm;
    @SerializedName("linux-arm64")
    public Map<String, List<JavaRuntime>> linuxArm64;

    @SerializedName("mac-os")
    public Map<String, List<JavaRuntime>> macOs;
    @SerializedName("mac-os-arm64")
    public Map<String, List<JavaRuntime>> macOsArm64;

    @SerializedName("windows-x64")
    public Map<String, List<JavaRuntime>> windowsX64;
    @SerializedName("windows-x86")
    public Map<String, List<JavaRuntime>> windowsX86;

    public Map<String, List<JavaRuntime>> getForSystem() {
        switch (OS.getOS()) {
            case WINDOWS:
                if (!OS.is64Bit()) {
                    return windowsX86;
                }

                return windowsX64;
            case OSX:
                if (OS.isArm()) {
                    return macOsArm64;
                }

                return macOs;
            case LINUX:
                if (!OS.is64Bit()) {
                    if (OS.isArm()) {
                        return linuxArm;
                    }

                    return linuxI386;
                }

                if (OS.isArm()) {
                    return linuxArm64;
                }

                return linux;
        }

        return null;
    }

    public static String getSystem() {
        switch (OS.getOS()) {
            case WINDOWS:
                if (!OS.is64Bit()) {
                    return "windows-x86";
                }

                return "windows-x64";
            case OSX:
                if (OS.isArm()) {
                    return "mac-os-arm64";
                }

                return "mac-os";
            case LINUX:
                if (!OS.is64Bit()) {
                    if (OS.isArm()) {
                        return "linux-arm";
                    }

                    return "linux-i386";
                }

                if (OS.isArm()) {
                    return "linux-arm64";
                }

                return "linux";
        }

        return "unknown";
    }
}
