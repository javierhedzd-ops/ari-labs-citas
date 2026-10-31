package mx.arilabs.citas

import android.content.Context
import android.os.Build
import java.io.File
import java.time.LocalDateTime

/**
 * Si la app se cierra sola, guarda qué pasó en un archivo. Al abrirla otra vez, ofrece
 * mandarle ese informe a Ari Labs por WhatsApp (la app no usa internet).
 */
object Fallos {

    private const val ARCHIVO = "fallo.txt"

    fun instalar(context: Context) {
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        if (anterior is Guardian) return
        Thread.setDefaultUncaughtExceptionHandler(Guardian(context.applicationContext, anterior))
    }

    fun pendiente(context: Context): String? =
        runCatching { File(context.filesDir, ARCHIVO).takeIf { it.exists() }?.readText() }.getOrNull()

    fun olvidar(context: Context) {
        File(context.filesDir, ARCHIVO).delete()
    }

    private class Guardian(val app: Context, val anterior: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(t: Thread, e: Throwable) {
            runCatching {
                val texto = buildString {
                    appendLine("Mis citas ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android ${Build.VERSION.RELEASE} · ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine(LocalDateTime.now().withNano(0).toString())
                    appendLine()
                    append(e.stackTraceToString().take(6000))
                }
                File(app.filesDir, ARCHIVO).writeText(texto)
            }
            anterior?.uncaughtException(t, e)
        }
    }
}
