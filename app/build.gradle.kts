import java.util.Properties
import java.io.FileInputStream

// ─────────────────────────────────────────────
// Load API keys from local.properties
// ─────────────────────────────────────────────
val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
}

// Load Gemini API Key (fallback string if missing)
val geminiKey = localProps.getProperty("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY")

// Load OpenWeatherMap API Key (fallback string if missing)
val openWeatherKey = localProps.getProperty("OPENWEATHER_API_KEY", "YOUR_OPENWEATHER_API_KEY")


plugins {
    alias(libs.plugins.android.application)
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

android {

    // Generate BuildConfig fields
    buildFeatures {
        buildConfig = true
    }

    namespace = "com.example.group316weatherappproject"
    compileSdk = 34   // USE A STABLE VERSION (36 is experimental)

    defaultConfig {
        applicationId = "com.example.group316weatherappproject"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ─────────────────────────────────────────────
        // Inject Gemini API Key into BuildConfig
        // ─────────────────────────────────────────────
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
        
        // Inject OpenWeatherMap API Key into BuildConfig
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openWeatherKey\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")

    // OkHttp + Gson
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ⭐ Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")

    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/html"))
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))

    executionData.setFrom(fileTree(layout.buildDirectory) {
        include(
            "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
            "outputs/code_coverage/debugAndroidTest/connected/**/*.ec"
        )
    })
}
