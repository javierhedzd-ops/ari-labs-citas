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

    @Synchronized
    fun leer(): List<Cita> {
        if (!archivo.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(archivo.readText())
            (0 until arr.length()).mapNotNull { i -> runCatching { desdeJson(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrElse { emptyList() }
    }

    @Synchronized
    fun guardar(citas: List<Cita>) {
        val arr = JSONArray()
        citas.forEach { arr.put(aJson(it)) }
        val tmp = File(archivo.parentFile, "citas.json.tmp")
        tmp.writeText(arr.toString())
        if (!tmp.renameTo(archivo)) {
            archivo.writeText(arr.toString())
            tmp.delete()
        }
    }

    fun siguienteId(citas: List<Cita>): Long = (citas.maxOfOrNull { it.id } ?: 0L) + 1

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
    )
}
