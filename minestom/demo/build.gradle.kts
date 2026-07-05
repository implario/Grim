plugins {
    application
    grim.`base-conventions`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":minestom"))
    implementation(libs.minestom)
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("ac.grim.grimac.platform.minestom.demo.DemoServer")
}

tasks.named<JavaExec>("run") {
    // Grim's packet bridge cannot observe BufferedPacket broadcasts, so viewable
    // packet grouping must be disabled on any server running Grim.
    jvmArgs("-Dminestom.viewable-packet=false")
    workingDir = file("run").also { it.mkdirs() }
}
