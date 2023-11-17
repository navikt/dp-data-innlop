import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
    id("com.github.davidmc24.gradle.plugin.avro") version "1.5.0"
    id("com.expediagroup.graphql") version "6.4.0"
}

repositories {
    mavenCentral()
    maven("https://packages.confluent.io/maven")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(libs.rapids.and.rivers)
    implementation("org.apache.avro:avro:1.11.0")
    implementation("io.confluent:kafka-avro-serializer:7.2.2")
    implementation(libs.kotlin.logging)
    implementation(libs.konfig)

    implementation("com.expediagroup", "graphql-kotlin-spring-client", "6.4.0")
    implementation("com.nimbusds:oauth2-oidc-sdk:10.7")

    testImplementation("no.nav.security:mock-oauth2-server:0.5.8")
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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
