package mx.arilabs.citas.datos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

/**
 * Guarda las citas en un archivo JSON dentro del teléfono (memoria privada de la app).
 * Escribe primero a un archivo temporal y luego lo cambia de nombre, para que un cierre
 * a medias nunca deje el archivo roto.
 */
class Almacen(context: Context) {

    private val archivo = File(context.filesDir, "citas.json")
    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    var nombre: String
        get() = prefs.getString("nombre", "") ?: ""
        set(v) = prefs.edit().putString("nombre", v.trim()).apply()

    // ------------------------------------------------ Licencia

    var clave: String
        get() = prefs.getString("clave", "") ?: ""
        set(v) = prefs.edit().putString("clave", v.trim()).apply()

    /**
     * Fecha de hoy que no se deja atrasar: si alguien regresa la fecha del teléfono para que
     * la licencia no venza, se usa la fecha más reciente que la app ya vio.
     */
    fun hoySeguro(): LocalDate {
        val visto = prefs.getString("ultimaFecha", null)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val hoy = LocalDate.now()
        return if (visto != null && visto.isAfter(hoy)) visto else {
            prefs.edit().putString("ultimaFecha", hoy.toString()).apply()
            hoy
        }
    }

    // ------------------------------------------------ Repertorio y equipo

    private val archivoRepertorio = File(context.filesDir, "repertorio.json")

    @Synchronized
    fun leerRepertorio(): List<Cancion> {
        if (!archivoRepertorio.exists()) return Repertorio.inicial()
        return runCatching {
            val a = JSONArray(archivoRepertorio.readText())
            (0 until a.length()).map { i ->
                val o = a.getJSONObject(i)
                Cancion(
                    id = o.getLong("id"), titulo = o.getString("titulo"), tono = o.optString("tono"),
                    momento = runCatching { Momento.valueOf(o.optString("momento")) }.getOrDefault(Momento.OTRO),
                    notas = o.optString("notas"),
                )
            }
        }.getOrElse { Repertorio.inicial() }
    }

    @Synchronized
    fun guardarRepertorio(lista: List<Cancion>) {
        val a = JSONArray()
        lista.forEach { c -> a.put(JSONObject().put("id", c.id).put("titulo", c.titulo).put("tono", c.tono).put("momento", c.momento.name).put("notas", c.notas)) }
        val tmp = File(archivoRepertorio.parentFile, "repertorio.json.tmp")
        tmp.writeText(a.toString())
        if (!tmp.renameTo(archivoRepertorio)) { archivoRepertorio.writeText(a.toString()); tmp.delete() }
    }

    var equipo: List<String>
        get() = prefs.getString("equipo", null)?.split("\n")?.filter { it.isNotBlank() } ?: Repertorio.equipoInicial
        set(v) = prefs.edit().putString("equipo", v.joinToString("\n")).apply()

    /** La versión anterior de citas.json: si el archivo se daña, de aquí se recuperan las citas. */
    private val respaldo = File(context.filesDir, "citas.respaldo.json")

    @Synchronized
    fun leer(): List<Cita> {
        for (f in listOf(archivo, respaldo)) {
            if (!f.exists()) continue
            val lista = runCatching {
                val arr = JSONArray(f.readText())
                (0 until arr.length()).mapNotNull { i -> runCatching { desdeJson(arr.getJSONObject(i)) }.getOrNull() }
            }.getOrNull()
            if (lista != null) return lista
            // Dañado: se aparta para que el siguiente guardado no lo pise, y se prueba con el respaldo.
            runCatching { f.copyTo(File(f.parentFile, f.name + ".danado"), overwrite = true) }
        }
        return emptyList()
    }

    @Synchronized
    fun guardar(citas: List<Cita>) {
        val arr = JSONArray()
        citas.forEach { arr.put(aJson(it)) }
        if (archivo.exists()) runCatching { archivo.copyTo(respaldo, overwrite = true) }
        val tmp = File(archivo.parentFile, "citas.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(archivo)) {
            archivo.writeText(arr.toString())
            tmp.delete()
        }
    }

