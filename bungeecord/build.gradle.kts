plugins {
    id("platform-conventions")
    alias(libs.plugins.attache)
}

repositories {
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly(libs.bungeecord)
    implementation(libs.attache.bungeecord)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.packetevents.bungeecord)
    implementation(libs.adventure.text.serializer.plain)

    testImplementation(libs.bungeecord)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("Identica-ChatBlocker-BungeeCord-${project.version}.jar")
}