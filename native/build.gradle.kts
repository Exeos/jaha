plugins {
    id("base")
}

val jniIncludeDir = System.getProperty("java.home") + "/include"
val jniPlatformIncludeDir = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "$jniIncludeDir/win32"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "$jniIncludeDir/darwin"
    else -> "$jniIncludeDir/linux"
}

val libName = when {
    org.gradle.internal.os.OperatingSystem.current().isWindows -> "jahanative.dll"
    org.gradle.internal.os.OperatingSystem.current().isMacOsX -> "libjahanative.dylib"
    else -> "libjahanative.so"
}

val generatedHeaderDir = project(":agent").layout.buildDirectory.dir("generated/jni")
val outputDir = layout.buildDirectory.dir("lib")

tasks.register<Exec>("buildNative") {
    dependsOn(":agent:compileJava") // for jni headers

    val output = outputDir.get().asFile
    doFirst { output.mkdirs() }

    workingDir = file("src/main/cpp")
    val outFile = output.resolve(libName).absolutePath

    commandLine(
        "g++", "-shared", "-fPIC", "-O2",
        "-I$jniIncludeDir", "-I$jniPlatformIncludeDir",
        "-I${generatedHeaderDir.get().asFile.absolutePath}",
        "define_class.cpp",
        "-o", outFile
    )
}

tasks.build {
    dependsOn("buildNative")
}