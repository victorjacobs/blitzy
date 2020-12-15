import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val coroutinesVersion = "1.4.2"
val logbackVersion = "1.2.3"
val jacksonVersion = "2.12.0"
val cliktVersion = "2.8.0"
val javaWebsocketVersion = "1.5.1"
val elkiVersion = "0.7.5"
val ktorVersion = "1.4.3"

plugins {
    application
    kotlin("jvm") version "1.4.21"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.4.1"
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
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
