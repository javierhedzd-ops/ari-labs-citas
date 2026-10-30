package mx.arilabs.citas.lector

import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.datos.Zona
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Lo que se entendió de un texto libre. Lo que falte o sea dudoso va en [dudas]. */
data class Borrador(
    val fecha: LocalDate? = null,
    val hora: LocalTime? = null,
    val cliente: String = "",
    val tipo: Tipo? = null,
    val lugar: String = "",
    val colonia: String = "",
    val zona: Zona = Zona.SIN_ZONA,
    val telefono: String = "",
    val precio: Int? = null,
    val anticipo: Int? = null,
    /** Minutos que dura, si se dijo ("2 horas", "hora y media"). */
    val duracion: Int? = null,
    val notas: String = "",
    val dudas: List<String> = emptyList(),
    val texto: String = "",
)

/**
 * Ordena una cita escrita como salga ("sábado 3pm, colonia Chuburná, doña Mari").
 * Todo con reglas, sin internet ni inteligencia artificial: lo que no entienda lo
 * deja como duda para que el tío lo confirme antes de guardar.
 *
 * El orden importa: primero lo que tiene forma fija (teléfono, dinero, dirección),
 * luego hora y fecha, luego el cliente (antes que el tipo, por "doña Rosario"), y al
 * final la colonia y lo que sobre.
 */
object LectorDeCitas {

    private val diasSemana = mapOf(
        "lunes" to DayOfWeek.MONDAY, "martes" to DayOfWeek.TUESDAY,
        "miercoles" to DayOfWeek.WEDNESDAY, "jueves" to DayOfWeek.THURSDAY,
        "viernes" to DayOfWeek.FRIDAY, "sabado" to DayOfWeek.SATURDAY,
        "domingo" to DayOfWeek.SUNDAY,
    )

    private val meses = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    private const val NUM = """\d{1,3}(?:,\d{3})+|\d{2,6}"""
    private const val HONORIFICO =
        """dona|don|sra\.?|sr\.?|senora|senor|srita\.?|senorita|familia|fam\.|hermana|hermano|""" +
            """maestra|maestro|profesora|profesor|profe|lic\.?|ing\.?|dra\.?|dr\.?|tia|tio|abuelita|abuelito"""

