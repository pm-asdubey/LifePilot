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

rootProject.name = "LifePilot"

include(":app")
include(":core:common")
include(":domain")
include(":data")
include(":designsystem")
include(":features:home")
include(":features:library")
include(":features:search")
include(":features:object")
include(":features:document")
include(":features:settings")
include(":features:timeline")
include(":features:planner")
include(":evals")
