plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

val saseEnvironment = providers.gradleProperty("sase.environment").orElse("DEMO_LOCAL")
val saseSupabaseUrl = providers.gradleProperty("sase.supabaseUrl").orElse("")
val saseSupabasePublishableKey = providers.gradleProperty("sase.supabasePublishableKey").orElse("")

fun buildConfigString(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

kotlin {
  androidTarget {
    compilations.all {
      kotlinOptions {
        jvmTarget = "11"
      }
    }
  }

  jvm("desktop") {
    mainRun {
      mainClass.set("com.example.MainKt")
    }
  }

  listOf(
    iosX64(),
    iosArm64(),
    iosSimulatorArm64()
  ).forEach { iosTarget ->
    iosTarget.binaries.framework {
      baseName = "ComposeApp"
      isStatic = true
    }
  }

  sourceSets {
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.materialIconsExtended)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.content.negotiation)
      implementation(libs.ktor.serialization.kotlinx.json)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.ktor.client.mock)
    }

    androidMain.dependencies {
      implementation(platform(libs.androidx.compose.bom.get()))
      implementation(libs.androidx.compose.ui)
      implementation(libs.androidx.compose.ui.graphics)
      implementation(libs.androidx.compose.ui.tooling.preview)
      implementation(libs.androidx.compose.material3)
      implementation(libs.androidx.compose.material.icons.core)
      implementation(libs.androidx.compose.material.icons.extended)
      implementation(libs.androidx.activity.compose)
      implementation(libs.androidx.core.ktx)
      implementation(libs.androidx.lifecycle.runtime.ktx)
      implementation(libs.androidx.lifecycle.viewmodel.compose)
      implementation(libs.androidx.lifecycle.runtime.compose)
      implementation(libs.kotlinx.coroutines.android)
      implementation(libs.ktor.client.okhttp)
    }

    val desktopMain by getting
    desktopMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.ktor.client.cio)
    }

    iosMain.dependencies {
      implementation(libs.ktor.client.darwin)
    }
  }
}

compose.desktop {
  application {
    mainClass = "com.example.MainKt"
  }
}

android {
  namespace = "com.example"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.aistudio.labvirtual.kvmpx"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
    buildConfigField("String", "SASE_APP_ENVIRONMENT", buildConfigString(saseEnvironment.get()))
    buildConfigField("String", "SASE_SUPABASE_URL", buildConfigString(saseSupabaseUrl.get()))
    buildConfigField(
      "String",
      "SASE_SUPABASE_PUBLISHABLE_KEY",
      buildConfigString(saseSupabasePublishableKey.get())
    )
  }

  buildTypes {
    getByName("release") {
      buildConfigField("String", "SASE_APP_ENVIRONMENT", buildConfigString("PRODUCTION"))
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}
