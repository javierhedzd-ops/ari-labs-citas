package mx.arilabs.citas.lector

import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.datos.Zona
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

/**
 * 1000 usuarios simulados. Cada uno escribe una cita a su manera (orden, acentos,
 * mayúsculas, separadores, formatos de hora, fecha, teléfono y dinero) y se compara
 * lo que entendió el lector con lo que la persona quiso decir.
 *
 * Lo que más importa es el "error callado": un dato entendido mal SIN preguntar.
 * Un dato que el lector no entiende pero pregunta no es grave: el tío lo corrige.
 *
 * No sustituye a personas reales: mide el lector contra formas de escribir que se
 * conocen. Las que no se conozcan saldrán al probar con gente.
 */
class SimulacionMilUsuariosTest {

    private val usuarios = 1000
    private val r = Random(2026)

    private val campos = listOf("fecha", "hora", "tipo", "cliente", "colonia", "zona", "lugar", "telefono", "precio", "anticipo")
    private val bien = campos.associateWith { 0 }.toMutableMap()
    private val preguntado = campos.associateWith { 0 }.toMutableMap()
    private val callados = campos.associateWith { 0 }.toMutableMap()
    private val total = campos.associateWith { 0 }.toMutableMap()
    private val ejemplosCallados = mutableListOf<String>()
    private var perfectas = 0

    private data class Esperado(
        val texto: String,
        val hoy: LocalDate,
        val fecha: LocalDate?,
        val hora: LocalTime?,
        val horaDudosa: Boolean,
        val tipo: Tipo?,
        val cliente: String?,
        val clienteSinTitulo: Boolean,
        val colonia: String?,
        val zona: Zona?,
        val lugar: String?,
        val telefono: String?,
        val precio: Int?,
        val anticipo: Int?,
    )

    @Test
    fun milUsuarios() {
        repeat(usuarios) { evaluar(generar()) }

        val reporte = buildString {
            appendLine("Simulación de $usuarios usuarios")
            appendLine("Citas entendidas completas sin preguntar nada: $perfectas de $usuarios")
            appendLine()
            appendLine("campo      | casos | bien  | preguntó | mal sin avisar")
            for (c in campos) {
                val n = total.getValue(c)
                if (n == 0) continue
                appendLine(
                    "%-10s | %5d | %4.1f%% | %7.1f%% | %5.1f%%".format(
                        c, n, 100.0 * bien.getValue(c) / n, 100.0 * preguntado.getValue(c) / n, 100.0 * callados.getValue(c) / n,
                    ),
                )
            }
            if (ejemplosCallados.isNotEmpty()) {
                appendLine()
                appendLine("Ejemplos de errores sin avisar:")
                ejemplosCallados.take(40).forEach { appendLine("- $it") }
            }
        }
        println(reporte)
        runCatching {
            File("build/reports").mkdirs()
            File("build/reports/simulacion-1000.txt").writeText(reporte)
        }

        // Límites: casi nunca entender mal sin avisar.
        val limites = mapOf(
            "fecha" to 1.0, "hora" to 1.0, "telefono" to 0.5, "precio" to 1.0, "anticipo" to 1.0,
            "tipo" to 1.0, "cliente" to 3.0, "colonia" to 3.0, "zona" to 2.0, "lugar" to 5.0,
        )
        for ((c, max) in limites) {
            val n = total.getValue(c).coerceAtLeast(1)
            val pct = 100.0 * callados.getValue(c) / n
            assertTrue("Demasiados errores sin avisar en $c: " + "%.1f".format(pct) + "% (máximo $max%)\n" + reporte, pct <= max)
        }
    }

    // ------------------------------------------------------------ evaluar

