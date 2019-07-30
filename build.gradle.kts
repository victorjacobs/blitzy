import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val coroutinesVersion = "1.3.0-RC"
val logbackVersion = "1.2.3"
val jacksonVersion = "2.9.9"
val cliktVersion = "2.0.0"
val javaWebsocketVersion = "1.4.0"
val elkiVersion = "0.7.5"

plugins {
    application
    kotlin("jvm") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("org.jlleitschuh.gradle.ktlint") version "8.0.0"
}

group = "dev.vjcbs"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

application {
    mainClassName = "dev.vjcbs.blitzy.MainKt"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.github.ajalt:clikt:$cliktVersion")
    implementation("org.java-websocket:Java-WebSocket:$javaWebsocketVersion")
    implementation("de.lmu.ifi.dbs.elki:elki:$elkiVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}