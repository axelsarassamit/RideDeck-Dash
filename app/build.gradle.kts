plugins { id("com.android.application") }
android {
    namespace = "com.axelsarassamit.ridedeck.dash"
    compileSdk = 36
    buildFeatures { buildConfig = true }
    defaultConfig {
        applicationId = "com.axelsarassamit.ridedeck.dash"
        minSdk = 26
        targetSdk = 35
        val releaseVersion = (System.getenv("DASH_VERSION_NAME") ?: "0.4.1").removePrefix("v")
        require(Regex("[0-9]{1,3}\\.[0-9]{1,2}\\.[0-9]{1,2}").matches(releaseVersion)) { "Use numeric major.minor.patch; minor and patch must be under 100" }
        val parts = releaseVersion.split(".").map(String::toInt)
        versionCode = parts[0] * 10000 + parts[1] * 100 + parts[2]
        versionName = releaseVersion
    }
    signingConfigs {
        create("release") {
            val path = System.getenv("DASH_KEYSTORE_PATH")
            if (!path.isNullOrBlank()) {
                storeFile = file(path)
                storePassword = System.getenv("DASH_KEYSTORE_PASSWORD")
                keyAlias = "ridedeck-dash"
                keyPassword = System.getenv("DASH_KEYSTORE_PASSWORD")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-dev" }
        release { signingConfig = signingConfigs.getByName("release") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.core:core:1.16.0")
    implementation("androidx.media3:media3-transformer:1.9.3")
    implementation("androidx.media3:media3-effect:1.9.3")
    testImplementation("junit:junit:4.13.2")
}