    // Formas comunes en México: 9991234567, 999 123 4567, 999-123-45-67, 99 9123 4567.
    // Nunca juntar números sueltos ("calle 32 9991234567" no es "32 99912345").
    private val reTelefono = Regex(
        """(?<![\d/#$,.])(?:\+?52[\s-]?)?""" +
            """(?:\d{10}|\d{3}[\s.-]\d{3}[\s.-]\d{4}|\d{3}[\s.-]\d{3}[\s.-]\d{2}[\s.-]\d{2}|\d{2}[\s.-]\d{4}[\s.-]\d{4})(?![\d])""",
    )
    private val reAnticipo = Regex("""\b(?:anticipo|adelanto|dejo|abono|abonaron|dejaron|apartaron|aparto)\s*(?:de\s*)?\$?\s*($NUM)(?:\s*pesos)?""")
    private val rePrecio = Regex(
        """\b(?:precio|cobro|cobra|cuesta|son|total|cobrar|cobre)\s*(?:de\s*)?\$?\s*($NUM)(?:\s*pesos)?""" +
            """|\$\s*($NUM)(?:\s*pesos)?""" +
            """|\b($NUM)\s*pesos\b""",
    )
    private val reCalle = Regex(
        """\b(?:calle|c\.|c)\s*\d{1,3}[a-z]?(?:\s*(?:#|num\.?|numero|no\.)\s*\d{1,4}[a-z]?)?""" +
            """(?:\s*(?:x|por)\s*\d{1,3}[a-z]?(?:\s*y\s*\d{1,3}[a-z]?)?)?""",
    )
    private val reSitio = Regex("""\b(?:iglesia|parroquia|capilla|funeraria|velatorio|panteon|cementerio|templo|catedral|salon)\b""")
    private val reAvenida = Regex("""\b(?:av\.|avenida|periferico|prolongacion|privada|carretera)\s+""")
    private val reHora = Regex(
        """(?:\b(?:a\s+)?las?\s+)?\b(\d{1,2})(?:[:.](\d{2}))?\s*""" +
            """(y\s+media|y\s+cuarto|menos\s+cuarto)?\s*""" +
            """((?:a\.?\s?m|p\.?\s?m)\b\.?|hrs|hr|h\b|de\s+la\s+manana|de\s+la\s+tarde|de\s+la\s+noche|de\s+la\s+madrugada|del\s+mediodia)?""",
    )
    private val reMediodia = Regex("""\b(?:al\s+|a\s+)?mediodia\b""")
    private val reFechaMes = Regex("""\b(?:el\s+)?(\d{1,2})\s+de\s+(${meses.joinToString("|")})(?:\s+(?:de\s+|del\s+)?(\d{4}))?\b""")
    private val reFechaBarra = Regex("""(?<![\d#])(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?(?!\d)""")
    private val reFechaDia = Regex("""\b(?:el\s+)?dia\s+(\d{1,2})\b""")
    private val reRelativa = Regex("""\b(pasado\s+manana|manana|hoy)\b""")
    private val reSemana = Regex("""\b(?:(este|el\s+proximo|proximo|el)\s+)?(${diasSemana.keys.joinToString("|")})(\s+que\s+viene)?\b""")
    private val reTrasHonorifico = Regex("""\b(?:$HONORIFICO)\s*$""")
    private val reTipos = listOf(
        Regex("""\bnovenario\b|\bnovenas?\b""") to Tipo.NOVENA,
        Regex("""\bvelorios?\b|\bvelacion\b|\bvelada\b""") to Tipo.VELORIO,
        Regex("""\bmisas?\b""") to Tipo.MISA,
        Regex("""\bmisas?\b|\bbautizos?\b|\bprimera\s+comunion\b|\bcomunion\b|\bconfirmacion\b""") to Tipo.MISA,
        Regex("""\brezos?\b|\brezar\b|\brosarios?\b|\bcabo\s+de\s+ano\b""") to Tipo.REZO,
        Regex("""\bxv\b|\bquince\s+anos\b|\bquinceanera\b|\b15\s+anos\b""") to Tipo.XV,
        Regex("""\bbodas?\b|\bcasamiento\b""") to Tipo.BODA,
        Regex("""\bfiestas?\b|\bcumpleanos\b|\bcumple\b|\bposada\b|\bgraduacion\b|\bevento\b|\brecepcion\b""") to Tipo.FIESTA,
    )
    // "2 horas", "hora y media", "media hora", "90 minutos". "15 hrs" sigue siendo una hora del día.
    private val reDuracion = Regex(
        """\b(?:por\s+|de\s+|durante\s+)?(?:(hora\s+y\s+media)|(media\s+hora)|(una|dos|tres|\d(?:\.5)?)\s+horas?\b|(\d{2,3})\s*(?:minutos|min)\b)""",
    )
    private val reCliente = Regex("""\b(?:$HONORIFICO)\s+(?=[a-z])""")
    private val reClienteSuave = Regex("""\b(?:con|para|casa\s+de|de\s+parte\s+de)\s+(?=[a-z])""")
    private val reColonia = Regex("""\b(?:colonia|col\.|col|fraccionamiento|fracc\.|fracc|frac\.|frac|barrio|residencial)\s+""")

