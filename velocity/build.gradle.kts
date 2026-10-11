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

// the plugin version comes from the Gradle project version (see src/main/templates)
val generateBuildConstants = tasks.register<Copy>("generateBuildConstants") {
    val properties = mapOf("version" to project.version.toString())
    inputs.properties(properties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/buildConstants"))
    expand(properties)
}

extensions.configure<JavaPluginExtension> {
    sourceSets["main"].java.srcDir(generateBuildConstants.map { it.destinationDir })
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("Identica-ChatBlocker-Velocity-${project.version}.jar")
}