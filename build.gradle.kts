import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.intellij.platform)
}

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()

    intellijPlatform {
        // defaultRepositories() only. An earlier version of this block called snapshots() as
        // well, on the strength of a real measurement of the wrong index. See the long note in
        // gradle/libs.versions.toml: the plugin downloads the IDE from the JetBrains CDN via
        // the product release feed, not from intellij-repository, so what is or is not present
        // in that maven repository does not decide anything here. Gateway is an ordinary
        // release. Do not add snapshots() back without a failure that it fixes.
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // The product under which this plugin runs. `gateway` is the launcher application,
        // not an IDE, and that distinction is the whole point of this repository: a connector
        // lives in the launcher, hands the IDE a machine and an SSH endpoint, and then stops.
        // Anything that has to happen inside the running IDE belongs in the sibling
        // xcpng-intellij-plugin instead.
        gateway(libs.versions.gateway)

        // Reads the plugin against the platform's own compatibility rules. It is the cheapest
        // check that plugin.xml is coherent, and it catches a missing dependency or a bad
        // since-build long before the plugin is loaded anywhere.
        pluginVerifier()
    }

    // compileOnly, not implementation: the IntelliJ Platform provides both at runtime.
    // Bundling either one ships a second copy for the platform to load, which surfaces as a
    // NoSuchMethodError or a LinkageError inside the IDE and not as a build failure. Same
    // trap as the toolbox sibling records for the Toolbox API, reached by a different route.
    //
    // kotlin-stdlib is handled separately, by kotlin.stdlib.default.dependency=false in
    // gradle.properties, because the Kotlin Gradle plugin adds it rather than this block.
    compileOnly(libs.coroutines.core)
    compileOnly(libs.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.core)
    testImplementation(libs.serialization.json)
}

intellijPlatform {
    pluginConfiguration {
        // These are written into plugin.xml at build time. Keeping them here rather than in
        // the file means the version cannot drift from gradle.properties, which is the
        // failure the toolbox sibling hit with its two name fields (see PR #6 there: one of
        // two names was changed and the UI still showed the old one).
        id = "dev.gounthar.xcpng.gateway"
        name = "XCP-ng (unofficial)"
        version = project.version.toString()

        ideaVersion {
            // 262 is the 2026.2 line, matching the Gateway build pinned in the catalog.
            // untilBuild is deliberately left unset: a fixed upper bound is the single most
            // common reason a working plugin stops loading after an IDE upgrade, and there is
            // no evidence yet of an incompatibility to encode.
            sinceBuild = "262"
            untilBuild = provider { null }
        }
    }

    // THE CHECK THAT TESTS THE ONE CLAIM A GREEN BUILD CANNOT.
    //
    // <depends>com.jetbrains.gateway</depends> is resolved at LOAD time, so compiling
    // successfully says nothing about whether that module id exists. The verifier loads the
    // artifact against a real Gateway and reports a missing dependency as a problem, which is
    // the only way to settle it short of launching the app.
    //
    // create(), not ide(). The DSL method is `create`; `ide` does not exist and fails as an
    // unresolved reference. Read from IntelliJPlatformExtension$PluginVerification$Ides, which
    // also offers recommended(), latest {}, select {}, current() and local(...).
    pluginVerification {
        ides {
            create(IntelliJPlatformType.Gateway, libs.versions.gateway.get())
        }
    }

    // Publishing is not wired to a token here. It is a deliberate act, and a token in a build
    // file is a token in a repository. See SECURITY.md.
}

tasks.test {
    useJUnitPlatform()
}

/**
 * Points git at the tracked hooks directory, once, for whoever builds this.
 *
 * Carried from the toolbox sibling unchanged, and for the reason recorded there: hooks are
 * not installed by cloning, so a hook nobody enables enforces nothing. It sets the LOCAL
 * value on purpose, so a developer with a global hooks directory still gets this project's
 * hooks, and it says so when it overrides one. `-PskipGitHooks` opts out.
 */
// tasks.register, not `by tasks.registering`. The delegated form is deprecated as of
// Gradle 9.6 ("Use the named() API instead") and warns on every build.
val installGitHooks = tasks.register("installGitHooks") {
    description = "Points core.hooksPath at .githooks for this repository."
    group = "verification"

    val hooksDir = ".githooks"
    val projectDir = layout.projectDirectory.asFile
    val gitDir = layout.projectDirectory.file(".git").asFile
    val skip = providers.gradleProperty("skipGitHooks").isPresent

    onlyIf { !skip && gitDir.exists() }

    doLast {
        fun git(vararg args: String): Pair<Int, String> {
            val proc = ProcessBuilder(listOf("git") + args)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText().trim()
            return proc.waitFor() to out
        }

        val (_, local) = git("config", "--local", "--get", "core.hooksPath")
        if (local == hooksDir) return@doLast

        val (inheritedCode, inherited) = git("config", "--get", "core.hooksPath")
        val (code, out) = git("config", "--local", "core.hooksPath", hooksDir)
        if (code != 0) {
            println("Could not set core.hooksPath: $out")
            return@doLast
        }
        println("Set core.hooksPath to $hooksDir for this repository; commit-message hooks are active.")
        if (inheritedCode == 0 && inherited.isNotEmpty() && inherited != hooksDir) {
            println("  Note: this overrides '$inherited' here only. Use -PskipGitHooks to opt out.")
        }
    }
}

tasks.named("build") { dependsOn(installGitHooks) }
