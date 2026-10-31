package mx.arilabs.citas.datos

import java.math.BigInteger
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Licencia anual por teléfono, sin servidor. Javi firma
 * "MISCITAS2|<código del teléfono>|<nombre>|<vence AAAAMMDD>" con su llave secreta (ECDSA P-256,
 * en su página de licencias) y la app lo comprueba con la llave pública que lleva dentro.
 * Una clave solo sirve en el teléfono para el que se sacó y hasta la fecha que dice.
 *
 * Formato: base64url(nombre) + "." + AAAAMMDD + "." + base64url(firma r||s de 64 bytes, como WebCrypto).
 */
object Licencia {

    /** Citas propias que se pueden apuntar sin activar la app. */
    const val CITAS_DE_PRUEBA = 5

    /** Días antes del vencimiento en que se avisa. */
    const val DIAS_AVISO = 15

    data class Datos(val nombre: String, val vence: java.time.LocalDate) {
        fun vigente(hoy: java.time.LocalDate) = !hoy.isAfter(vence)
        fun porVencer(hoy: java.time.LocalDate) = vigente(hoy) && !hoy.plusDays(DIAS_AVISO.toLong()).isBefore(vence)
    }

    /** "3f9a12bc77de0451" -> "3F9A-12BC-77DE-0451" */
    fun bonito(codigo: String): String = codigo.uppercase().chunked(4).joinToString("-")

    fun normalizar(codigo: String): String = codigo.lowercase().filter { it in '0'..'9' || it in 'a'..'f' }

    /** Datos de la licencia si la clave está bien firmada para ese teléfono (vigente o no); si no, null. */
    fun verificar(codigoTelefono: String, clave: String, llavePublicaB64: String): Datos? {
        if (llavePublicaB64.isBlank()) return null
        val partes = clave.trim().split('.')
        if (partes.size != 3) return null
        return runCatching { comprobar(codigoTelefono, partes, llavePublicaB64) }.getOrNull()
    }

    private fun comprobar(codigoTelefono: String, partes: List<String>, llavePublicaB64: String): Datos? {
        val dec = Base64.getUrlDecoder()
        val nombre = String(dec.decode(partes[0]), Charsets.UTF_8)
        val vence = partes[1]
        if (!Regex("""\d{8}""").matches(vence)) return null
        val fecha = java.time.LocalDate.parse(vence, java.time.format.DateTimeFormatter.BASIC_ISO_DATE)
        val firma = dec.decode(partes[2])
        if (firma.size != 64 || nombre.isBlank()) return null
        val llave = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(llavePublicaB64.trim())))
        val v = Signature.getInstance("SHA256withECDSA")
        v.initVerify(llave)
        v.update("MISCITAS2|${normalizar(codigoTelefono)}|$nombre|$vence".toByteArray(Charsets.UTF_8))
        return if (v.verify(aDer(firma))) Datos(nombre, fecha) else null
    }

    /** WebCrypto da r||s (64 bytes); Java espera DER. */
    internal fun aDer(raw: ByteArray): ByteArray {
        fun entero(b: ByteArray): ByteArray {
            val v = BigInteger(1, b).toByteArray() // con 0x00 al frente si hace falta
            return byteArrayOf(0x02, v.size.toByte()) + v
        }
        val r = entero(raw.copyOfRange(0, 32))
        val s = entero(raw.copyOfRange(32, 64))
        val cuerpo = r + s
        return byteArrayOf(0x30, cuerpo.size.toByte()) + cuerpo
    }
}
