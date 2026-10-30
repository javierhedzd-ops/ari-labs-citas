package mx.arilabs.citas.datos

import java.time.LocalDate
import java.time.LocalTime

enum class Tipo(val nombre: String) {
    REZO("Rezo"),
    NOVENA("Novena"),
    VELORIO("Velorio"),
    MISA("Misa"),
    OTRO("Otro"),
}

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
) {
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
}
