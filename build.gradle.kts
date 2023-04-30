plugins {
    application
    `java-library`
    id("org.jetbrains.kotlin.jvm") version "1.8.+"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    // kotlinx serialization
    kotlin("plugin.serialization") version "1.8.+"
}

group = "me.xtrm"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.8.21")
    implementation("com.akuleshov7", "ktoml-core","0.4.1")
    implementation("com.github.Minestom", "Minestom", "79ce9570ea")
}

application {
    mainClass.set("me.xtrm.queueserver.MainKt")
}

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {
    withType<Jar> {
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }
    }
}
