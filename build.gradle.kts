plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin

    id("net.mamoe.mirai-console") version Versions.mirai
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
    api(cssxsh("baidu-oauth", Versions.baidu))
    api(cssxsh("baidu-netdisk", Versions.baidu))
    implementation(ktor("client-serialization", Versions.ktor))
    implementation(ktor("client-encoding", Versions.ktor))
    testImplementation(kotlin("test", Versions.kotlin))
}

mirai {
    configureShadow {
        exclude {
            it.path.startsWith("kotlin")
        }
        exclude {
            it.path.startsWith("org")
        }
        exclude {
            it.path.startsWith("io/ktor") &&
                (it.path.startsWith("io/ktor/client/features/compression") || it.path.startsWith("io/ktor/client/features/json")).not()
        }
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