    private fun evaluar(e: Esperado) {
        val b = try {
            LectorDeCitas.leer(e.texto, e.hoy)
        } catch (t: Throwable) {
            throw AssertionError("El lector se cayó con: \"${e.texto}\"", t)
        }
        val d = b.dudas
        var perfecta = d.isEmpty()

        fun marca(campo: String, ok: Boolean, pregunto: Boolean, detalle: () -> String) {
            total[campo] = total.getValue(campo) + 1
            when {
                ok && !pregunto -> bien[campo] = bien.getValue(campo) + 1
                pregunto -> { preguntado[campo] = preguntado.getValue(campo) + 1; perfecta = false }
                else -> {
                    callados[campo] = callados.getValue(campo) + 1
                    perfecta = false
                    ejemplosCallados += "[$campo] \"${e.texto.replace("\n", " / ")}\" → ${detalle()}"
                }
            }
        }

        // Fecha
        if (e.fecha != null) marca("fecha", b.fecha == e.fecha, d.any { it.startsWith("¿Qué día") }) { "fecha ${b.fecha}, esperaba ${e.fecha}" }
        else marca("fecha", b.fecha == null, d.any { it.startsWith("¿Qué día") }) { "inventó fecha ${b.fecha}" }

        // Hora (si era dudosa, debe preguntar y eso cuenta como bien)
        if (e.hora != null) {
            val pregunto = d.any { it.startsWith("¿A qué hora") || it.startsWith("¿Las") }
            if (e.horaDudosa) marca("hora", b.hora == e.hora && pregunto, false) { "hora ${b.hora}, esperaba ${e.hora} preguntando" }
            else marca("hora", b.hora == e.hora, pregunto && b.hora != e.hora) { "hora ${b.hora}, esperaba ${e.hora}" }
        } else marca("hora", b.hora == null, d.any { it.startsWith("¿A qué hora") }) { "inventó hora ${b.hora}" }

        // Tipo
        marca("tipo", b.tipo == e.tipo, e.tipo == null && b.tipo == null && d.any { it.startsWith("¿Qué evento") }) { "tipo ${b.tipo}, esperaba ${e.tipo}" }

        // Cliente
        if (e.cliente != null) {
            val ok = n(b.cliente) == n(e.cliente)
            val pregunto = d.any { it.startsWith("¿La cita es con") || it.startsWith("¿Con quién") }
            if (e.clienteSinTitulo) marca("cliente", ok && pregunto, !ok && pregunto) { "cliente \"${b.cliente}\", esperaba \"${e.cliente}\"" }
            else marca("cliente", ok, !ok && pregunto) { "cliente \"${b.cliente}\", esperaba \"${e.cliente}\"" }
        }

        // Colonia y zona
        if (e.colonia != null) {
            val ok = n(b.colonia) == n(e.colonia)
            val pregunto = d.any { it.startsWith("No conozco la colonia") }
            marca("colonia", ok, !ok && pregunto) { "colonia \"${b.colonia}\", esperaba \"${e.colonia}\"" }
            if (e.zona == Zona.SIN_ZONA) marca("zona", b.zona == Zona.SIN_ZONA && pregunto, false) { "zona ${b.zona} para colonia desconocida" }
            else marca("zona", b.zona == e.zona, b.zona == Zona.SIN_ZONA && pregunto) { "zona ${b.zona}, esperaba ${e.zona} (${e.colonia})" }
        }

        // Lugar: basta con que contenga la dirección
        if (e.lugar != null) {
            val ok = n(b.lugar).contains(n(e.lugar))
            marca("lugar", ok, false) { "lugar \"${b.lugar}\", esperaba \"${e.lugar}\"" }
        }

        // Teléfono y dinero: no deben aparecer si no se dijeron
        marca("telefono", b.telefono == (e.telefono ?: ""), false) { "teléfono \"${b.telefono}\", esperaba \"${e.telefono}\"" }
        marca("precio", b.precio == e.precio, false) { "precio ${b.precio}, esperaba ${e.precio}" }
        marca("anticipo", b.anticipo == e.anticipo, false) { "anticipo ${b.anticipo}, esperaba ${e.anticipo}" }

        if (perfecta) perfectas++
    }

    private fun n(s: String) = Colonias.normalizar(s.replace(".", " "))

    // ------------------------------------------------------------ generar

