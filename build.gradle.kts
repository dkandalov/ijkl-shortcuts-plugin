import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

buildscript {
    repositories {
        mavenCentral()
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { setUrl("http://dl.bintray.com/jetbrains/intellij-plugin-service") }
    }
}
plugins {
    java
    idea
    kotlin("jvm") version "1.1.1"
    id("org.jetbrains.intellij") version "0.2.17"
}
java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

repositories {
    mavenCentral()
}

java.sourceSets {
    "main" {
        kotlin.srcDirs("./src")
        resources.srcDirs("./resources")
    }
    "test" {
        kotlin.srcDirs("./test")
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        apiVersion = "1.0"
        languageVersion = "1.0"
    }
}

intellij {
    val ideVersion = System.getenv().getOrDefault("IJKL_PLUGIN_IDEA_VERSION", "IC-172.3757.29")
    println("Using ide version: $ideVersion")
    version = ideVersion
    pluginName = "ijkl-shortcuts"
    downloadSources = true
    sameSinceUntilBuild = false
    updateSinceUntilBuild = false
}

val Any.kotlin: SourceDirectorySet get() = withConvention(KotlinSourceSet::class) { kotlin }
