plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.alieninvaders"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.alieninvaders"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        prefab = true
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.games:games-activity:1.2.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.rajawali3d:rajawali:1.1.970")
    testImplementation("junit:junit:4.13.2")
     // Mockito core library
    testImplementation("org.mockito:mockito-core:5.+")
    // Mockito-Kotlin for easier mocking in Kotlin
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.+")
    testImplementation ("androidx.test:core:1.4.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}