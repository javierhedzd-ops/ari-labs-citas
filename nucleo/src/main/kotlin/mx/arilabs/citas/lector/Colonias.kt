package mx.arilabs.citas.lector

import mx.arilabs.citas.datos.Zona
import java.text.Normalizer

/**
 * Colonias de Mérida y alrededores con su zona aproximada, para ordenar el día sin cruzar
 * la ciudad. Es una lista a mano: si una colonia no está, la cita queda "Sin zona" y el tío
 * la escoge al guardar. Revisar con él la primera semana.
 */
object Colonias {

    private val lista: Map<Zona, List<String>> = mapOf(
        Zona.CENTRO to listOf(
            "Centro", "Santiago", "Santa Ana", "Santa Lucía", "San Juan", "San Sebastián",
            "La Mejorada", "San Cristóbal", "La Ermita", "Mejorada", "Itzimná", "Alemán",
            "Nueva Alemán", "Miguel Alemán", "García Ginerés", "Jesús Carranza",
        ),
        Zona.NORTE to listOf(
            "Chuburná", "Chuburná de Hidalgo", "Montebello", "Montes de Amé", "Altabrisa",
            "Francisco de Montejo", "México", "México Norte", "Benito Juárez Norte",
            "Buenavista", "San Ramón Norte", "Campestre", "Las Américas", "Cordemex",
            "Temozón Norte", "Dzityá", "Cholul", "Conkal", "Chablekal", "Komchén",
            "Sodzil Norte", "Gonzalo Guerrero", "Vista Alegre", "Pensiones Norte",
            "San Antonio Cucul", "Santa Gertrudis Copó", "Xcumpich", "Loma Bonita",
            "Prado Norte", "Real Montejo", "Colonia Yucatán",
        ),
        Zona.ORIENTE to listOf(
            "Vergel", "Fraccionamiento del Parque", "Pacabtún", "Los Héroes", "Kanasín",
            "Polígono 108", "Jardines del Este", "Salvador Alvarado Oriente", "Maya",
            "Fidel Velázquez", "Juan B. Sosa", "Mayapán", "Brisas del Bosque",
            "Gran Santa Fe", "Morelos Oriente", "Santa Rosa", "Los Pinos", "Miraflores",
        ),
        Zona.SUR to listOf(
            "San José Tecoh", "Emiliano Zapata Sur", "Serapio Rendón", "Castilla Cámara",
            "San Antonio Xluch", "Dzununcán", "Xmatkuil", "Plan de Ayala Sur",
            "Delio Moreno Cantón", "Cinco Colonias", "Cortés Sarmiento", "Santa Rosa Sur",
            "Mercedes Barrera", "Nueva Mulsay", "Umán", "Tixcacal", "Mulchechén",
        ),
        Zona.PONIENTE to listOf(
            "Caucel", "Ciudad Caucel", "Xoclán", "Bojórquez", "Pensiones", "Jardines de Pensiones",
            "Juan Pablo II", "Mulsay", "Chenkú", "Yucalpetén", "Francisco I. Madero",
            "Obrera", "Inalámbrica", "Susulá", "Las Granjas", "Nora Quintana",
        ),
    )

    /** Nombre normalizado -> (nombre bonito, zona). Las más largas primero para que gane la más exacta. */
    private val indice: List<Triple<String, String, Zona>> =
        lista.flatMap { (zona, nombres) -> nombres.map { Triple(normalizar(it), it, zona) } }
            .sortedByDescending { it.first.length }

    fun zonaDe(colonia: String): Zona {
        val n = normalizar(colonia)
        if (n.isBlank()) return Zona.SIN_ZONA
        return indice.firstOrNull { it.first == n }?.third
            ?: indice.firstOrNull { (" $n ").contains(" ${it.first} ") }?.third
            // "Parque" por "Fraccionamiento del Parque", "Yucatán" por "Colonia Yucatán".
            ?: indice.firstOrNull { n.length >= 5 && it.first.endsWith(" $n") }?.third
            ?: Zona.SIN_ZONA
    }

    /** El nombre completo de una colonia conocida: "yucatan" -> "Colonia Yucatán". Null si no se conoce. */
    fun canonica(colonia: String): String? {
        val n = normalizar(colonia)
        if (n.isBlank()) return null
        return indice.firstOrNull { it.first == n }?.second
            ?: indice.firstOrNull { n.length >= 5 && it.first.endsWith(" $n") }?.second
    }

    /** Todas las colonias con su zona (para pruebas y para la lista de la app). */
    fun todas(): List<Pair<String, Zona>> = indice.map { it.second to it.third }

    /** (nombre bonito, nombre normalizado), del más largo al más corto. */
    fun porLargo(): List<Pair<String, String>> = indice.map { it.second to it.first }

    /** ¿Empieza el nombre de una colonia conocida en la posición [i] de un texto plano? */
    fun empiezaEn(plano: String, i: Int): Boolean {
        val resto = plano.substring(i).replace(Regex("[^a-z0-9 ]"), " ") + " "
        return indice.any { it.first.length >= 5 && resto.startsWith(it.first + " ") }
    }

    /** Busca una colonia conocida dentro de un texto libre. Devuelve el nombre bonito. */
    fun buscarEn(texto: String): String? {
        val n = " " + normalizar(texto) + " "
        return indice.firstOrNull { n.contains(" " + it.first + " ") }?.second
    }

    fun normalizar(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
