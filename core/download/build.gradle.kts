plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.colamusic.core.download"
    compileSdk = 34
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))

    implementation(libs.bundles.media3)
    implementation(libs.okhttp)
    implementation(libs.workmanager.runtime)
    implementation(libs.workmanager.hilt)
    ksp(libs.workmanager.hilt.compiler)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}
