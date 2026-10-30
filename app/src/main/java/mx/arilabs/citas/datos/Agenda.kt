package mx.arilabs.citas.datos

import java.time.LocalDate
import java.time.LocalTime

/** Cuentas de la agenda que no dependen de Android (se prueban en la JVM). */
object Agenda {

    fun delDia(citas: List<Cita>, dia: LocalDate, incluirCanceladas: Boolean = false): List<Cita> =
        citas.filter { it.fecha == dia && (incluirCanceladas || !it.cancelada) }
            .sortedBy { it.hora }

    /** Los 7 días después de [hoy] (sin contar hoy), solo los que tienen citas. */
    fun proximos7(citas: List<Cita>, hoy: LocalDate): Map<LocalDate, List<Cita>> =
        (1L..7L).map { hoy.plusDays(it) }
            .associateWith { delDia(citas, it) }
            .filterValues { it.isNotEmpty() }

    /**
     * Ordena el día por zona para no cruzar la ciudad: empieza por la zona de la primera
     * cita del día y sigue por las zonas vecinas; dentro de cada zona, por hora.
     * Las horas siguen siendo las de cada cita: esto es una sugerencia de recorrido.
     */
    fun porZona(delDia: List<Cita>): List<Cita> {
        if (delDia.size < 2) return delDia
        val primera = delDia.minBy { it.hora }.zona
        val vuelta = listOf(Zona.CENTRO, Zona.NORTE, Zona.ORIENTE, Zona.SUR, Zona.PONIENTE)
        val inicio = vuelta.indexOf(primera).coerceAtLeast(0)
        val orden = (vuelta.drop(inicio) + vuelta.take(inicio)) + Zona.SIN_ZONA
        return delDia.sortedWith(compareBy<Cita> { orden.indexOf(it.zona) }.thenBy { it.hora })
    }

    /** Citas activas del mismo día que [cita], sin ella: a quienes avisar si se mueve. */
    fun otrasDelDia(citas: List<Cita>, cita: Cita, dia: LocalDate = cita.fecha): List<Cita> =
        delDia(citas, dia).filter { it.id != cita.id }

    fun choca(citas: List<Cita>, dia: LocalDate, hora: LocalTime, sinId: Long? = null): Cita? =
        delDia(citas, dia).firstOrNull { it.id != sinId && it.hora == hora }
}
