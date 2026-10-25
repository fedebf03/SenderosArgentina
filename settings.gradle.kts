pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SenderosArgentina"
include(":app")

include(":features:auth")
include(":features:senderos")
include(":features:mapa")
include(":features:recorrido")
include(":features:perfil")

include(":core:domain")
include(":core:data")
include(":core:network")
include(":core:database")
include(":core:ui")
include(":core:testing")
