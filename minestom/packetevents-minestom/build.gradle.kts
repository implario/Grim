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

    // PacketEvents (Grim's builds)
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

    mavenCentral()
}

dependencies {
    // The consumer's server application provides Minestom itself.
    compileOnly(libs.minestom)

    api(libs.packetevents.api)
    // Stock byte-buffer operators over real Netty ByteBufs; only channel
    // operations are reimplemented for the fake Minestom channel.
    api(libs.packetevents.netty.common)
    api(libs.netty.buffer)

    testImplementation(libs.minestom)
    testImplementation(libs.minestom.testing)
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publishing.publications.create<MavenPublication>("maven") {
    from(components["java"])
}
