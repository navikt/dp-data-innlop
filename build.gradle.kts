import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("com.expediagroup.graphql") version "8.1.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.rapids.and.rivers)
    implementation("org.apache.avro:avro:1.12.0")
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)
    implementation("io.confluent:kafka-avro-serializer:7.7.1")

    implementation("com.expediagroup", "graphql-kotlin-spring-client", "6.4.0")
    implementation("com.nimbusds:oauth2-oidc-sdk:11.20.1")

    testImplementation("no.nav.security:mock-oauth2-server:2.1.9") {
        exclude(group = "junit", module = "junit")
    }
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}

application {
    mainClass.set("no.nav.dagpenger.data.innlop.MainKt")
}

graphql {
    client {
        schemaFile = file("pdl-schema.graphql")
        packageName = "no.nav.pdl"
    }
}
