// Dataset-builder: a JVM-only, independently runnable Gradle module.
//
// It is intentionally NOT wired to the root version catalog so that it can be built
// in isolation from `tools/dataset-builder/`. Versions below mirror the root catalog
// (Kotlin 2.4.10, kotlinx-serialization 1.11.0) but are declared locally on purpose.
plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10"
    application
}

group = "app.vocabflash"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    testImplementation(kotlin("test"))
}

// Compile to a portable JVM target without forcing a toolchain download.
// (The root build pins a local JDK 21; we only require JVM 17 bytecode.)
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

application {
    mainClass.set("app.vocabflash.datasetbuilder.MainKt")
}

// Give the `run` task a sane heap for streaming a large Kaikki extract.
tasks.named<JavaExec>("run") {
    maxHeapSize = "3g"
}
