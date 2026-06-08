pluginManagement {
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
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }

        // Mozilla GeckoView 仓库
        maven { url = uri("https://maven.mozilla.org/maven2/") }
    }
}
rootProject.name = "抖音管控App"
include(":app")
