// build.gradle.kts (root)
plugins {
    // Kotlin plugin and Compose Compiler plugin (apply false so modules can apply them)
    id("org.jetbrains.kotlin.android") version "2.0.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false

    // Android Gradle Plugin. Adjust version if your environment requires a different AGP.
    id("com.android.application") version "8.13.0" apply false
    id("com.android.library") version "8.13.0" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
