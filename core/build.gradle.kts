buildscript {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}

sourceSets.main {
    java.srcDirs("../src") //old freecol
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

    //old freecol
    implementation(files("../jars/findbugs-annotations.jar"))
    implementation(files("../jars/commons-cli-1.4.jar"))
    implementation(files("../jars/cortado-0.6.0.jar"))
    implementation(files("../jars/jogg-0.0.17.jar"))
    implementation(files("../jars/jorbis-0.0.17.jar"))
    implementation(files("../jars/miglayout-core-5.0.jar"))
    implementation(files("../jars/miglayout-swing-5.0.jar"))
}

repositories {
    jcenter()
}
