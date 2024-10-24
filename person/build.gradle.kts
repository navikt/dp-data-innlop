import com.expediagroup.graphql.plugin.gradle.graphql

plugins {
    id("common")
    `java-library`
    id("com.expediagroup.graphql") version "8.2.0"
}

dependencies {
    implementation(libs.konfig)
    implementation(libs.bundles.ktor.client)

    implementation("com.expediagroup", "graphql-kotlin-spring-client", "6.4.0")
    implementation("com.nimbusds:oauth2-oidc-sdk:11.20.1")

    testImplementation("no.nav.security:mock-oauth2-server:2.1.9") {
        exclude(group = "junit", module = "junit")
    }

    testImplementation(libs.mockk)
}

graphql {
    client {
        sdlEndpoint = "https://navikt.github.io/pdl/pdl-api-sdl.graphqls"
        packageName = "no.nav.pdl"
    }
}