    private val dias = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val meses = listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
    private val titulos = listOf("doña", "don", "sra.", "señora", "familia", "la señora", "maestra", "profe", "hermana")
    private val nombres = listOf(
        "Mari", "Rosario", "Lupe", "Guadalupe", "Domingo", "Pedro Canché", "Chucho", "Rosa May", "José Dzul",
        "Concepción Uc", "Filomena Tun", "Santiago Chi", "Socorro", "María de los Ángeles", "Pech", "Canul",
        "Cauich", "Aurora Euán", "Juanita", "Teresa Balam", "Ramón Ku", "Lucía Chan", "Martha Poot", "Isabel Cen",
        "Carmen", "Gloria Puc", "Elsy", "Reyna Pat", "Mercedes", "Ángela Caamal",
    )
    private val sinTitulo = listOf("Mari Pech", "Lupe Canul", "Rosa Chan", "Martha Poot", "Elsy Uc")
    private val desconocidas = listOf("Las Palmas", "San Pedro Uxmal", "Los Almendros", "Villa Magna", "Ciudad Industrial")
    private val calles = listOf("calle 21 x 30 y 32", "c. 60 #512 x 45", "calle 50 num 312 por 47", "calle 8 x 13", "calle 115 #430 x 52 y 54")
    private val sitios = listOf("funeraria Pérez", "capilla del Carmen", "funeraria La Paz", "parroquia de Fátima")

    private fun <T> Random.de(l: List<T>) = l[nextInt(l.size)]
    private fun Random.prob(p: Double) = nextDouble() < p

