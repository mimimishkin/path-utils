import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
import org.jreleaser.model.Active

plugins {
    kotlin("multiplatform") version "2.1.10"
    id("org.jreleaser") version "1.17.0"
    `maven-publish`
}

group = "dev.mimimishkin"
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

jreleaser {
    project {
        inceptionYear = "2025"
        authors = listOf("mimimishkin")
    }

    release {
        github {
            skipRelease = true
            skipTag = true
        }
    }

    signing {
        active = Active.ALWAYS
        armored = true
        verify = true
    }

    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                url = "https://central.sonatype.com/api/v1/publisher"
                stagingRepositories = listOf(layout.buildDirectory.dir("staging-deploy").get().toString())
                setAuthorization("Basic")
                sign = true
                checksums = true
                sourceJar = true
                javadocJar = true
                retryDelay = 60
            }
        }
    }
}