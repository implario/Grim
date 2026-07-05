import versioning.BuildConfig

plugins {
    `maven-publish`
    grim.`base-conventions`
}

// Minestom targets Java 25 class files, so this module cannot follow the
// repo-wide release 17 default from base-conventions.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    val localOverride = if (BuildConfig.mavenLocalOverride) mavenLocal() else null

    // Grim API & PacketEvents
    val grimPublicReleases = maven("https://maven.grim.ac/public/releases") {
        mavenContent { releasesOnly() }
    }
    val grimPublicSnapshots = maven("https://maven.grim.ac/public/snapshots") {
        mavenContent { snapshotsOnly() }
    }
    val grimLegacySnapshots = maven("https://repo.grim.ac/snapshots")
    exclusiveContent {
        forRepositories(*listOfNotNull(localOverride, grimPublicReleases, grimPublicSnapshots, grimLegacySnapshots).toTypedArray())
        filter {
            includeGroup("ac.grim.grimac")
            includeGroup("com.github.retrooper")
        }
    }

    // Configuralize (transitive api dependency of :common)
    exclusive("https://nexus.scarsz.me/content/repositories/releases", { mavenContent { releasesOnly() } }) {
        includeGroup("github.scarsz")
    }

    mavenCentral()
}

dependencies {
    // Consumers get the whole anticheat transitively from this single artifact.
    api(project(":common"))
    api(project(":minestom:packetevents-minestom"))

    // The consumer's server application provides Minestom itself.
    compileOnly(libs.minestom)
    compileOnly(libs.packetevents.api)

    testImplementation(libs.minestom)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
