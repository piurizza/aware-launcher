import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

// Release builds pass these from CI: ./gradlew assembleRelease -PversionName=1.2.3 -PversionCode=1002003
val appVersionName = providers.gradleProperty("versionName").getOrElse("0.0.0-dev")
val appVersionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)

// Signing is configured only when a keystore is provided (see .github/workflows/release.yml).
val keystorePath: String? = System.getenv("AWARE_KEYSTORE_PATH")

android {
    namespace = "app.aware.launcher"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.aware.launcher"
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("AWARE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("AWARE_KEY_ALIAS")
                keyPassword = System.getenv("AWARE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = false
    }

    // Keep the APK free of Google-only blobs (also required by F-Droid).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        checkReleaseBuilds = true
        sarifReport = true
        htmlReport = true
        // Version freshness is Dependabot's job, not a reason to fail a build.
        disable += setOf(
            "OldTargetApi",
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "NewerVersionAvailable"
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The "type check" gate: any compiler warning fails the build.
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    testImplementation(libs.junit)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    source.setFrom("src/main/java", "src/test/java")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        md.required.set(false)
    }
}

// detekt bundles its own Kotlin compiler and refuses to run with a different one.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(libs.versions.detekt.kotlin.get())
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint.get())
    android.set(true)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.SARIF)
    }
}
