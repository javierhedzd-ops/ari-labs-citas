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
    val notas: String = "",
    val dudas: List<String> = emptyList(),
    val texto: String = "",
)

/**
 * Ordena una cita escrita como salga ("sábado 3pm, colonia Chuburná, doña Mari").
 * Todo con reglas, sin internet ni inteligencia artificial: lo que no entienda lo
 * deja como duda para que el tío lo confirme antes de guardar.
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

    private val reTelefono = Regex("""(?<!\d)(?:\+?52[\s-]?)?(?:\d[\s-]?){9}\d(?!\d)""")
    private val reAnticipo = Regex("""\b(?:anticipo|adelanto|dejo|abono|abonaron|dejaron)\s*(?:de\s*)?\$?\s*($NUM)(?:\s*pesos)?""")
    private val rePrecio = Regex(
        """\b(?:precio|cobro|cobra|cuesta|son|total|cobrar)\s*(?:de\s*)?\$?\s*($NUM)(?:\s*pesos)?""" +
            """|\$\s*($NUM)(?:\s*pesos)?""" +
            """|\b($NUM)\s*pesos\b""",
    )
    private val reHora = Regex(
        """(?:\b(?:a\s+)?las?\s+)?\b(\d{1,2})(?:[:.](\d{2}))?\s*""" +
            """(y\s+media|y\s+cuarto|menos\s+cuarto)?\s*""" +
            """(a\.?\s?m\.?|p\.?\s?m\.?|hrs|hr|h\b|de\s+la\s+manana|de\s+la\s+tarde|de\s+la\s+noche|de\s+la\s+madrugada|del\s+mediodia)?""",
    )
    private val reMediodia = Regex("""\b(?:al\s+|a\s+)?mediodia\b""")
    private val reFechaMes = Regex("""\b(?:el\s+)?(\d{1,2})\s+de\s+(${meses.joinToString("|")})(?:\s+(?:de\s+|del\s+)?(\d{4}))?\b""")
    private val reFechaBarra = Regex("""(?<![\d#])(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?(?!\d)""")
    private val reFechaDia = Regex("""\b(?:el\s+)?dia\s+(\d{1,2})\b""")
    private val reRelativa = Regex("""\b(pasado\s+manana|manana|hoy)\b""")
    private val reSemana = Regex("""\b(?:(este|el\s+proximo|proximo|el)\s+)?(${diasSemana.keys.joinToString("|")})(\s+que\s+viene)?\b""")
    private val reTipos = listOf(
        Regex("""\bnovenario\b|\bnovenas?\b""") to Tipo.NOVENA,
        Regex("""\bvelorios?\b|\bvelacion\b|\bvelada\b""") to Tipo.VELORIO,
        Regex("""\bmisas?\b""") to Tipo.MISA,
        Regex("""\brezos?\b|\brezar\b|\brosarios?\b|\bcabo\s+de\s+ano\b""") to Tipo.REZO,
    )
    private val reCliente = Regex(
        """\b(?:dona|don|sra\.?|sr\.?|senora|senor|srita\.?|senorita|familia|fam\.|hermana|hermano|""" +
            """maestra|maestro|profesora|profesor|profe|lic\.?|ing\.?|dra\.?|dr\.?|tia|tio|abuelita|abuelito)\s+[a-zn]+""",
    )
    private val reClienteSuave = Regex("""\b(?:con|para|casa\s+de|de\s+parte\s+de)\s+(?=[a-zn])""")
    private val reColonia = Regex("""\b(?:colonia|col\.|col|fraccionamiento|fracc\.|fracc|frac\.|frac|barrio|residencial)\s+""")
    private val reLugar = Regex(
        """\b(?:calle|c\.|num|numero|esquina|entre|cruzamientos|iglesia|parroquia|capilla|funeraria|""" +
            """panteon|cementerio|templo|catedral|casa|privada|av\.|avenida|periferico|km)\b|#|\d+\s*x\s*\d+""",
    )

    fun leer(texto: String, hoy: LocalDate): Borrador {
        val t = Texto(texto)
        val dudas = mutableListOf<String>()

        val telefono = t.tomar(reTelefono)?.let { m -> m.value.filter(Char::isDigit).takeLast(10) } ?: ""
        val anticipo = t.tomar(reAnticipo)?.let { pesos(it.groupValues[1]) }
        val precio = t.tomar(rePrecio)?.let { m ->
            pesos(m.groupValues.drop(1).first { it.isNotEmpty() })
        }

        val hora = leerHora(t, dudas)
        val fecha = leerFecha(t, hoy)

        val tipo = reTipos.firstNotNullOfOrNull { (re, tipo) -> t.tomar(re)?.let { tipo } }

        val cliente = t.tomar(reCliente)?.let { m -> t.hastaComa(m.range.first, incluirInicio = true) }
            ?: t.tomar(reClienteSuave)?.let { m -> t.hastaComa(m.range.last + 1) }?.takeIf { it.isNotBlank() }
            ?: ""

        var colonia = t.tomar(reColonia)?.let { m -> t.hastaComa(m.range.last + 1) }?.let(::limpiar) ?: ""
        if (colonia.isEmpty()) colonia = t.tomarColoniaConocida() ?: ""
        val zona = Colonias.zonaDe(colonia)

        val sobras = t.sobras().map(::limpiar).filter { s -> s.any(Char::isLetter) || s.any(Char::isDigit) }
        val lugar = sobras.firstOrNull { reLugar.containsMatchIn(Texto.plano(it)) } ?: ""
        var resto = sobras.filter { it != lugar }

        var clienteFinal = limpiar(cliente)
        if (clienteFinal.isEmpty()) {
            val candidato = resto.firstOrNull { s -> s.all { it.isLetter() || it == ' ' || it == '.' } }
            if (candidato != null) {
                clienteFinal = candidato
                resto = resto - candidato
                dudas += "¿La cita es con $candidato?"
            }
        }

        if (fecha == null) dudas += "¿Qué día es?"
        if (hora == null) dudas += "¿A qué hora?"
        if (tipo == null) dudas += "¿Es rezo, novena, velorio o misa?"
        if (clienteFinal.isEmpty()) dudas += "¿Con quién es la cita?"
        if (colonia.isNotEmpty() && zona == Zona.SIN_ZONA) dudas += "No conozco la colonia $colonia: escoge la zona."
        if (lugar.isEmpty() && colonia.isEmpty()) dudas += "¿Dónde es?"
        if (anticipo != null && precio != null && anticipo > precio) dudas += "El anticipo es mayor que el precio."

        return Borrador(
            fecha = fecha,
            hora = hora,
            cliente = clienteFinal,
            tipo = tipo,
            lugar = lugar,
            colonia = colonia,
            zona = zona,
            telefono = telefono,
            precio = precio,
            anticipo = anticipo,
            notas = resto.joinToString(", "),
            dudas = dudas,
            texto = texto.trim(),
        )
    }

    private fun leerHora(t: Texto, dudas: MutableList<String>): LocalTime? {
        t.tomar(reMediodia)?.let { return LocalTime.NOON }
        val m = t.tomar(reHora) { m ->
            // Sin "a las", minutos ni "pm/de la tarde" un número suelto es otra cosa (calle 60).
            val conLas = m.value.trimStart().startsWith("a ") || m.value.trimStart().startsWith("la")
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
            val anio = m.groupValues[3].toIntOrNull()
            return fechaValida(dia, mes, anio, hoy)
        }
        t.tomar(reFechaBarra)?.let { m ->
            val anio = m.groupValues[3].toIntOrNull()?.let { if (it < 100) 2000 + it else it }
            return fechaValida(m.groupValues[1].toInt(), m.groupValues[2].toInt(), anio, hoy)
        }
        t.tomar(reFechaDia)?.let { m ->
            val dia = m.groupValues[1].toInt()
            val esteMes = runCatching { hoy.withDayOfMonth(dia) }.getOrNull()
            return when {
                esteMes != null && !esteMes.isBefore(hoy) -> esteMes
                else -> runCatching { hoy.plusMonths(1).withDayOfMonth(dia) }.getOrNull()
            }
        }
        t.tomar(reRelativa)?.let { m ->
            return when (m.groupValues[1].replace(Regex("\\s+"), " ")) {
                "hoy" -> hoy
                "manana" -> hoy.plusDays(1)
                else -> hoy.plusDays(2)
            }
        }
        t.tomar(reSemana)?.let { m ->
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

        /** Desde [inicio] hasta la siguiente coma o algo ya usado; lo marca como usado. */
        fun hastaComa(inicio: Int, incluirInicio: Boolean = false): String {
            var fin = inicio
            while (fin < original.length && original[fin] !in ",;\n" && (!usado[fin] || (incluirInicio && fin < usadoHasta(inicio)))) fin++
            for (i in inicio until fin) usado[i] = true
            return original.substring(inicio, fin).trim()
        }

        private fun usadoHasta(inicio: Int): Int {
            var i = inicio
            while (i < original.length && usado[i]) i++
            return i
        }

        fun tomarColoniaConocida(): String? {
            val nombre = Colonias.buscarEn(sobras().joinToString(" , ")) ?: return null
            val re = Regex("""\b${Regex.escape(Colonias.normalizar(nombre))}\b""")
            // La versión plana conserva signos; se busca palabra por palabra.
            val m = re.find(plano.replace(Regex("[^a-z0-9 ]"), " ")) ?: return nombre
            if (m.range.any { usado[it] }) return nombre
            m.range.forEach { usado[it] = true }
            return nombre
        }

        fun sobras(): List<String> {
            val sb = StringBuilder()
            original.forEachIndexed { i, c -> sb.append(if (usado[i]) ',' else c) }
            return sb.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }
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
