import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21

plugins {
    kotlin("multiplatform") version "2.1.10"
    id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "io.github.mimimishkin"
version = "1.0.2"
description = "Dependency-free multiplatform library for working with svg paths"

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

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(group.toString(), name, version.toString())

    pom {
        name = project.name
        description = project.description
        inceptionYear = "2025"
        url = "https://github.com/mimimishkin/path-utils"
        licenses {
            license {
                name = "MIT"
            }
        }
        developers {
            developer {
                id = "mimimishkin"
                name = "Mimimishkin"
                email = "printf.mika@gmail.com"
            }
        }
        scm {
            url = "https://github.com/mimimishkin/path-utils"
            connection = "scm:git:git://github.com/mimimishkin/path-utils"
        }
    }
}