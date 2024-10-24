rootProject.name = "dp-dataprodukter"

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20241022.94.4a3bcc")
        }
    }
}

include("behandling")
include("soknad")
include("person")
