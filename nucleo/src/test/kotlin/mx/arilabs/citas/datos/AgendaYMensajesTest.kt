package mx.arilabs.citas.datos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AgendaYMensajesTest {

    private val hoy = LocalDate.of(2026, 10, 30)

    private fun cita(id: Long, h: Int, zona: Zona, cliente: String = "C$id", fecha: LocalDate = hoy) =
        Cita(id = id, fecha = fecha, hora = LocalTime.of(h, 0), cliente = cliente, tipo = Tipo.REZO, zona = zona)

    @Test
    fun porZonaEmpiezaPorLaPrimeraCita() {
        val dia = listOf(
            cita(1, 10, Zona.NORTE),
            cita(2, 12, Zona.SUR),
            cita(3, 15, Zona.NORTE),
            cita(4, 17, Zona.ORIENTE),
        )
        assertEquals(listOf(1L, 3L, 4L, 2L), Agenda.porZona(dia).map { it.id })
    }

    @Test
    fun proximosNoIncluyeHoyNiCanceladas() {
        val citas = listOf(
            cita(1, 10, Zona.NORTE),
            cita(2, 10, Zona.NORTE, fecha = hoy.plusDays(2)),
            cita(3, 10, Zona.NORTE, fecha = hoy.plusDays(3)).copy(cancelada = true),
            cita(4, 10, Zona.NORTE, fecha = hoy.plusDays(8)),
        )
        val p = Agenda.proximos7(citas, hoy)
        assertEquals(setOf(hoy.plusDays(2)), p.keys)
    }

    @Test
    fun horasEnPalabras() {
        assertEquals("3:00 de la tarde", Mensajes.hora(LocalTime.of(15, 0)))
        assertEquals("12:00 del mediodía", Mensajes.hora(LocalTime.NOON))
        assertEquals("8:30 de la noche", Mensajes.hora(LocalTime.of(20, 30)))
        assertEquals("sábado 31 de octubre", Mensajes.fecha(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun cancelacionConAnticipo() {
        val c = Cita(
            id = 1, fecha = hoy.plusDays(1), hora = LocalTime.of(17, 0), cliente = "doña Mari",
            tipo = Tipo.NOVENA, anticipo = 200, cancelada = true, anticipoAlCancelar = Anticipo.SE_DEVUELVE,
        )
        val m = Mensajes.cancelacion(c, "Don Javier", hoy)
        assertTrue(m, m.startsWith("Buenas, doña Mari. Le escribe Don Javier."))
        assertTrue(m, m.contains("la novena de mañana a las 5:00 de la tarde queda cancelada"))
        assertTrue(m, m.contains("Le devuelvo el anticipo de \$200."))
    }
}
