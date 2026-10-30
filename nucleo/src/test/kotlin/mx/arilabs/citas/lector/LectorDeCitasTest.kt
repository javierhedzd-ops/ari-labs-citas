package mx.arilabs.citas.lector

import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.datos.Zona
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class LectorDeCitasTest {

    // Viernes 30 de octubre de 2026.
    private val hoy = LocalDate.of(2026, 10, 30)

    @Test
    fun ejemploDelTio() {
        val b = LectorDeCitas.leer("sábado 3pm, colonia Chuburná, doña Mari", hoy)
        assertEquals(LocalDate.of(2026, 10, 31), b.fecha)
        assertEquals(LocalTime.of(15, 0), b.hora)
        assertEquals("Chuburná", b.colonia)
        assertEquals(Zona.NORTE, b.zona)
        assertEquals("doña Mari", b.cliente)
        assertNull(b.tipo)
        assertTrue(b.dudas.any { it.startsWith("¿Es rezo") })
    }

    @Test
    fun citaCompleta() {
        val b = LectorDeCitas.leer(
            "mañana a las 8 de la mañana misa en la iglesia de Santiago, don Pedro 9991234567 \$800 anticipo 300",
            hoy,
        )
        assertEquals(LocalDate.of(2026, 10, 31), b.fecha)
        assertEquals(LocalTime.of(8, 0), b.hora)
        assertEquals(Tipo.MISA, b.tipo)
        assertEquals("don Pedro", b.cliente)
        assertEquals("9991234567", b.telefono)
        assertEquals(800, b.precio)
        assertEquals(300, b.anticipo)
        assertEquals("Santiago", b.colonia)
        assertEquals(Zona.CENTRO, b.zona)
    }

    @Test
    fun direccionYTelefonoConEspacios() {
        val b = LectorDeCitas.leer("novena el 5 de noviembre a las 7, calle 21 x 30 y 32, Montebello, familia Canché, 999 123 4567, 600 pesos", hoy)
        assertEquals(LocalDate.of(2026, 11, 5), b.fecha)
        assertEquals(LocalTime.of(19, 0), b.hora)
        assertEquals(Tipo.NOVENA, b.tipo)
        assertEquals("familia Canché", b.cliente)
        assertEquals("9991234567", b.telefono)
        assertEquals(600, b.precio)
        assertEquals("calle 21 x 30 y 32", b.lugar)
        assertEquals("Montebello", b.colonia)
        assertEquals(Zona.NORTE, b.zona)
    }

    @Test
    fun horaDudosaSePregunta() {
        val b = LectorDeCitas.leer("rezo hoy a las 9, doña Lupe", hoy)
        assertEquals(hoy, b.fecha)
        assertEquals(LocalTime.of(9, 0), b.hora)
        assertTrue(b.dudas.any { it.contains("de la mañana o de la noche") })
    }

    @Test
    fun mediaHoraYVelorio() {
        val b = LectorDeCitas.leer("velorio domingo 6 y media de la tarde, funeraria, sra. Pech", hoy)
        assertEquals(LocalDate.of(2026, 11, 1), b.fecha)
        assertEquals(LocalTime.of(18, 30), b.hora)
        assertEquals(Tipo.VELORIO, b.tipo)
        assertEquals("sra. Pech", b.cliente)
        assertEquals("funeraria", b.lugar)
    }

    @Test
    fun fechaPasadaEsDelProximoAnio() {
        val b = LectorDeCitas.leer("misa 2/1 a las 10am, don Chucho", hoy)
        assertEquals(LocalDate.of(2027, 1, 2), b.fecha)
        assertEquals(LocalTime.of(10, 0), b.hora)
    }

    @Test
    fun numeroDeCalleNoEsHora() {
        val b = LectorDeCitas.leer("rezo viernes, calle 60 x 45, centro, doña Rosa", hoy)
        assertNull(b.hora)
        assertEquals(hoy, b.fecha)
        assertEquals(Zona.CENTRO, b.zona)
    }

    @Test
    fun sinNadaPreguntaTodo() {
        val b = LectorDeCitas.leer("algo", hoy)
        assertTrue(b.dudas.any { it.startsWith("¿Qué día") })
        assertTrue(b.dudas.any { it.startsWith("¿A qué hora") })
    }
}
