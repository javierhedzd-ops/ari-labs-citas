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

    /** Otra cita del mismo día que se encima con [hora] durante [duracion] minutos. */
    fun choca(citas: List<Cita>, dia: LocalDate, hora: LocalTime, sinId: Long? = null, duracion: Int = 60): Cita? {
        val ini = hora.toSecondOfDay() / 60
        val fin = ini + duracion
        return delDia(citas, dia).firstOrNull { o ->
            val oi = o.hora.toSecondOfDay() / 60
            o.id != sinId && ini < oi + o.duracion && oi < fin
        }
    }

    data class Cobros(val cobrado: Int, val porCobrar: Int, val citas: Int, val pendientes: List<Cita>)

    /** Dinero entre [desde] y [hasta] (incluidos). */
    fun cobros(citas: List<Cita>, desde: LocalDate, hasta: LocalDate): Cobros {
        val rango = citas.filter { !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }
        return Cobros(
            cobrado = rango.sumOf { it.cobrado() },
            porCobrar = rango.sumOf { it.porCobrar() },
            citas = rango.count { !it.cancelada },
            pendientes = rango.filter { it.porCobrar() > 0 }.sortedWith(compareBy({ it.fecha }, { it.hora })),
        )
    }

    data class Cliente(
        val nombre: String,
        val telefono: String,
        val colonia: String,
        val citas: List<Cita>,
        val ultima: Cita?,
        val proxima: Cita?,
    )

    /** Clientes agrupados por nombre (sin importar mayúsculas ni acentos), los de cita más reciente primero. */
    fun clientes(citas: List<Cita>, hoy: LocalDate): List<Cliente> =
        citas.filter { it.cliente.isNotBlank() }
            .groupBy { mx.arilabs.citas.lector.Colonias.normalizar(it.cliente) }
            .values
            .map { lista ->
                val orden = lista.sortedWith(compareBy({ it.fecha }, { it.hora }))
                val pasadas = orden.filter { it.fecha.isBefore(hoy) || it.hecha }
                val futuras = orden.filter { !it.fecha.isBefore(hoy) && !it.cancelada && !it.hecha }
                Cliente(
                    nombre = orden.last().cliente,
                    telefono = orden.lastOrNull { it.telefono.isNotBlank() }?.telefono ?: "",
                    colonia = orden.lastOrNull { it.colonia.isNotBlank() }?.colonia ?: "",
                    citas = orden.reversed(),
                    ultima = pasadas.lastOrNull(),
                    proxima = futuras.firstOrNull(),
                )
            }
            .sortedByDescending { c -> c.citas.maxOf { it.fecha } }

    /** La siguiente cita activa desde este momento. */
    fun proxima(citas: List<Cita>, ahora: java.time.LocalDateTime): Cita? =
        citas.filter { !it.cancelada && !it.hecha && !java.time.LocalDateTime.of(it.fecha, it.hora).isBefore(ahora.minusMinutes(30)) }
            .minByOrNull { java.time.LocalDateTime.of(it.fecha, it.hora) }
}
