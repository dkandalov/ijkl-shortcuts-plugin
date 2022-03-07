import org.gradle.api.internal.HasConvention
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("jvm").version("1.5.10")
    id("org.jetbrains.intellij").version("1.4.0")
    id("java")
}
repositories {
    mavenCentral()
}

val SourceSet.kotlin: SourceDirectorySet
    get() = (this as HasConvention).convention.getPlugin<KotlinSourceSet>().kotlin

sourceSets {
    main {
        kotlin.srcDirs("./src")
        resources.srcDirs("./resources")
    }
    test {
        kotlin.srcDirs("./test")
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.5"
        languageVersion = "1.5"
        // Compiler flag to allow building against pre-released versions of Kotlin
        // because IJ EAP can be built using pre-released Kotlin but it's still worth doing to check API compatibility
        freeCompilerArgs = freeCompilerArgs + listOf("-Xskip-metadata-version-check")
    }
}

configure<IntelliJPluginExtension> {
    // To find available IDE versions see https://www.jetbrains.com/intellij-repository/releases
    version.set("212.4746.92")
    // version.set("LATEST-EAP-SNAPSHOT")

    pluginName.set("ijkl-shortcuts")
    downloadSources.set(true)
    sameSinceUntilBuild.set(false)
    updateSinceUntilBuild.set(false)
}
