package mx.arilabs.citas.datos

import java.time.LocalDate
import java.time.LocalTime

/**
 * Citas de ejemplo para conocer o enseñar la app. Llevan id negativo: así se reconocen,
 * no generan avisos y se quitan de un solo toque en Ajustes.
 */
object Ejemplos {

    fun esEjemplo(c: Cita) = c.id < 0

    fun crear(hoy: LocalDate): List<Cita> {
        var id = -1L
        fun c(dias: Long, h: Int, m: Int, cliente: String, tipo: Tipo, colonia: String, zona: Zona, lugar: String = "",
              tel: String = "", precio: Int? = null, anticipo: Int? = null, hecha: Boolean = false, pagada: Boolean = false) =
            Cita(
                id = id--, fecha = hoy.plusDays(dias), hora = LocalTime.of(h, m), cliente = cliente, tipo = tipo,
                colonia = colonia, zona = zona, lugar = lugar, telefono = tel, precio = precio, anticipo = anticipo,
                hecha = hecha, pagada = pagada, notas = "Cita de ejemplo",
            )
        return listOf(
            c(-3, 18, 0, "doña Lupe Canul", Tipo.REZO, "Pensiones", Zona.PONIENTE, "calle 33 x 70", "9991112233", 500, 200, hecha = true),
            c(-1, 19, 30, "familia Chan", Tipo.VELORIO, "Centro", Zona.CENTRO, "funeraria La Paz", "9994445566", 800, 800, hecha = true, pagada = true),
            c(0, 10, 0, "don Pedro Canché", Tipo.REZO, "Centro", Zona.CENTRO, "calle 60 x 45", "9997778899", 500, 200),
            c(0, 15, 0, "doña Mari Pech", Tipo.NOVENA, "Chuburná", Zona.NORTE, "calle 21 x 30 y 32", "9991234567", 600, 200),
            c(0, 19, 30, "familia Uc", Tipo.MISA, "Montebello", Zona.NORTE, "parroquia de Fátima", "9993216540", 700),
            c(1, 9, 0, "señora Rosario Dzul", Tipo.REZO, "Vergel", Zona.ORIENTE, "calle 18 #210", "9995551234", 450, 150),
            c(1, 18, 0, "doña Mari Pech", Tipo.NOVENA, "Chuburná", Zona.NORTE, "calle 21 x 30 y 32", "9991234567"),
            c(2, 17, 0, "don Ramón Ku", Tipo.REZO, "San José Tecoh", Zona.SUR, "calle 105 x 54", "9998887766", 500),
            c(4, 11, 0, "familia Cauich", Tipo.MISA, "Santiago", Zona.CENTRO, "iglesia de Santiago", "9996663322", 900, 300),
            c(6, 20, 0, "maestra Teresa Balam", Tipo.REZO, "Caucel", Zona.PONIENTE, "calle 59 x 104", "9992223344", 500),
        )
    }
}
