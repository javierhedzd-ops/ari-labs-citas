package mx.arilabs.citas.datos

import java.time.LocalDate
import java.time.LocalTime

/** Tipo de evento y cuánto suele durar (minutos). Máximo 2 horas por evento. */
enum class Tipo(val nombre: String, val duracion: Int) {
    REZO("Rezo", 60),
    NOVENA("Novena", 60),
    VELORIO("Velorio", 90),
    MISA("Misa", 60),
    BODA("Boda", 120),
    XV("XV años", 120),
    FIESTA("Fiesta", 120),
    OTRO("Otro", 60),
}

const val DURACION_MAXIMA = 120

/** En qué parte del evento va una canción. */
enum class Momento(val nombre: String) {
    ENTRADA("Entrada"),
    MISA("Misa"),
    COMUNION("Comunión"),
    SALIDA("Salida"),
    VALS("Vals"),
    FIESTA("Fiesta"),
    REZO("Rezo"),
    OTRO("Otra"),
}

data class Cancion(
    val id: Long,
    val titulo: String,
    val tono: String = "",
    val momento: Momento = Momento.OTRO,
    val notas: String = "",
)

enum class Zona(val nombre: String, val orden: Int) {
    CENTRO("Centro", 0),
    NORTE("Norte", 1),
    ORIENTE("Oriente", 2),
    SUR("Sur", 3),
    PONIENTE("Poniente", 4),
    SIN_ZONA("Sin zona", 5),
}

/** Qué pasó con el anticipo al cancelar. */
enum class Anticipo(val nombre: String) {
    SE_QUEDA("Se queda con el anticipo"),
    SE_DEVUELVE("Se devuelve el anticipo"),
    OTRA_FECHA("El anticipo pasa a otra fecha"),
    SIN_ANTICIPO("No había anticipo"),
}

data class Cita(
    val id: Long,
    val fecha: LocalDate,
    val hora: LocalTime,
    val cliente: String,
    val tipo: Tipo,
    val lugar: String = "",
    val colonia: String = "",
    val zona: Zona = Zona.SIN_ZONA,
    val telefono: String = "",
    /** En pesos. Null si no se dijo. */
    val precio: Int? = null,
    val anticipo: Int? = null,
    val notas: String = "",
    val cancelada: Boolean = false,
    val anticipoAlCancelar: Anticipo? = null,
    /** Lo que el tío escribió, tal cual, por si algo se entendió mal. */
    val textoOriginal: String = "",
    /** Ya se hizo (el rezo, la misa...). */
    val hecha: Boolean = false,
    /** Ya se cobró todo lo que faltaba. */
    val pagada: Boolean = false,
    /** Minutos que dura (máximo 2 horas). */
    val duracion: Int = tipo.duracion,
    /** Canciones de este evento, en orden (ids del repertorio). */
    val canciones: List<Long> = emptyList(),
    /** Lo del equipo que ya se palomeó para este evento. */
    val equipoListo: Set<String> = emptySet(),
) {
    fun termina(): java.time.LocalTime = hora.plusMinutes(duracion.toLong())

    /** Lo que se busca en Maps: dirección, colonia y ciudad. */
    fun direccionParaMapa(): String =
        listOf(lugar, colonia, "Mérida, Yucatán")
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")

    fun falta(): Int? {
        val p = precio ?: return null
        return p - (anticipo ?: 0)
    }

    /** Lo que ya entró a la bolsa por esta cita. */
    fun cobrado(): Int {
        val a = anticipo ?: 0
        return when {
            cancelada -> if (anticipoAlCancelar == Anticipo.SE_QUEDA || anticipoAlCancelar == Anticipo.OTRA_FECHA) a else 0
            pagada -> maxOf(precio ?: 0, a)
            else -> a
        }
    }

    /** Lo que falta cobrar (0 si está cancelada o pagada). */
    fun porCobrar(): Int =
        if (cancelada || pagada) 0 else maxOf(0, (precio ?: 0) - (anticipo ?: 0))
}
