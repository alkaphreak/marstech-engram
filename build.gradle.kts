import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "io.marstech.engram"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    // MCP SDK
    implementation("io.modelcontextprotocol:kotlin-sdk:0.7.2")

    // Ktor (SSE transport — v2 readiness)
    implementation("io.ktor:ktor-server-netty:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // SQLite via Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:0.57.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.57.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.57.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.57.0")
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.0")
    implementation("ch.qos.logback:logback-classic:1.5.12")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

application {
    mainClass.set("io.marstech.engram.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("marstech-engram")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named("distTar") { dependsOn(tasks.named("shadowJar")) }
tasks.named("distZip") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startScripts") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startShadowScripts") { dependsOn(tasks.named("jar")) }

kotlin {
    jvmToolchain(21)
}
