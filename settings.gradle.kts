pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

rootProject.name = "Meemo"

include(":app")
include(":core:common")
include(":core:model")
include(":core:ui")
include(":core:database")
include(":core:datastore")
include(":domain")
include(":nlu")
include(":capability")
include(":capability:android")
include(":capability:accessibility")
include(":capability:root")
include(":voice")
include(":feature:assistant")
include(":feature:onboarding")
include(":feature:commands")
include(":feature:apps")
include(":feature:history")
include(":feature:settings")
