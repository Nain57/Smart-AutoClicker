@file:Suppress("UnstableApiUsage")

pluginManagement {
    includeBuild("build-logic")
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

rootProject.name = "Smart Auto Clicker"

include(":core:common:android")
include(":core:common:base")
include(":core:common:bitmaps")
include(":core:common:display")
include(":core:common:ui")
include(":core:dumb")
include(":core:smart:database")
include(":core:smart:detection")
include(":core:smart:domain")
include(":core:smart:processing")

include(":feature:backup")
include(":feature:billing")
include(":feature:dumb-config")
include(":feature:smart-config")
include(":feature:smart-debugging")
include(":feature:tutorial")

include(":smartautoclicker")
