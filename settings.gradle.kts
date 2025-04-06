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

rootProject.name = "Klick'r"

include(":core:common:android")
include(":core:common:base")
include(":core:common:bitmaps")
include(":core:common:display")
include(":core:common:overlays")
include(":core:common:permissions")
include(":core:common:quality")
include(":core:common:settings")
include(":core:common:ui")
include(":core:dumb")
include(":core:smart:database")
include(":core:smart:detection")
include(":core:smart:domain")
include(":core:smart:processing")
include(":core:smart:training")

include(":feature:backup")
include(":feature:dumb-config")
include(":feature:notifications")
include(":feature:quick-settings-tile")
include(":feature:revenue")
include(":feature:review")
include(":feature:smart-config")
include(":feature:smart-debugging")
include(":feature:tutorial")

include(":smartautoclicker")
