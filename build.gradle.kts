import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    kotlin("kapt") version "1.3.21"
    application
}

group = "net.hybride"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val arrowVersion = "0.10.2"
dependencies {
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    /*compile("io.arrow-kt:arrow-fx:$arrowVersion")
    compile("io.arrow-kt:arrow-mtl:$arrowVersion")
    compile("io.arrow-kt:arrow-syntax:$arrowVersion")*/
    implementation("io.kotlintest:kotlintest-runner-junit5:3.3.2")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("net.hybride.concurrent.ActorExKt")
}
