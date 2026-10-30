package mx.arilabs.citas.datos

import java.time.LocalDate
import java.time.LocalTime

/** Textos para WhatsApp y para copiar. Cálidos y cortos, como los escribiría el tío. */
object Mensajes {

    private val dias = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    fun diaSemana(f: LocalDate) = dias[f.dayOfWeek.value - 1]

    /** "sábado 2 de noviembre" */
    fun fecha(f: LocalDate) = "${diaSemana(f)} ${f.dayOfMonth} de ${meses[f.monthValue - 1]}"

    /** "hoy", "mañana" o "el sábado 2 de noviembre" */
    fun cuando(f: LocalDate, hoy: LocalDate) = when (f) {
        hoy -> "hoy"
        hoy.plusDays(1) -> "mañana"
        else -> "el ${fecha(f)}"
    }

    /** "3:00 de la tarde", "12:00 del mediodía" */
    fun hora(h: LocalTime): String {
        val h12 = when {
            h.hour == 0 -> 12
            h.hour > 12 -> h.hour - 12
            else -> h.hour
        }
        val parte = when (h.hour) {
            in 0..5 -> "de la madrugada"
            in 6..11 -> "de la mañana"
            12 -> "del mediodía"
            in 13..18 -> "de la tarde"
            else -> "de la noche"
        }
        return String.format(java.util.Locale.US, "%d:%02d %s", h12, h.minute, parte)
    }

    /** "3:00 p. m." para las listas, donde cabe menos. */
    fun horaCorta(h: LocalTime): String {
        val h12 = if (h.hour % 12 == 0) 12 else h.hour % 12
        return String.format(java.util.Locale.US, "%d:%02d %s", h12, h.minute, if (h.hour < 12) "a. m." else "p. m.")
    }

    fun pesos(n: Int) = "$" + String.format(java.util.Locale.US, "%,d", n)

    private fun saludo(cliente: String, nombre: String): String {
        val a = if (cliente.isBlank()) "Buenas." else "Buenas, $cliente."
        return if (nombre.isBlank()) a else "$a Le escribe $nombre."
    }

    private fun tipo(c: Cita) = when (c.tipo) {
        Tipo.REZO -> "el rezo"
        Tipo.NOVENA -> "la novena"
        Tipo.VELORIO -> "el velorio"
        Tipo.MISA -> "la misa"
        Tipo.OTRO -> "la cita"
    }

    private fun femenino(c: Cita) = c.tipo == Tipo.NOVENA || c.tipo == Tipo.MISA || c.tipo == Tipo.OTRO

    /** "de hoy", "de mañana" o "del sábado 2 de noviembre" */
    private fun deCuando(f: LocalDate, hoy: LocalDate) = when (f) {
        hoy -> "de hoy"
        hoy.plusDays(1) -> "de mañana"
        else -> "del ${fecha(f)}"
    }

    fun recordatorio(c: Cita, nombre: String, hoy: LocalDate): String {
        val donde = listOf(c.lugar, c.colonia).filter { it.isNotBlank() }.joinToString(", ")
        return buildString {
            append(saludo(c.cliente, nombre))
            append(" Le recuerdo ${tipo(c)} ${cuando(c.fecha, hoy)} a las ${hora(c.hora)}")
            if (donde.isNotBlank()) append(", en $donde")
            append(". Cualquier cosa me avisa. Gracias.")
        }
    }

    /** La lista del día para copiar o mandarse a sí mismo. */
    fun avisoDelDia(citas: List<Cita>, dia: LocalDate, hoy: LocalDate): String {
        val titulo = when (dia) {
            hoy -> "Citas de hoy, ${fecha(dia)}"
            hoy.plusDays(1) -> "Citas de mañana, ${fecha(dia)}"
            else -> "Citas del ${fecha(dia)}"
        }
        if (citas.isEmpty()) return "$titulo: ninguna."
        return titulo + ":\n" + citas.joinToString("\n") { c ->
            listOf(horaCorta(c.hora), c.tipo.nombre, c.cliente, c.colonia.ifBlank { c.lugar }, c.telefono)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
        }
    }

    /** Para la persona cuya cita se movió. */
    fun citaMovida(c: Cita, nombre: String, hoy: LocalDate): String =
        saludo(c.cliente, nombre) +
            " Como quedamos, ${tipo(c)} queda para ${cuando(c.fecha, hoy)} a las ${hora(c.hora)}. Gracias por su comprensión."

    /** Para los demás clientes del día en que hubo un cambio. Se puede editar antes de mandar. */
    fun cambioEnElDia(otra: Cita, nombre: String, hoy: LocalDate): String =
        saludo(otra.cliente, nombre) +
            " Le aviso que hubo un cambio en mi agenda ${deCuando(otra.fecha, hoy)}." +
            " Lo suyo sigue a las ${hora(otra.hora)}; si eso cambia, le aviso con tiempo. Gracias."

    fun cancelacion(c: Cita, nombre: String, hoy: LocalDate): String {
        val anticipo = c.anticipo?.takeIf { it > 0 }?.let { pesos(it) }
        val linea = when {
            anticipo == null -> ""
            c.anticipoAlCancelar == Anticipo.SE_QUEDA -> " El anticipo de $anticipo queda como lo acordamos."
            c.anticipoAlCancelar == Anticipo.SE_DEVUELVE -> " Le devuelvo el anticipo de $anticipo."
            c.anticipoAlCancelar == Anticipo.OTRA_FECHA -> " El anticipo de $anticipo queda para la fecha que volvamos a agendar."
            else -> ""
        }
        return saludo(c.cliente, nombre) +
            " Le confirmo que ${tipo(c)} ${deCuando(c.fecha, hoy)} a las ${hora(c.hora)} queda ${if (femenino(c)) "cancelada" else "cancelado"}.$linea"
    }
}
