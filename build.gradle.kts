plugins {
    kotlin("jvm") version Versions.kotlin
    kotlin("plugin.serialization") version Versions.kotlin

    id("net.mamoe.mirai-console") version Versions.mirai
}

group = "io.github.gnuf0rce"
version = "1.0.0-dev-1"

repositories {
    mavenLocal()
    // maven(url = "https://maven.aliyun.com/repository/public")
    mavenCentral()
    // maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    gradlePluginPortal()
    maven(url = "https://maven.pkg.github.com/cssxsh/baidu-client") {
        credentials {
            username = System.getenv("GITHUB_ID")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(cssxsh("baidu-oauth", Versions.baidu))
    implementation(cssxsh("baidu-netdisk", Versions.baidu))
    implementation(ktor("client-serialization", Versions.ktor))
    implementation(ktor("client-encoding", Versions.ktor))
    testImplementation(kotlin("test"))
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
            languageSettings.optIn("net.mamoe.mirai.console.util.ConsoleExperimentalApi")
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