    private fun generar(): Esperado {
        val hoy = LocalDate.of(2026, 11, 2).plusDays(r.nextLong(0, 60))
        val partes = mutableListOf<String>()

        // Fecha
        var fecha: LocalDate? = null
        if (!r.prob(0.04)) {
            when (r.nextInt(8)) {
                0 -> { partes += "hoy"; fecha = hoy }
                1 -> { partes += "mañana"; fecha = hoy.plusDays(1) }
                2 -> { partes += "pasado mañana"; fecha = hoy.plusDays(2) }
                3, 4 -> {
                    val dow = DayOfWeek.of(r.nextInt(1, 8))
                    val estilo = r.nextInt(4)
                    partes += listOf("", "el ", "este ", "el próximo ")[estilo] + dias[dow.value - 1]
                    var d = (dow.value - hoy.dayOfWeek.value + 7) % 7
                    if (d == 0 && estilo == 3) d = 7
                    fecha = hoy.plusDays(d.toLong())
                }
                5 -> {
                    val f = hoy.plusDays(r.nextLong(1, 50))
                    partes += "el ${f.dayOfMonth} de ${meses[f.monthValue - 1]}"; fecha = f
                }
                6 -> {
                    val f = hoy.plusDays(r.nextLong(1, 50))
                    partes += "${f.dayOfMonth}/${f.monthValue}"; fecha = f
                }
                else -> {
                    val f = hoy.plusDays(r.nextLong(0, 20))
                    partes += "el día ${f.dayOfMonth}"
                    fecha = f
                }
            }
        }

        // Hora
        var hora: LocalTime? = null
        var dudosa = false
        if (!r.prob(0.04)) {
            val h = r.nextInt(8, 22)
            val h12 = if (h > 12) h - 12 else h
            val tarde = h >= 12
            val parte = when {
                h < 12 -> "de la mañana"
                h < 19 -> "de la tarde"
                else -> "de la noche"
            }
            val media = r.prob(0.25)
            val mm = if (media) 30 else 0
            val opciones = mutableListOf(
                "${h12}${if (media) ":30" else ""}${if (tarde) "pm" else "am"}" to false,
                "${h12}${if (media) ":30" else ""} ${if (tarde) "p.m." else "a.m."}" to false,
                "a las $h12${if (media) " y media" else ""} $parte" to false,
                "%d:%02d".format(h, mm) to false,
                "a las %d:%02d hrs".format(h, mm) to false,
            )
            if (h12 in 1..7 && tarde) opciones += "a las $h12${if (media) " y media" else ""}" to false
            if (h12 in 8..11 && !tarde) opciones += "a las $h12${if (media) " y media" else ""}" to true
            if (h == 12 && !media) opciones += "mediodía" to false
            val (texto, ambigua) = r.de(opciones)
            partes += texto
            hora = LocalTime.of(h, mm)
            dudosa = ambigua
        }

        // Tipo
        var tipo: Tipo? = null
        if (!r.prob(0.08)) {
            val (palabra, t) = r.de(
                listOf("rezo" to Tipo.REZO, "rosario" to Tipo.REZO, "cabo de año" to Tipo.REZO, "novena" to Tipo.NOVENA,
                    "novenario" to Tipo.NOVENA, "velorio" to Tipo.VELORIO, "misa" to Tipo.MISA),
            )
            partes += palabra; tipo = t
        }

        // Cliente
        val cliente: String
        val clienteSinTitulo = r.prob(0.12)
        if (clienteSinTitulo) {
            cliente = r.de(sinTitulo); partes += cliente
        } else {
            val tit = r.de(titulos)
            val escrito = "$tit ${r.de(nombres)}"
            partes += escrito
            // "la señora Mari": el artículo no es parte del nombre.
            cliente = escrito.removePrefix("la ")
        }

        // Colonia
        var colonia: String? = null
        var zona: Zona? = null
        val tiraColonia = r.nextDouble()
        when {
            tiraColonia < 0.80 -> {
                val (nombre, z) = r.de(Colonias.todas())
                colonia = nombre; zona = z
                partes += when (r.nextInt(4)) {
                    0 -> "colonia $nombre"
                    1 -> "col. $nombre"
                    2 -> "en la colonia $nombre"
                    else -> nombre
                }
            }
            tiraColonia < 0.90 -> {
                val nombre = r.de(desconocidas)
                colonia = nombre; zona = Zona.SIN_ZONA
                partes += "colonia $nombre"
            }
        }

        // Lugar
        var lugar: String? = null
        if (r.prob(0.55)) { lugar = r.de(calles); partes += lugar }
        else if (r.prob(0.15)) { lugar = r.de(sitios); partes += lugar }

        // Teléfono
        var telefono: String? = null
        if (r.prob(0.6)) {
            val num = "999" + (1..7).joinToString("") { r.nextInt(10).toString() }
            telefono = num
            partes += when (r.nextInt(5)) {
                0 -> num
                1 -> "${num.take(3)} ${num.substring(3, 6)} ${num.takeLast(4)}"
                2 -> "${num.take(3)}-${num.substring(3, 6)}-${num.takeLast(4)}"
                3 -> "+52 ${num.take(3)} ${num.substring(3, 6)} ${num.takeLast(4)}"
                else -> "cel $num"
            }
        }

        // Dinero
        var precio: Int? = null
        var anticipo: Int? = null
        if (r.prob(0.5)) {
            val p = r.de(listOf(300, 400, 500, 600, 800, 1000, 1200, 1500, 2500))
            precio = p
            partes += when (r.nextInt(4)) {
                0 -> "$" + (if (p >= 1000) "%,d".format(java.util.Locale.US, p) else "$p")
                1 -> "$p pesos"
                2 -> "cobro $p"
                else -> "precio $$p"
            }
            if (r.prob(0.4)) {
                val a = p / 2 / 50 * 50
                anticipo = a
                partes += when (r.nextInt(3)) {
                    0 -> "anticipo $a"
                    1 -> "dejó $a"
                    else -> "adelanto de $a"
                }
            }
        }

        // Cómo lo escribe esta persona
        if (r.prob(0.6)) partes.shuffle(r)
        val sep = r.de(listOf(", ", ", ", ", ", ", ", ", ", " ", " ", "\n", "\n", ". ", " - "))
        var texto = partes.joinToString(sep)
        if (r.prob(0.3)) texto = sinAcentos(texto)
        when (r.nextInt(10)) {
            0, 1 -> texto = texto.lowercase()
            2 -> texto = texto.uppercase()
        }

        return Esperado(texto, hoy, fecha, hora, dudosa, tipo, cliente, clienteSinTitulo, colonia, zona, lugar, telefono, precio, anticipo)
    }

    private fun sinAcentos(s: String) = s.map {
        when (it) {
            'á' -> 'a'; 'é' -> 'e'; 'í' -> 'i'; 'ó' -> 'o'; 'ú' -> 'u'
            'Á' -> 'A'; 'É' -> 'E'; 'Í' -> 'I'; 'Ó' -> 'O'; 'Ú' -> 'U'
            else -> it
        }
    }.joinToString("")
}
