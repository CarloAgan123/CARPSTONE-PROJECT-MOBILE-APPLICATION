plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    //id("dagger.hilt.android.plugin")
    //id("com.google.dagger.hilt.android")
    alias(libs.plugins.dagger.hilt.plugin)
    alias(libs.plugins.kotlin.kapt)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")

}

android {
    namespace = "com.example.myfirstapplication"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myfirstapplication"
        minSdk = 24
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
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.github.leandroborgesferreira:loading-button-android:2.3.0")
    implementation("com.github.dmytrodanylyk:android-morphing-button:1.0")

    //apply plugin :"kotlin-kapt"

    implementation (libs.navigation.fragment.ktx.v253)
    implementation (libs.androidx.navigation.ui.ktx.v253)

    // Dagger Hilt
    //implementation("com.google.dagger:hilt-android:2.44")
    //kapt("com.google.dagger:hilt-android-compiler:2.44")
    implementation(libs.dagger.hilt)
    kapt(libs.dagger.hilt.compiler)

    //Glide
    implementation (libs.glide)

    //circular image
    implementation(libs.circleimageview)

    //viewpager2 indicatior
    implementation(libs.viewpagerindicator)

    //stepView
    implementation (libs.stepview)

    //QRCode
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)


    //Android Ktx
    implementation (libs.androidx.navigation.fragment.ktx)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // ViewModel
    implementation(libs.androidx.activity.ktx)

    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.dotsindicator)

    implementation("com.github.dhaval2404:imagepicker:2.1")

}

/*kapt {
    correctErrorTypes = true
}*/
