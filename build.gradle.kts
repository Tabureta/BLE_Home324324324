// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    val objectboxVersion by extra("4.2.0") // Версия ObjectBox

    repositories {
        mavenCentral() // Репозиторий Maven Central
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2") // Android Gradle Plugin
        classpath("io.objectbox:objectbox-gradle-plugin:$objectboxVersion") // Плагин ObjectBox
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
