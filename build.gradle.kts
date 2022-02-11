plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"

    id("net.mamoe.mirai-console") version "2.10.0"
    id("net.mamoe.maven-central-publish") version "0.7.1"
}

group = "io.github.gnuf0rce"
version = "1.2.5"

mavenCentralPublish {
    useCentralS01()
    singleDevGithubProject("gnuf0rce", "netdisk-filesync-plugin", "cssxsh")
    licenseFromGitHubProject("AGPL-3.0", "master")
    publication {
        artifact(tasks.getByName("buildPlugin"))
    }
}

mirai {
    configureShadow {
        exclude("module-info.class")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    api("xyz.cssxsh.baidu:baidu-netdisk:2.0.7") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "io.ktor")
    }
    implementation("io.ktor:ktor-client-serialization:1.6.5") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
        exclude(group = "io.ktor", module = "ktor-client-core")
    }
    implementation("io.ktor:ktor-client-encoding:1.6.5") {
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.slf4j")
        exclude(group = "io.ktor", module = "ktor-client-core")
    }
    compileOnly("net.mamoe:mirai-core-utils:2.10.0")
    //
    testImplementation(kotlin("test", "1.6.0"))
}

mirai {
    configureShadow {
        exclude("module-info.class")
    }
}

kotlin {
    explicitApi()
    sourceSets {
        all {
            // languageSettings.optIn("net.mamoe.mirai.console.util.ConsoleExperimentalApi")
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}
