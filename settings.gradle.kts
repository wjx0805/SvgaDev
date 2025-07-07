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
        maven {
            setUrl("https://raw.githubusercontent.com/wjx0805/Svgadev/RELEASE_TAG/")
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "SvgaDev"
include(":app")
include(":svga")
