import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("jvm").version("1.5.10")
    id("org.jetbrains.dokka").version("1.4.32")
    `java-library`
    `maven-publish`
    signing
    jacoco
    id("com.github.nbaztec.coveralls-jacoco").version("1.2.12")
    id("io.github.gradle-nexus.publish-plugin").version("1.1.0")
    id("pl.allegro.tech.build.axion-release").version("1.13.2")
}

val tagVersion = System.getenv("GITHUB_REF")?.split('/')?.last()

project.version = scmVersion.version

repositories {
    mavenCentral()
}
jacoco {
    toolVersion = "0.8.7"
}
dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.12.5")
    api("net.sourceforge.streamsupport:android-retrofuture:1.7.3")
    api("com.squareup.okhttp3:okhttp:4.9.1")
    api("org.slf4j:slf4j-api:1.7.32")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("org.assertj:assertj-core:3.20.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")
    testImplementation("org.slf4j:slf4j-simple:1.7.32")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
    reports {
        xml.required.set(true)
        html.required.set(true)
    }

}


tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("Unleash Proxy Client")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/Unleash/unleash-android-proxy-sdk/tree/${tagVersion ?: "main"}/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

val sonatypeUsername: String? by project
val sonatypePassword: String? by project
val group: String? by project

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group
            artifactId = "unleash-android-proxy-sdk"
            version = "${version}"
            pom {
                name.set("Unleash Android SDK")
                description.set("Android SDK for Unleash")
                url.set("https://docs.getunleash.io/unleash-android-proxy-sdk/index.html")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("chrkolst")
                        name.set("Christopher Kolstad")
                        email.set("chriswk@getunleash.ai")
                    }
                    developer {
                        id.set("ivarconr")
                        name.set("Ivar Conradi Østhus")
                        email.set("ivarconr@getunleash.ai")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Unleash/unleash-android-proxy-sdk")
                    developerConnection.set("scm:git:ssh://git@github.com:Unleash/unleash-android-proxy-sdk")
                    url.set("https://github.com/Unleash/unleash-android-proxy-sdk")
                }
            }
        }
    }
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
            name = "test"
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(sonatypeUsername)
            password.set(sonatypePassword)
        }
    }
}

val signingKey: String? by project
val signingPassphrase: String? by project
signing {
    if (signingKey != null && signingPassphrase != null) {
        useInMemoryPgpKeys(signingKey, signingPassphrase)
        sign(publishing.publications["mavenJava"])
    }
}
