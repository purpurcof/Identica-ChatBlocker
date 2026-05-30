plugins {
    id("platform-conventions")
    alias(libs.plugins.attache)
}

repositories {
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly(libs.velocity)
    annotationProcessor(libs.velocity)
    implementation(libs.attache.velocity)
    compileOnly(libs.packetevents.api)
    compileOnly(libs.packetevents.velocity)

    testImplementation(libs.velocity)
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("Identica-ChatBlocker-Velocity-${project.version}.jar")
}
