package mx.arilabs.citas

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/**
 * Revisa que el APK instalado esté firmado con la llave de Ari Labs. Una copia modificada
 * (por ejemplo, para quitarle la licencia) se tiene que volver a firmar con otra llave.
 */
object Integridad {

    /** true si es la app original, o si es la versión de prueba (sin firma esperada). */
    fun original(context: Context): Boolean {
        val esperada = BuildConfig.FIRMA_ESPERADA
        if (esperada.isBlank()) return true
        return huellas(context).any { it.equals(esperada, ignoreCase = true) }
    }

    @Suppress("DEPRECATION")
    private fun huellas(context: Context): List<String> = runCatching {
        val pm = context.packageManager
        val certificados: List<ByteArray> = if (Build.VERSION.SDK_INT >= 28) {
            val firma = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo
                ?: return@runCatching emptyList()
            val lista = if (firma.hasMultipleSigners()) firma.apkContentsSigners else firma.signingCertificateHistory
            lista.map { it.toByteArray() }
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures?.map { it.toByteArray() } ?: emptyList()
        }
        certificados.map { c -> MessageDigest.getInstance("SHA-256").digest(c).joinToString("") { "%02x".format(it) } }
    }.getOrDefault(emptyList())
}
