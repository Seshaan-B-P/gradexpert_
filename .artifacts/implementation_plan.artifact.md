# Fix Gradle Sync Error: Missing Version Catalog Definitions

The project is experiencing a Gradle sync error because `app/build.gradle.kts` references multiple plugins and libraries via the `libs` version catalog that are not defined in `gradle/libs.versions.toml`. Specifically, the error `None of the following candidates is applicable: fun PluginDependenciesSpec.kotlin(module: String)` occurs because `libs.plugins.kotlin.compose` is not found, causing the compiler to misinterpret `kotlin` as the built-in Gradle function.

Additionally, many dependencies in the `dependencies` block are also missing from the catalog.

## User Review Required

> [!IMPORTANT]
> The project uses **Android Gradle Plugin (AGP) 9.3.1**, which includes **Built-in Kotlin** support. The `org.jetbrains.kotlin.android` plugin is no longer required and should not be added to the `plugins` block. However, the Compose compiler plugin (`org.jetbrains.kotlin.plugin.compose`) and others still need to be defined in the version catalog if referenced via `alias`.

## Proposed Changes

### [GradeXpert Project Root]

#### [MODIFY] [libs.versions.toml](file:///D:/MAD/GradeXpert/gradle/libs.versions.toml)
- Add missing plugin definitions for `kotlin.compose`, `ksp`, `roborazzi`, `secrets`, and `google-services`.
- Add missing library definitions for Compose, Firebase, Room, Retrofit, and other dependencies used in `app/build.gradle.kts`.
- Update versions to be compatible with AGP 9.3.1 and Kotlin 2.2.10 (runtime requirement for AGP 9.0).

## Verification Plan

### Automated Tests
- Run `./gradlew :app:dependencies` or trigger a Gradle Sync in Android Studio to verify that all accessors are resolved.
- Build the project using `gradle_build("app:assembleDebug")`.

### Manual Verification
- Confirm that the `Unresolved reference` error in `app/build.gradle.kts` is gone.