    /** Palabras que cortan un nombre de cliente o de colonia. */
    private val paradas = setOf(
        "colonia", "col", "fracc", "fraccionamiento", "frac", "barrio", "residencial",
        "calle", "c", "av", "avenida", "privada", "periferico", "x", "por", "num", "numero",
        "en", "a", "al", "para", "con", "tel", "cel", "telefono", "celular", "whatsapp",
        "hoy", "manana", "pasado", "el", "este", "proximo",
        "rezo", "rezos", "rezar", "rosario", "novena", "novenas", "novenario", "velorio", "velorios",
        "misa", "misas", "cabo", "boda", "bodas", "xv", "fiesta", "cumpleanos", "bautizo", "posada", "evento", "anticipo", "adelanto", "cobro", "cobra", "precio", "dejo", "pesos",
        "iglesia", "parroquia", "capilla", "funeraria", "panteon", "cementerio", "casa",
    ) + diasSemana.keys + meses
    private val conectores = setOf("de", "del", "la", "los", "las", "y")

    fun leer(texto: String, hoy: LocalDate): Borrador {
        val t = Texto(texto)
        val dudas = mutableListOf<String>()

        val telefono = t.tomar(reTelefono)?.let { m -> m.value.filter(Char::isDigit).takeLast(10) } ?: ""
        val anticipo = t.tomar(reAnticipo)?.let { pesos(it.groupValues[1]) }
        val precio = t.tomar(rePrecio)?.let { m -> pesos(m.groupValues.drop(1).first { it.isNotEmpty() }) }

        val duracion = t.tomar(reDuracion)?.let { m ->
            when {
                m.groupValues[1].isNotEmpty() -> 90
                m.groupValues[2].isNotEmpty() -> 30
                m.groupValues[3].isNotEmpty() -> when (m.groupValues[3]) {
                    "una" -> 60; "dos" -> 120; "tres" -> 180
                    else -> (m.groupValues[3].toDouble() * 60).toInt()
                }
                else -> m.groupValues[4].toInt()
            }
        }?.let { min ->
            if (min > 120) { dudas += "Máximo 2 horas por evento: lo dejé en 2 horas."; 120 } else min
        }

        val lugares = mutableListOf<String>()
        t.tomar(reCalle)?.let { lugares += t.original.substring(it.range).trim() }

        val hora = leerHora(t, dudas)
        val fecha = leerFecha(t, hoy)

        // El cliente antes que el tipo: "doña Rosario" no es un rosario.
        var cliente = t.tomar(reCliente)?.let { m ->
            val nombre = t.palabras(m.range.last + 1, maximo = 4, esNombre = true)
            if (nombre.isEmpty()) "" else t.original.substring(m.range.first, m.range.last + 1).trim() + " " + nombre
        } ?: ""

        val tipo = reTipos.firstNotNullOfOrNull { (re, tipo) -> t.tomar(re)?.let { tipo } }

        t.tomar(reSitio)?.let { m ->
            val resto = t.palabras(m.range.last + 1, maximo = 4, esNombre = false, conectoresAlInicio = true)
            lugares += listOf(t.original.substring(m.range).trim(), resto).filter { it.isNotBlank() }.joinToString(" ")
        }
        t.tomar(reAvenida)?.let { m ->
            val resto = t.palabras(m.range.last + 1, maximo = 5, esNombre = false, numeros = true)
            lugares += (t.original.substring(m.range).trim() + " " + resto).trim()
        }

        if (cliente.isEmpty()) {
            cliente = t.tomar(reClienteSuave)?.let { m -> t.palabras(m.range.last + 1, maximo = 4, esNombre = true) } ?: ""
        }

        var colonia = t.tomar(reColonia)?.let { m -> t.palabras(m.range.last + 1, maximo = 4, esNombre = false, numeros = true) } ?: ""
        if (colonia.isEmpty()) colonia = t.tomarColoniaConocida() ?: ""
        // "iglesia de Santiago": el lugar ya dice el barrio.
        if (colonia.isEmpty()) colonia = lugares.firstNotNullOfOrNull { Colonias.buscarEn(it) } ?: ""
        // Sin quitar "La" de "La Mejorada"; y con el nombre completo si se conoce ("Colonia Yucatán").
        colonia = colonia.trim().trim(',', ';', '.', ':', '-', ' ')
        colonia = Colonias.canonica(colonia) ?: colonia
        val zona = Colonias.zonaDe(colonia)

        var resto = t.sobras().map(::limpiar).filter { s -> s.any(Char::isLetterOrDigit) && Texto.plano(s) !in paradas }

        if (cliente.isEmpty()) {
            val candidato = resto.firstOrNull { s ->
                val palabras = s.split(' ')
                palabras.size in 1..5 && s.all { it.isLetter() || it == ' ' || it == '.' } &&
                    palabras.any { Texto.plano(it) !in paradas && Texto.plano(it) !in conectores }
            }
            if (candidato != null) {
                cliente = candidato
                resto = resto - candidato
                dudas += "¿La cita es con $candidato?"
            }
        }

        val lugar = lugares.filter { it.isNotBlank() }.joinToString(", ")

        if (fecha == null) dudas += "¿Qué día es?"
        if (hora == null) dudas += "¿A qué hora?"
        if (tipo == null) dudas += "¿Qué evento es? (misa, boda, XV, rezo…)"
        if (cliente.isEmpty()) dudas += "¿Con quién es la cita?"
        if (colonia.isNotEmpty() && zona == Zona.SIN_ZONA) dudas += "No conozco la colonia $colonia: escoge la zona."
        if (lugar.isEmpty() && colonia.isEmpty()) dudas += "¿Dónde es?"
        if (anticipo != null && precio != null && anticipo > precio) dudas += "El anticipo es mayor que el precio."

        return Borrador(
            fecha = fecha,
            hora = hora,
            cliente = limpiar(cliente),
            tipo = tipo,
            lugar = lugar,
            colonia = colonia,
            zona = zona,
            telefono = telefono,
            precio = precio,
            anticipo = anticipo,
            duracion = duracion,
            notas = resto.joinToString(", "),
            dudas = dudas,
            texto = texto.trim(),
        )
    }