    fun siguienteId(citas: List<Cita>): Long = (citas.maxOfOrNull { it.id } ?: 0L) + 1

    /**
     * Copia de todas las citas en texto, para mandarla por WhatsApp a uno mismo. Sin campos
     * vacíos ni el mensaje original, para que quepa en un mensaje aunque haya cientos de citas.
     */
    fun exportar(citas: List<Cita>): String {
        val arr = JSONArray()
        citas.forEach { c ->
            val o = aJson(c)
            o.remove("textoOriginal")
            o.keys().asSequence().toList().forEach { k ->
                val v = o.opt(k)
                if (v == "" || v == false || (v is JSONArray && v.length() == 0)) o.remove(k)
            }
            arr.put(o)
        }
        return JSONObject().put("app", "mis-citas").put("version", 1).put("citas", arr).toString()
    }

    /** Lee una copia pegada. Devuelve null si el texto no es una copia válida. */
    fun importar(texto: String): List<Cita>? {
        val inicio = texto.indexOf('{')
        val fin = texto.lastIndexOf('}')
        if (inicio < 0 || fin <= inicio) return null
        return runCatching {
            val o = JSONObject(texto.substring(inicio, fin + 1))
            if (o.optString("app") != "mis-citas") return null
            val arr = o.getJSONArray("citas")
            (0 until arr.length()).mapNotNull { i -> runCatching { desdeJson(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrNull()
    }

    private fun aJson(c: Cita) = JSONObject().apply {
        put("id", c.id)
        put("fecha", c.fecha.toString())
        put("hora", c.hora.toString())
        put("cliente", c.cliente)
        put("tipo", c.tipo.name)
        put("lugar", c.lugar)
        put("colonia", c.colonia)
        put("zona", c.zona.name)
        put("telefono", c.telefono)
        c.precio?.let { put("precio", it) }
        c.anticipo?.let { put("anticipo", it) }
        put("notas", c.notas)
        put("cancelada", c.cancelada)
        c.anticipoAlCancelar?.let { put("anticipoAlCancelar", it.name) }
        put("textoOriginal", c.textoOriginal)
        put("hecha", c.hecha)
        put("pagada", c.pagada)
        put("duracion", c.duracion)
        put("canciones", JSONArray().apply { c.canciones.forEach { put(it) } })
        put("equipoListo", JSONArray().apply { c.equipoListo.forEach { put(it) } })
        put("mapa", c.mapa)
    }

    private fun desdeJson(o: JSONObject) = Cita(
        id = o.getLong("id"),
        fecha = LocalDate.parse(o.getString("fecha")),
        hora = LocalTime.parse(o.getString("hora")),
        cliente = o.optString("cliente"),
        tipo = runCatching { Tipo.valueOf(o.optString("tipo")) }.getOrDefault(Tipo.OTRO),
        lugar = o.optString("lugar"),
        colonia = o.optString("colonia"),
        zona = runCatching { Zona.valueOf(o.optString("zona")) }.getOrDefault(Zona.SIN_ZONA),
        telefono = o.optString("telefono"),
        precio = if (o.has("precio")) o.getInt("precio") else null,
        anticipo = if (o.has("anticipo")) o.getInt("anticipo") else null,
        notas = o.optString("notas"),
        cancelada = o.optBoolean("cancelada"),
        anticipoAlCancelar = o.optString("anticipoAlCancelar").takeIf { it.isNotEmpty() }
            ?.let { runCatching { Anticipo.valueOf(it) }.getOrNull() },
        textoOriginal = o.optString("textoOriginal"),
        hecha = o.optBoolean("hecha"),
        pagada = o.optBoolean("pagada"),
    ).let { c ->
        c.copy(
            duracion = o.optInt("duracion", c.tipo.duracion).coerceIn(15, DURACION_MAXIMA),
            canciones = o.optJSONArray("canciones")?.let { a -> (0 until a.length()).map { a.getLong(it) } } ?: emptyList(),
            equipoListo = o.optJSONArray("equipoListo")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet(),
            mapa = o.optString("mapa"),
        )
    }
}
