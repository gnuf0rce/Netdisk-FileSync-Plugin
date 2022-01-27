plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.6.0"

    id("net.mamoe.mirai-console") version "2.10.0-RC2"
    id("net.mamoe.maven-central-publish") version "0.7.0"
}

group = "io.github.gnuf0rce"
version = "1.2.1"

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
        exclude {
            it.path.startsWith("okhttp3")
        }
        exclude {
            it.path.startsWith("okio")
        }
    }
}

repositories {
    mavenLocal()
    // maven(url = "https://maven.aliyun.com/repository/public")
    mavenCentral()
    // maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
}

dependencies {
    api("xyz.cssxsh.baidu:baidu-netdisk:2.0.4") {
        exclude(group = "org.jetbrains.kotlin")
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
    compileOnly("net.mamoe:mirai-core-utils:${mirai.coreVersion}")
    //
    testImplementation(kotlin("test", kotlin.coreLibrariesVersion))
}

mirai {
    configureShadow {
        exclude("module-info.class")
    }
}

kotlin {
    sourceSets {
        all {
            // languageSettings.optIn("net.mamoe.mirai.console.util.ConsoleExperimentalApi")
        }
    }
}

java {
    disableAutoTargetJvm()
}

tasks {
    test {
        useJUnitPlatform()
    }
}