    private fun leerHora(t: Texto, dudas: MutableList<String>): LocalTime? {
        t.tomar(reMediodia)?.let { return LocalTime.NOON }
        val m = t.tomar(reHora) { m ->
            // Sin "a las", minutos ni "pm/de la tarde" un número suelto es otra cosa (calle 60).
            val v = m.value.trimStart()
            val conLas = v.startsWith("a ") || v.startsWith("la")
            val h = m.groupValues[1].toInt()
            h in 0..23 && (conLas || m.groupValues[2].isNotEmpty() || m.groupValues[3].isNotEmpty() || m.groupValues[4].isNotEmpty())
        } ?: return null

        var h = m.groupValues[1].toInt()
        var min = m.groupValues[2].ifEmpty { "0" }.toInt()
        when (m.groupValues[3].replace(Regex("\\s+"), " ")) {
            "y media" -> min = 30
            "y cuarto" -> min = 15
            "menos cuarto" -> { h -= 1; min = 45 }
        }
        val marca = m.groupValues[4].replace(Regex("[\\s.]"), "")
        val tarde = marca == "pm" || marca.endsWith("tarde") || marca.endsWith("noche")
        val manana = marca == "am" || marca.endsWith("manana") || marca.endsWith("madrugada")
        when {
            tarde && h < 12 -> h += 12
            manana && h == 12 -> h = 0
            marca.endsWith("mediodia") -> h = 12
            marca.isEmpty() && h in 1..7 -> h += 12
            marca.isEmpty() && h in 8..11 -> dudas += "¿Las $h de la mañana o de la noche?"
        }
        if (h !in 0..23 || min !in 0..59) return null
        return LocalTime.of(h, min)
    }

