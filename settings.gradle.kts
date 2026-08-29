rootProject.name = "xcpng-gateway"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        // The IntelliJ Platform Gradle plugin adds the JetBrains repositories itself, through
        // the `intellijPlatform` repository extension declared in build.gradle.kts. It is
        // declared there rather than here because the extension is only available once the
        // plugin has been applied, and applying it is a build-script concern.
    }
}
