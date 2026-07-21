plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

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
      implementation(libs.supabase.auth)
      implementation(libs.supabase.postgrest)
    }

    commonTest.dependencies {
      implementation(kotlin("test"))
      implementation(libs.kotlinx.coroutines.test)
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

    val desktopTest by getting
    desktopTest.dependencies {
      @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
      implementation(compose.uiTest)
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
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    // supabase-kt/Ktor usan java.time; con minSdk 24 se requiere desugaring.
    isCoreLibraryDesugaringEnabled = true
  }

  buildFeatures {
    compose = true
  }
}

dependencies {
  coreLibraryDesugaring(libs.desugar.jdk.libs)
}