    private fun leerFecha(t: Texto, hoy: LocalDate): LocalDate? {
        t.tomar(reFechaMes)?.let { m ->
            val dia = m.groupValues[1].toInt()
            val mes = meses.indexOf(m.groupValues[2]) + 1
            return fechaValida(dia, mes, m.groupValues[3].toIntOrNull(), hoy)
        }
        t.tomar(reFechaBarra)?.let { m ->
            val anio = m.groupValues[3].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            return fechaValida(m.groupValues[1].toInt(), m.groupValues[2].toInt(), anio, hoy)
        }
        t.tomar(reFechaDia)?.let { m ->
            val dia = m.groupValues[1].toInt()
            val esteMes = runCatching { hoy.withDayOfMonth(dia) }.getOrNull()
            return if (esteMes != null && !esteMes.isBefore(hoy)) esteMes
            else runCatching { hoy.plusMonths(1).withDayOfMonth(dia) }.getOrNull()
        }
        t.tomar(reRelativa) { m -> !t.precedidoPor(m.range.first, reTrasHonorifico) }?.let { m ->
            return when (m.groupValues[1].replace(Regex("\\s+"), " ")) {
                "hoy" -> hoy
                "manana" -> hoy.plusDays(1)
                else -> hoy.plusDays(2)
            }
        }
        // "don Domingo" es una persona, no un día.
        t.tomar(reSemana) { m -> !t.precedidoPor(m.range.first, reTrasHonorifico) }?.let { m ->
            val dia = diasSemana.getValue(m.groupValues[2])
            var dias = (dia.value - hoy.dayOfWeek.value + 7) % 7
            val proximo = m.groupValues[1].contains("proximo") || m.groupValues[3].isNotEmpty()
            if (dias == 0 && proximo) dias = 7
            return hoy.plusDays(dias.toLong())
        }
        return null
    }

    private fun fechaValida(dia: Int, mes: Int, anio: Int?, hoy: LocalDate): LocalDate? {
        val f = runCatching { LocalDate.of(anio ?: hoy.year, mes, dia) }.getOrNull() ?: return null
        return if (anio == null && f.isBefore(hoy)) runCatching { f.plusYears(1) }.getOrNull() else f
    }

    private fun pesos(s: String): Int? = s.replace(",", "").toIntOrNull()

    /** Quita comas, puntos sueltos y palabras de relleno de las orillas. */
    internal fun limpiar(s: String): String {
        var r = s.trim().trim(',', ';', '.', ':', '-', ' ')
        val relleno = Regex("""^(?:y|en|el|la|de|del|a|con|para|por)\s+|\s+(?:y|en|el|la|de|del|a|con|para|por)$""", RegexOption.IGNORE_CASE)
        repeat(3) { r = r.replace(relleno, "").trim().trim(',', ';', '.', ':', '-', ' ') }
        if (r.lowercase() in setOf("y", "en", "el", "la", "de", "del", "a", "con", "para", "por", "las", "los")) return ""
        return r.replace(Regex("\\s+"), " ")
    }

    /**
     * El texto original y su versión "plana" (minúsculas y sin acentos, mismo largo) para
     * buscar con reglas. Lo que ya se entendió se marca como usado.
     */
    private class Texto(val original: String) {
        val plano = plano(original)
        val usado = BooleanArray(original.length)

        fun tomar(re: Regex, acepta: (MatchResult) -> Boolean = { true }): MatchResult? {
            var m = re.find(plano)
            while (m != null) {
                if (m.value.isNotBlank() && m.range.none { usado[it] } && acepta(m)) {
                    m.range.forEach { usado[it] = true }
                    return m
                }
                m = m.next()
            }
            return null
        }

        fun precedidoPor(inicio: Int, re: Regex): Boolean = re.containsMatchIn(plano.substring(maxOf(0, inicio - 12), inicio))

