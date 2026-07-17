plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.5"
}

group = "me.exeos"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

dependencies {
    implementation("org.ow2.asm:asm:9.10.1")
    implementation("org.ow2.asm:asm-commons:9.10.1")
    implementation("org.ow2.asm:asm-util:9.10.1")
    implementation("org.ow2.asm:asm-tree:9.10.1")
    implementation("org.ow2.asm:asm-analysis:9.10.1")
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("-h", layout.buildDirectory.dir("generated/jni").get().asFile.absolutePath))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(":native:buildNative")
    from(project(":native").layout.buildDirectory.dir("lib")) {
        include("jaha.*")
        exclude("**/*.lib", "**/*.pdb", "**/*.exp")
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.jar {
    enabled = false
}