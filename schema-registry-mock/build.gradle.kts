description =
    "Mocks the HTTP endpoint of the schema registry for seamlessly testing topologies with Avro or Protobuf serdes."

dependencies {
    val confluentVersion: String by project
    "api"(group = "io.confluent", name = "kafka-avro-serializer", version = confluentVersion)
    "api"(group = "io.confluent", name = "kafka-schema-registry-client", version = confluentVersion)
    "api"(group = "io.confluent", name = "kafka-streams-avro-serde", version = confluentVersion)

    implementation(group = "org.wiremock", name = "wiremock", version = "3.4.2")
    // required because other dependencies use different Jackson versions if this library is used in test scope
    api(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = "2.15.3")

    val junit5Version: String by project
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junit5Version)
    testRuntimeOnly(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junit5Version)
    testImplementation(group = "io.confluent", name = "kafka-protobuf-provider", version = confluentVersion)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                group = "org.fourpm"
                name = "schema-registry-mock"
                description =
                    "Mocks the HTTP endpoint of the schema registry for seamlessly testing topologies with Avro or Protobuf serdes."
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