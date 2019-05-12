buildscript {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}


plugins {
    kotlin("jvm") version "1.3.31"
}

dependencies {
    val gdxVersion = "1.9.9"

    implementation(kotlin("stdlib"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
}

repositories {
    jcenter()
}
