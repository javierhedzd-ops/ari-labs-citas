plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Ari Labs · Citas. Sin internet: no pide el permiso INTERNET y guarda todo en el teléfono.
android {
    namespace = "mx.arilabs.citas"
    compileSdk = 36

    defaultConfig {
        applicationId = "mx.arilabs.citas"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "1.2.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Llave pública de licencias de Ari Labs (página de licencias de Javi). Vacía = sin candado.
        buildConfigField("String", "LLAVE_LICENCIA", "\"${(project.findProperty("licenciaPublica") as String?) ?: ""}\"")
    }

    // Firma de la versión final: el workflow deja la llave en FIRMA_ARCHIVO (sale de los secretos
    // de GitHub). Sin llave, la versión final no se firma y solo sirve la de prueba.
    val archivoFirma = System.getenv("FIRMA_ARCHIVO")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (archivoFirma != null) {
            create("final") {
                storeFile = archivoFirma
                storeType = "PKCS12"
                storePassword = System.getenv("FIRMA_CLAVE")
                keyAlias = "arilabs"
                keyPassword = System.getenv("FIRMA_CLAVE")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (archivoFirma != null) signingConfig = signingConfigs.getByName("final")
        }
    }

    lint {
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":nucleo"))

    val composeBom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.10.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    implementation("androidx.compose.ui:ui-tooling-preview")

}
