package com.atlauncher.managers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.atlauncher.FileSystem;
import com.atlauncher.Gsons;
import com.atlauncher.data.minecraft.LWJGLLibrary;
import com.atlauncher.data.minecraft.Library;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class LWJGLManager {
    private static final Map<String, List<Library>> versionLibMap = new HashMap<>();

    public static void loadLWJGLVersions() {
        PerformanceManager.start();
        LogManager.debug("Loading LWJGL versions");

        versionLibMap.clear();

        try {
            for (File file : Objects.requireNonNull(FileSystem.LWJGL_VERSIONS_JSON.toFile().listFiles())) {
                addLWJGLVersion(Gsons.DEFAULT.fromJson(new FileReader(file), LWJGLLibrary.class));
            }
        } catch (JsonSyntaxException | FileNotFoundException | JsonIOException e) {
            LogManager.logStackTrace(e);
        }

        LogManager.debug("Finished loading LWJGL versions");
        PerformanceManager.end();
    }

    public static void addLWJGLVersion(LWJGLLibrary lwjglLibrary) {
        if (lwjglLibrary != null) versionLibMap.put(lwjglLibrary.version, lwjglLibrary.libraries);
    }

    public static List<Library> getLWJGLLibraries(String lwjglVersion) {
        return versionLibMap.getOrDefault(lwjglVersion, new ArrayList<>()).stream().filter(Library::hasNativeForOS).collect(Collectors.toList());
    }
}
