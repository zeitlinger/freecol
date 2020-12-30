plugins {
    `java-library`
    `maven-publish`
}

repositories {
    jcenter()
}

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

dependencies {
    //old freecol
    implementation(files("../jars/findbugs-annotations.jar"))
    implementation(files("../jars/commons-cli-1.4.jar"))
    implementation(files("../jars/cortado-0.6.0.jar"))
    implementation(files("../jars/jogg-0.0.17.jar"))
    implementation(files("../jars/jorbis-0.0.17.jar"))
    implementation(files("../jars/miglayout-core-5.0.jar"))
    implementation(files("../jars/miglayout-swing-5.0.jar"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.freecol"
            artifactId = "freecol"
            version = "0.1"

            from(components["java"])
        }
    }
}
