plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
}

val qnnSDKLocalPath = "C:\\Qualcomm\\AIStack\\QAIRT\\2.32.6.250402" // 실제 경로로 수정 필요
val models = listOf("llama3_2_3b")
val relAssetsPath = "src/main/assets/models/"
val buildDir = layout.buildDirectory
val libsDir = buildDir.dir("libs")


android {
    namespace = "com.example.tokkit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tokkit"
        minSdk = 31 // ChatApp 요구사항에 맞춰 수정
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ChatApp의 네이티브 빌드 설정 통합
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                abiFilters("arm64-v8a")
                arguments("-DQNN_SDK_ROOT_PATH=$qnnSDKLocalPath")
            }
        }

        // ChatApp의 JNI 라이브러리 설정 통합
        sourceSets {
            getByName("main") {
                jniLibs.srcDir(libsDir)
            }
        }
    }

    // 데이터 바인딩
    buildFeatures {
        dataBinding = true
        viewBinding = true
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

    // Genie 관련 네이티브 빌드 설정
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"        }
    }

    // ChatApp의 패키징 옵션 통합
    packagingOptions {
        jniLibs.useLegacyPackaging = true
    }

    aaptOptions {
        noCompress("bin", "json")
    }
}

// QNN SDK 검증 및 라이브러리 복사 로직
tasks.register("validateQnnSdk") {
    doLast {
        if (!file(qnnSDKLocalPath).exists()) {
            throw RuntimeException("QNN SDK does not exist at $qnnSDKLocalPath. Please set the correct path.")
        }

        if (!file("$qnnSDKLocalPath/lib/aarch64-android/libGenie.so").exists()) {
            throw RuntimeException("libGenie.so does not exist. Please check QNN SDK installation.")
        }

        // ChatApp에서 가져온 모델 관련 파일 검증
        models.forEach { model ->
            if (!file("$relAssetsPath$model/genie-config.json").exists()) {
                throw RuntimeException("Missing genie-config.json for $model.")
            }
            if (!file("$relAssetsPath$model/tokenizer.json").exists()) {
                throw RuntimeException("Missing tokenizer.json for $model.")
            }
        }
    }
}

// ChatApp에서 가져온 라이브러리 복사 로직
tasks.register("copyQnnLibs") {
    doLast {
        val libsABIDir = buildDir.dir("libs/arm64-v8a").get().asFile
        libsABIDir.mkdirs()

        // QNN 라이브러리 복사
        copy {
            from(qnnSDKLocalPath)
            include("**/lib/aarch64-android/libQnnHtp.so")
            include("**/lib/aarch64-android/libQnnHtpPrepare.so")
            include("**/lib/aarch64-android/libQnnSystem.so")
            include("**/lib/aarch64-android/libQnnSaver.so")
            include("**/lib/hexagon-v**/unsigned/libQnnHtpV**Skel.so")
            include("**/lib/aarch64-android/libQnnHtpV**Stub.so")

            into(libsABIDir)
            // 디렉토리 구조 없이 라이브러리 파일만 복사
            eachFile {
                path = name
            }
            includeEmptyDirs = false
        }
    }
}

tasks.named("preBuild") {
    dependsOn("validateQnnSdk", "copyQnnLibs")
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Room with KSP
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ViewPager2
    implementation ("androidx.viewpager2:viewpager2:1.0.0")

    // RecyclerView
    implementation ("androidx.recyclerview:recyclerview:1.2.1")

    // Material Design
    implementation ("com.google.android.material:material:1.6.0")

    // Glide (이미지 로딩)
    implementation ("com.github.bumptech.glide:glide:4.13.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.13.0")

    // Lottie
    implementation("com.airbnb.android:lottie:6.1.0")

    // markdown
    implementation ("io.noties.markwon:core:4.6.2")
    implementation ("io.noties.markwon:editor:4.6.2")

    // flexbox (자동 줄바꿈)
    implementation ("com.google.android.flexbox:flexbox:3.0.0")

    //Gson
    implementation ("com.google.code.gson:gson:2.8.9")
}
