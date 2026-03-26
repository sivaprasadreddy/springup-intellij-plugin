plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "dev.sivalabs"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1")
        bundledPlugin("com.intellij.java")
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    signPlugin {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}