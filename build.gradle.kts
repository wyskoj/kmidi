plugins {
    kotlin("multiplatform") version "1.9.24"
     id("org.jetbrains.dokka") version "1.9.20"
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    `maven-publish`
    signing
}

group = "org.wysko"
version = "0.0.3"

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            artifact(tasks.register("${name}JavadocJar", Jar::class) {
                archiveClassifier.set("javadoc")
                archiveAppendix.set(this@withType.name)
            })

            pom {
                name.set("kmidi")
                description.set("A pragmatic, Kotlin Multiplatform library for parsing, building, and analyzing MIDI files.")
                url.set("https://github.com/wyskoj/kmidi")

                licenses {
                    license {
                        name.set("Apache-2.0 license")
                        url.set("https://opensource.org/license/apache-2-0/")
                    }
                }
                developers {
                    developer {
                        id.set("wyskoj")
                        name.set("Jacob Wysko")
                        email.set("jacob@wysko.org")
                    }
                }
                scm {
                    url.set("https://github.com/wyskoj/kmidi")
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            username = System.getenv("OSSRH_USERNAME").also {
                println("OSSRH_USERNAME = ${it.substring(0..2)}...")
            }
            password = System.getenv("OSSRH_PASSWORD").also {
                println("OSSRH_PASSWORD = ${it.substring(0..2)}...")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("OSSRH_GPG_SECRET_KEY_ID").also {
            println("OSSRH_GPG_SECRET_KEY_ID = ${it.substring(0..2)}...")
        },
        System.getenv("OSSRH_GPG_SECRET_KEY").also {
            println("OSSRH_GPG_SECRET_KEY = ${it.substring(0..2)}...")
        },
        System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD").also {
            println("OSSRH_GPG_SECRET_KEY_PASSWORD = ${it.substring(0..2)}...")
        }
    )
    sign(publishing.publications)
}