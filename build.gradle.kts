import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
    kotlin("multiplatform") version "2.1.10"
    `maven-publish`
}

group = "my.utilities"
version = "1.0.1"

kotlin {
    jvm().compilerOptions.jvmTarget = JVM_21

    sourceSets {
        commonMain
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmMain
    }
}

publishing {
    repositories {
        mavenCentral()
    }
}