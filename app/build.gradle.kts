plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.tokkit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tokkit"
        minSdk = 26
        targetSdk = 35
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

    // TFLite 모델 파일 관리
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
    }

    // 의존성 충돌 해결
    configurations.all {
        resolutionStrategy {
            // 특정 모듈의 버전 강제 지정
            force("org.tensorflow:tensorflow-lite-api:2.13.0")
            // 충돌하는 클래스를 가진 라이브러리 제외
            exclude(group = "com.google.ai.edge.litert", module = "litert-api")
        }
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

    // TensorFlow Lite 의존성 - 버전 통일
    implementation("org.tensorflow:tensorflow-lite:2.13.0")

    // Support, Metadata 라이브러리는 제외하고 필수 라이브러리만 사용
    // implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
    // implementation("org.tensorflow:tensorflow-lite-metadata:0.4.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}