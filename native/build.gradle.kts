plugins {
    id("base")
}

data class NativeTarget(val target: String, val platformDir: String, val outputName: String)

val targets = listOf(
    NativeTarget("x86_64-windows-gnu", "win32", "jaha.windows-x64"),
    NativeTarget("x86_64-linux-gnu", "linux", "jaha.linux-x64"),
    NativeTarget("x86_64-macos", "darwin", "jaha.mac-x64"),
    NativeTarget("aarch64-macos", "darwin", "jaha.mac-aarch64")
)

val includeDir = file("include")
val generatedHeaderDir = project(":agent").layout.buildDirectory.dir("generated/jni")
val outputDir = layout.buildDirectory.dir("lib")

val buildTasks = targets.map { t ->
    tasks.register<Exec>("buildNative_${t.target}") {
        dependsOn(":agent:compileJava")

        val out = outputDir.get().asFile
        doFirst { out.mkdirs() }

        workingDir = file("src/main/cpp")
        commandLine(
            "zig", "c++",
            "-target", t.target,
            "-shared", "-O2",
            "-I${includeDir.absolutePath}", // jni.h
            "-I${includeDir.resolve(t.platformDir).absolutePath}", // jni_md.h
            "-I${generatedHeaderDir.get().asFile.absolutePath}",
            "define_class.cpp",
            "member_accessor.cpp",
            "-o", out.resolve(t.outputName).absolutePath
        )
    }
}

tasks.register("buildNative") {
    dependsOn(buildTasks)
}