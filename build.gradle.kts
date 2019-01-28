import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

project.version = "0.1"
project.group = "ru.agalkin"

plugins {
    kotlin("jvm") version "1.3.20"
}

val kotlinVersion by extra {
    buildscript.configurations["classpath"].resolvedConfiguration.firstLevelModuleDependencies
        .find { it.moduleName == "org.jetbrains.kotlin.jvm.gradle.plugin" }?.moduleVersion
}

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    compile("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    compile("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    compile("commons-cli:commons-cli:1.4")
    compile("com.google.code.gson:gson:2.8.5")
    compile("org.lz4:lz4-java:1.5.0")

    testCompile("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:5.3.1")
}

val jar: Jar by tasks
jar.baseName = project.name
jar.version = project.version.toString()
jar.manifest.attributes["Main-Class"] = "ru.agalkin.beholder.MainKt"
jar.from(configurations.runtime.map { if (it.isDirectory) it else zipTree(it) })

tasks["build"].dependsOn(jar)

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = "1.8"

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions.jvmTarget = "1.8"

task("print-version") {
    doFirst {
        println(project.version)
    }
}
