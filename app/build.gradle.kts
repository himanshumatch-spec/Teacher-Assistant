plugins { id("com.android.application") }

android {
    namespace = "com.guruvasishta.teacherassistant"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.guruvasishta.teacherassistant"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.webkit:webkit:1.14.0")
}
