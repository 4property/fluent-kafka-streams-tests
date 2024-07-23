description = "Provides the fluent Kafka Streams test framework."

dependencies {
    api(project(":fluent-kafka-streams-tests"))
    api(project(":schema-registry-mock"))

    val junit5Version: String by project
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junit5Version)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junit5Version)
    api(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junit5Version)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                group = "org.fourpm"
                name = "fluent-kafka-streams-tests-junit5"
                description = "Provides the fluent Kafka Streams test framework."
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "Antojk71"
                        name = "Antony John"
                        email = "antony.john@4pm.ie"
                    }
                }
                scm {
                    connection = "scm:git:https://maven.pkg.github.com/4property/4pm-maven-repo"
                    developerConnection = "scm:https://maven.pkg.github.com/4property/4pm-maven-repo"
                    url = "https://maven.pkg.github.com/4property/4pm-maven-repo"
                }
            }
        }
    }
}
