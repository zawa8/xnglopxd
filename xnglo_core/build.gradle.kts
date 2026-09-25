plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Deliberately a plain Kotlin/JVM module (not an Android library module):
// keeps this buildable/testable with just `kotlinc`/Gradle's Kotlin JVM
// plugin, no Android SDK required -- see CLAUDE.md. The `:app` module
// depends on it below like any other Kotlin library.
//
// src/test/kotlin/xnglo/CoreTest.kt is a plain main()-based harness (no
// JUnit/kotlin.test dependency), same reasoning as xnglonpp-ext's
// tests/core_test.cpp -- verified in this repo's sandbox with a bare
// `kotlinc`, since no Android SDK is available here to run a real Gradle
// build. Run it directly:
//   kotlinc src/main/kotlin/xnglo/*.kt src/test/kotlin/xnglo/CoreTest.kt \
//     -include-runtime -d /tmp/xnglotest.jar && java -jar /tmp/xnglotest.jar
kotlin {
    jvmToolchain(17)
}
