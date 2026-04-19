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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "cola-music"

include(":app")

include(":core:model")
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:player")
include(":core:lyrics")
include(":core:download")
include(":core:diagnostics")

include(":feature:auth")
include(":feature:home")
include(":feature:library")
include(":feature:search")
include(":feature:player")
include(":feature:lyrics")
include(":feature:downloads")
include(":feature:settings")
