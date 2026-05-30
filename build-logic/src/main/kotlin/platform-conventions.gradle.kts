plugins {
    id("java-conventions")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":common"))
}

tasks.shadowJar {
    relocate("me.whereareiam.attache", "me.purpurcof.identica.addon.chatblocker.libs.attache")
    minimize()
}