        /**
         * Toma hasta [maximo] palabras desde [inicio] y las marca como usadas. Se detiene en
         * una coma, un punto final, algo ya usado, una palabra de [paradas] o, si [esNombre],
         * en un número o en el nombre de una colonia conocida.
         */
        fun palabras(
            inicio: Int,
            maximo: Int,
            esNombre: Boolean,
            numeros: Boolean = false,
            conectoresAlInicio: Boolean = false,
        ): String {
            val tomadas = mutableListOf<IntRange>()
            var i = inicio
            var cuenta = 0
            while (cuenta < maximo) {
                while (i < plano.length && plano[i] == ' ' && !usado[i]) i++
                if (i >= plano.length || usado[i] || plano[i] in ",;\n") break
                var fin = i
                while (fin < plano.length && plano[fin] != ' ' && plano[fin] !in ",;\n" && !usado[fin]) fin++
                var palabra = plano.substring(i, fin)
                val cierra = palabra.endsWith(".") && palabra.length > 2
                palabra = palabra.trimEnd('.', ':')
                if (palabra.isEmpty()) break
                val esNumero = palabra.any(Char::isDigit)
                // Tras "doña" la primera palabra siempre es nombre: "doña Rosario", "don Domingo".
                if (palabra in paradas && !(esNombre && cuenta == 0)) break
                if (esNumero && (esNombre || !numeros)) break
                if (!esNumero && palabra.any { !it.isLetter() && it != '.' && it != '\'' }) break
                if (esNombre && cuenta > 0 && Colonias.empiezaEn(plano, i)) break
                if (palabra in conectores && !(conectoresAlInicio && cuenta == 0)) {
                    // Un conector solo vale si después viene otra palabra útil ("María de la Cruz").
                    val siguiente = siguientePalabra(fin)
                    if (siguiente == null || siguiente in paradas || siguiente.any(Char::isDigit)) break
                }
                tomadas += i until (if (cierra) fin - 1 else fin)
                cuenta++
                i = fin
                if (cierra) break
            }
            // No dejar un conector colgando al final.
            while (tomadas.isNotEmpty() && plano.substring(tomadas.last()).trimEnd('.') in conectores) tomadas.removeAt(tomadas.lastIndex)
            if (tomadas.isEmpty()) return ""
            val desde = tomadas.first().first
            val hasta = tomadas.last().last
            for (k in desde..hasta) usado[k] = true
            return original.substring(desde, hasta + 1).trim()
        }

        private fun siguientePalabra(desde: Int): String? {
            var i = desde
            while (i < plano.length && plano[i] == ' ') i++
            if (i >= plano.length || usado[i] || plano[i] in ",;\n") return null
            var fin = i
            while (fin < plano.length && plano[fin] != ' ' && plano[fin] !in ",;\n" && !usado[fin]) fin++
            return plano.substring(i, fin).trimEnd('.')
        }

        fun tomarColoniaConocida(): String? {
            val limpio = plano.replace(Regex("[^a-z0-9 ]"), " ")
            for ((nombre, normal) in Colonias.porLargo()) {
                val patron = normal.split(' ').joinToString("""\s+""") { Regex.escape(it) }
                var m = Regex("""\b$patron\b""").find(limpio)
                while (m != null) {
                    if (m.range.none { usado[it] }) {
                        m.range.forEach { usado[it] = true }
                        return nombre
                    }
                    m = m.next()
                }
            }
            return null
        }

        fun sobras(): List<String> {
            val sb = StringBuilder()
            original.forEachIndexed { i, c -> sb.append(if (usado[i]) ',' else c) }
            return sb.split(Regex("""[,;\n]|\.\s""")).map { it.trim() }.filter { it.isNotEmpty() }
        }

        companion object {
            fun plano(s: String): String = buildString(s.length) {
                for (c in s) {
                    append(
                        when (c.lowercaseChar()) {
                            'á', 'à', 'ä', 'â' -> 'a'
                            'é', 'è', 'ë', 'ê' -> 'e'
                            'í', 'ì', 'ï', 'î' -> 'i'
                            'ó', 'ò', 'ö', 'ô' -> 'o'
                            'ú', 'ù', 'ü', 'û' -> 'u'
                            'ñ' -> 'n'
                            else -> c.lowercaseChar()
                        },
                    )
                }
            }
        }
    }
}
