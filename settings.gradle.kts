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

includeBuild("build-logic")

// Models for detection, only used for playstore flavour
include(":core:smart:detection-models:models:text:arabic")
include(":core:smart:detection-models:models:text:chinese_simplified")
include(":core:smart:detection-models:models:text:chinese_traditional")
include(":core:smart:detection-models:models:text:cyrillic")
include(":core:smart:detection-models:models:text:devanagari")
include(":core:smart:detection-models:models:text:japanese")
include(":core:smart:detection-models:models:text:kannada")
include(":core:smart:detection-models:models:text:korean")
include(":core:smart:detection-models:models:text:tamil")
include(":core:smart:detection-models:models:text:telugu")

include(":core:common:accessibility")
include(":core:common:actions")
include(":core:common:android")
include(":core:common:base")
include(":core:common:bitmaps")
include(":core:common:display")
include(":core:common:navigation")
include(":core:common:overlays")
include(":core:common:permissions")
include(":core:common:quality")
include(":core:common:settings")
include(":core:common:tutorial")
include(":core:common:ui")
include(":core:dumb")
include(":core:smart:database")
include(":core:smart:debugging")
include(":core:smart:detection")
include(":core:smart:detection-models")
include(":core:smart:domain")
include(":core:smart:processing")

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
