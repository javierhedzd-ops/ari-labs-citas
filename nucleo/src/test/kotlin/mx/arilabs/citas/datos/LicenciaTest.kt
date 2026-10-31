package mx.arilabs.citas.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class LicenciaTest {

    private val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val publica = Base64.getEncoder().encodeToString(par.public.encoded)

    /** Firma como lo hace la página (WebCrypto: r||s de 64 bytes). */
    private fun clave(tel: String, nombre: String): String {
        val s = Signature.getInstance("SHA256withECDSA")
        s.initSign(par.private)
        s.update("MISCITAS1|$tel|$nombre".toByteArray())
        val der = s.sign()
        // DER -> r||s
        var i = 2
        val lr = der[i + 1].toInt(); val r = BigInteger(der.copyOfRange(i + 2, i + 2 + lr)); i += 2 + lr
        val ls = der[i + 1].toInt(); val sv = BigInteger(der.copyOfRange(i + 2, i + 2 + ls))
        fun fijo(b: BigInteger) = b.toByteArray().let { if (it.size > 32) it.copyOfRange(it.size - 32, it.size) else ByteArray(32 - it.size) + it }
        val raw = fijo(r) + fijo(sv)
        val enc = Base64.getUrlEncoder().withoutPadding()
        return enc.encodeToString(nombre.toByteArray()) + "." + enc.encodeToString(raw)
    }

    @Test
    fun claveValidaSoloEnSuTelefono() {
        val c = clave("3f9a12bc77de0451", "Don Javier")
        assertEquals("Don Javier", Licencia.verificar("3F9A-12BC-77DE-0451", c, publica))
        assertNull(Licencia.verificar("0000000000000000", c, publica))
    }

    @Test
    fun claveAlteradaNoSirve() {
        val c = clave("3f9a12bc77de0451", "Don Javier")
        val otroNombre = Base64.getUrlEncoder().withoutPadding().encodeToString("Otro".toByteArray()) + "." + c.substringAfter('.')
        assertNull(Licencia.verificar("3f9a12bc77de0451", otroNombre, publica))
        assertNull(Licencia.verificar("3f9a12bc77de0451", "basura", publica))
        assertNull(Licencia.verificar("3f9a12bc77de0451", c, ""))
    }

    @Test
    fun formatoBonito() {
        assertEquals("3F9A-12BC-77DE-0451", Licencia.bonito("3f9a12bc77de0451"))
    }
}
