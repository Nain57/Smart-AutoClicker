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

include(":core:android")
include(":core:base")
include(":core:bitmaps")
include(":core:database")
include(":core:detection")
include(":core:display")
include(":core:domain")
include(":core:dumb")
include(":core:processing")
include(":core:ui")

include(":feature:backup")
include(":feature:billing")
include(":feature:floating-menu")
include(":feature:scenario-config")
include(":feature:scenario-config-dumb")
include(":feature:scenario-debugging")
include(":feature:tutorial")

include(":smartautoclicker")
