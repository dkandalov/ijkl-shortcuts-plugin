import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.internal.HasConvention
import org.jetbrains.intellij.IntelliJPluginExtension
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    idea
    java
    kotlin("jvm").version("1.3.71")
    id("org.jetbrains.intellij").version("0.4.17")
}
java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}
repositories {
    mavenCentral()
}

fun sourceRoots(block: SourceSetContainer.() -> Unit) = sourceSets.apply(block)
val SourceSet.kotlin: SourceDirectorySet
    get() = (this as HasConvention).convention.getPlugin<KotlinSourceSet>().kotlin
var SourceDirectorySet.sourceDirs: Iterable<File>
    get() = srcDirs
    set(value) { setSrcDirs(value) }

sourceRoots {
    getByName("main") {
        kotlin.srcDirs("./src")
        resources.srcDirs("./resources")
    }
    getByName("test") {
        kotlin.srcDirs("./test")
    }
}

tasks.withType<KotlinJvmCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.3"
        languageVersion = "1.3"
        // Compiler flag to allow building against pre-released versions of Kotlin
        // because IJ EAP can be built using pre-released Kotlin but it's still worth doing to check API compatability
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
}

configure<IntelliJPluginExtension> {
    val ideVersion = System.getenv().getOrDefault("IJKL_PLUGIN_IDEA_VERSION",
        "IC-193.5233.102"
//        "LATEST-EAP-SNAPSHOT"
    )
    println("Using ide version: $ideVersion")
    version = ideVersion
    pluginName = "ijkl-shortcuts"
    downloadSources = true
    sameSinceUntilBuild = false
    updateSinceUntilBuild = false
}

//task(name = "runIdeWithDifferentJvm", type = RunIdeTask::class, configuration = {
//    setJbreVersion("jbrex8u152b1024.10")
//})

