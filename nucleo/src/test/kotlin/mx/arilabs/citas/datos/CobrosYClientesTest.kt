package mx.arilabs.citas.datos

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CobrosYClientesTest {

    private val hoy = LocalDate.of(2026, 10, 30)

    private fun c(id: Long, dias: Long, cliente: String, precio: Int? = null, anticipo: Int? = null) =
        Cita(id = id, fecha = hoy.plusDays(dias), hora = LocalTime.of(17, 0), cliente = cliente, tipo = Tipo.REZO, precio = precio, anticipo = anticipo)

    @Test
    fun cobrosDeLaSemana() {
        val citas = listOf(
            c(1, 0, "doña Mari", 600, 200),
            c(2, 1, "don Pedro", 500).copy(pagada = true),
            c(3, 2, "familia Pech", 800, 300).copy(cancelada = true, anticipoAlCancelar = Anticipo.SE_QUEDA),
            c(4, 3, "doña Lupe", 400, 100).copy(cancelada = true, anticipoAlCancelar = Anticipo.SE_DEVUELVE),
            c(5, 20, "fuera de rango", 900),
        )
        val r = Agenda.cobros(citas, hoy, hoy.plusDays(6))
        assertEquals(200 + 500 + 300, r.cobrado)
        assertEquals(400, r.porCobrar)
        assertEquals(2, r.citas)
        assertEquals(listOf(1L), r.pendientes.map { it.id })
    }

    @Test
    fun clientesSeJuntanPorNombre() {
        val citas = listOf(c(1, -10, "Doña Mari"), c(2, 2, "doña mari"), c(3, 1, "don Pedro"))
        val cl = Agenda.clientes(citas, hoy)
        assertEquals(2, cl.size)
        val mari = cl.first { it.citas.size == 2 }
        assertEquals(1L, mari.ultima?.id)
        assertEquals(2L, mari.proxima?.id)
    }

    @Test
    fun proximaCita() {
        val citas = listOf(c(1, 0, "a"), c(2, 1, "b"), c(3, 0, "c").copy(hecha = true))
        assertEquals(1L, Agenda.proxima(citas, LocalDateTime.of(hoy, LocalTime.of(10, 0)))?.id)
        assertEquals(2L, Agenda.proxima(citas, LocalDateTime.of(hoy, LocalTime.of(18, 0)))?.id)
    }
}
