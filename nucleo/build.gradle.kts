plugins {
    id("org.jetbrains.kotlin.jvm")
}

// El corazón de la app sin Android: lector de citas, agenda y mensajes.
// Se prueba en la JVM (incluida la simulación de 1000 usuarios) sin compilar el APK.
kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    testLogging {
        events("failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
