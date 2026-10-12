plugins {
    id("java-conventions")
}

dependencies {
    // Configura is provided by Identica at runtime (this addon no longer loads its own copy). It stays on the
    // compile classpath because the Identica API references its types in signatures; keep the version in line
    // with the one the targeted Identica build ships.
    compileOnly(libs.configura)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.text.serializer.plain)
    compileOnly(libs.packetevents.api)

    testImplementation(libs.configura)
    testImplementation(libs.adventure.minimessage)
}
