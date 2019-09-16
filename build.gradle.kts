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
    implementation("com.github.tmtsoftware.script-dsl:script-dsl:5583d729ead5d6943ce07f7f343cdb5d08eeccaa")
    //fixme: why do we need to specify esw-ocs-app dependency explicitly
    implementation("com.github.tmtsoftware.esw:esw-ocs-app_2.13:7fd36560f222a03439920f8f77c1a169aeb82cf0")
    compile("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", "1.3.0")
    compile("org.jetbrains.kotlin", "kotlin-script-runtime", "1.3.50")

    testCompile("junit", "junit", "4.12")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += "-Xuse-experimental=kotlin.time.ExperimentalTime"
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