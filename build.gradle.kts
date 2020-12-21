plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.21"
}

repositories {
    jcenter()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    
    implementation("io.github.microutils:kotlin-logging-jvm:2.0.2")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

buildscript {
    repositories {
        jcenter()
        mavenCentral()
        gradlePluginPortal()
    }
}

sourceSets.main {
    java.srcDirs("src") //old freecol
}

dependencies {
    val gdxVersion = "1.9.10"

    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")

    //old freecol
    implementation(files("jars/findbugs-annotations.jar"))
    implementation(files("jars/commons-cli-1.4.jar"))
    implementation(files("jars/cortado-0.6.0.jar"))
    implementation(files("jars/jogg-0.0.17.jar"))
    implementation(files("jars/jorbis-0.0.17.jar"))
    implementation(files("jars/miglayout-core-5.0.jar"))
    implementation(files("jars/miglayout-swing-5.0.jar"))
}

