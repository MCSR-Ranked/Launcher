plugins {
    java
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

group = "com.mcsrrankedlauncher"
version = rootProject.file("src/main/resources/version").readText().trim().replace(".Beta", "")
