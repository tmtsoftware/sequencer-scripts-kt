import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "com.github.tmtsoftware.esw"
version = "0.1-SNAPSHOT"

plugins {
    java
    kotlin("jvm") version "1.3.50"
    `maven-publish`
    application
    id("org.jmailen.kotlinter") version "2.1.1"
}

application {
    mainClassName = "esw.ocs.app.SequencerApp"
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.tmtsoftware.esw:script-dsl:0.1-SNAPSHOT")
    implementation("com.github.tmtsoftware.esw:esw-ocs-app_2.13:0.1-SNAPSHOT")
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
    compile("org.jetbrains.kotlin", "kotlin-script-runtime", "1.3.50")

    testCompile("junit", "junit", "4.12")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
//            freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
    }
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

task<Jar>("sourcesJar") {
    from(project.the<SourceSetContainer>()["main"].java)
    archiveClassifier.set("sources")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            from(components["java"])
        }
    }
}

kotlinter {
    disabledRules = arrayOf("no-wildcard-imports")
}