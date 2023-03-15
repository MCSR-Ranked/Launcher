/*
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/7.1.1/userguide/multi_project_builds.html
 */
// TODO simplify this
val props = java.util.Properties()
props.load(File("gradle.properties").reader())
rootProject.name = props["name"] as String
include("app")
include("legacy-launch")