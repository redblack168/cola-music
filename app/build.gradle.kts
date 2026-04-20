import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseSigningProps: Properties = Properties().apply {
    val f = rootProject.file("release-signing.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

// Read .env.local for debug-only auto-fill of login fields. The file is
// gitignored so these values never ship. Release builds hard-code empty
// strings via buildConfigField so real APKs can't accidentally leak dev
// credentials.
val envLocal: Map<String, String> = buildMap {
    val f = rootProject.file(".env.local")
    if (f.exists()) {
        f.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith("#")) continue
                val eq = line.indexOf('=')
                if (eq <= 0) continue
                val key = line.substring(0, eq).trim()
                val value = line.substring(eq + 1).trim()
                    .removeSurrounding("\"")
                    .removeSurrounding("'")
                put(key, value)
            }
        }
    }
}
fun env(key: String) = envLocal[key] ?: ""
fun String.escapeBuildConfig() = replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.colamusic"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.colamusic"
        minSdk = 26
        targetSdk = 34
        versionCode = 34
        versionName = "0.4.7"
        vectorDrawables { useSupportLibrary = true }
        resourceConfigurations.addAll(listOf("en", "zh-rCN", "zh-rTW"))
    }

    signingConfigs {
        if (releaseSigningProps.getProperty("storeFile")?.isNotBlank() == true) {
            create("release") {
                storeFile = file(releaseSigningProps.getProperty("storeFile"))
                storePassword = releaseSigningProps.getProperty("storePassword")
                keyAlias = releaseSigningProps.getProperty("keyAlias")
                keyPassword = releaseSigningProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true

            // Auto-fill login fields from .env.local in debug only.
            listOf(
                "DEV_SUBSONIC_URL" to env("NAVIDROME_URL"),
                "DEV_SUBSONIC_USER" to env("NAVIDROME_USER"),
                "DEV_SUBSONIC_PASS" to env("NAVIDROME_PASSWORD"),
                "DEV_PLEX_URL" to env("PLEX_URL"),
                "DEV_PLEX_USER" to env("PLEX_USER"),
                "DEV_PLEX_PASS" to env("PLEX_PASSWORD"),
                "DEV_EMBY_URL" to env("EMBY_URL"),
                "DEV_EMBY_USER" to env("EMBY_USER"),
                "DEV_EMBY_PASS" to env("EMBY_PASSWORD"),
            ).forEach { (k, v) ->
                buildConfigField("String", k, "\"${v.escapeBuildConfig()}\"")
            }
        }
        release {
            // Ensure release APK has empty-string DEV_* fields — never
            // ships developer credentials.
            listOf(
                "DEV_SUBSONIC_URL", "DEV_SUBSONIC_USER", "DEV_SUBSONIC_PASS",
                "DEV_PLEX_URL", "DEV_PLEX_USER", "DEV_PLEX_PASS",
                "DEV_EMBY_URL", "DEV_EMBY_USER", "DEV_EMBY_PASS",
            ).forEach { buildConfigField("String", it, "\"\"") }

            // R8 minification temporarily off while we diagnose the play-tap crash —
            // the most recent suspect is R8 stripping something Hilt/Media3 needs at
            // runtime. Re-enable once we see the crash dump and know the real cause.
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/*.kotlin_module",
            "META-INF/versions/9/previous-compilation-data.bin"
        )
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:player"))
    implementation(project(":core:lyrics"))
    implementation(project(":core:download"))
    implementation(project(":core:diagnostics"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:home"))
    implementation(project(":feature:library"))
    implementation(project(":feature:search"))
    implementation(project(":feature:player"))
    implementation(project(":feature:lyrics"))
    implementation(project(":feature:downloads"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.bundles.lifecycle)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.coil.compose)

    implementation(libs.workmanager.runtime)
    implementation(libs.workmanager.hilt)
    ksp(libs.workmanager.hilt.compiler)

    implementation(libs.datastore.preferences)
    implementation(libs.androidx.appcompat)

    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit)
}
