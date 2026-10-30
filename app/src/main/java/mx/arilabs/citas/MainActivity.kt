package mx.arilabs.citas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import mx.arilabs.citas.datos.Agenda
import mx.arilabs.citas.datos.Almacen
import mx.arilabs.citas.datos.Anticipo
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.lector.Borrador
import mx.arilabs.citas.lector.LectorDeCitas
import mx.arilabs.citas.ui.Aviso
import mx.arilabs.citas.ui.Papel
import mx.arilabs.citas.ui.PantallaAgenda
import mx.arilabs.citas.ui.PantallaAjustes
import mx.arilabs.citas.ui.PantallaAvisos
import mx.arilabs.citas.ui.PantallaDetalle
import mx.arilabs.citas.ui.PantallaEscribir
import mx.arilabs.citas.ui.PantallaFormulario
import mx.arilabs.citas.ui.PantallaTodas
import mx.arilabs.citas.ui.TemaCitas
import java.time.LocalDate

private sealed interface Vista {
    data object Agenda : Vista
    data object Escribir : Vista
    data class Formulario(val borrador: Borrador, val editandoId: Long?) : Vista
    data class Detalle(val id: Long) : Vista
    data class Avisos(val titulo: String, val explicacion: String, val avisos: List<Aviso>) : Vista
    data object Ajustes : Vista
    data object Todas : Vista
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val almacen = Almacen(applicationContext)
        setContent {
            TemaCitas {
                Box(Modifier.fillMaxSize().background(Papel.fondo).safeDrawingPadding()) {
                    App(almacen)
                }
            }
        }
    }
}

@Composable
private fun App(almacen: Almacen) {
    val citas = remember { mutableStateListOf<Cita>().apply { addAll(almacen.leer()) } }
    var nombre by remember { mutableStateOf(almacen.nombre) }
    var vista by remember { mutableStateOf<Vista>(if (almacen.nombre.isBlank()) Vista.Ajustes else Vista.Agenda) }
    val hoy = LocalDate.now()

    val context = androidx.compose.ui.platform.LocalContext.current

    fun guardarTodo() {
        almacen.guardar(citas.toList())
        Recordatorios.reprogramar(context, citas.toList())
    }

    // Permiso de avisos (Android 13+), una sola vez y ya con el nombre puesto.
    val pedirAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(nombre.isNotBlank()) {
        Recordatorios.crearCanal(context)
        Recordatorios.reprogramar(context, citas.toList())
        if (nombre.isNotBlank() && Build.VERSION.SDK_INT >= 33) pedirAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun reemplazar(c: Cita) {
        val i = citas.indexOfFirst { it.id == c.id }
        if (i >= 0) citas[i] = c else citas.add(c)
        guardarTodo()
    }

    BackHandler(enabled = vista != Vista.Agenda && nombre.isNotBlank()) {
        vista = when (val v = vista) {
            is Vista.Formulario -> if (v.editandoId != null) Vista.Detalle(v.editandoId) else Vista.Escribir
            else -> Vista.Agenda
        }
    }

    when (val v = vista) {
        Vista.Agenda -> PantallaAgenda(
            citas = citas,
            hoy = hoy,
            onNueva = { vista = Vista.Escribir },
            onVer = { vista = Vista.Detalle(it.id) },
            onBuscar = { vista = Vista.Todas },
            onAjustes = { vista = Vista.Ajustes },
        )

        Vista.Todas -> PantallaTodas(
            citas = citas,
            hoy = hoy,
            onAtras = { vista = Vista.Agenda },
            onVer = { vista = Vista.Detalle(it.id) },
        )

        Vista.Escribir -> PantallaEscribir(
            onAtras = { vista = Vista.Agenda },
            onOrdenar = { texto -> vista = Vista.Formulario(LectorDeCitas.leer(texto, hoy), null) },
        )

        is Vista.Formulario -> PantallaFormulario(
            titulo = if (v.editandoId == null) "Revisa la cita" else "Editar cita",
            inicial = v.borrador,
            citas = citas,
            editandoId = v.editandoId,
            onAtras = { vista = if (v.editandoId != null) Vista.Detalle(v.editandoId) else Vista.Escribir },
            onGuardar = { b, nueveDias ->
                val anterior = v.editandoId?.let { id -> citas.firstOrNull { it.id == id } }
                val cita = Cita(
                    id = anterior?.id ?: almacen.siguienteId(citas),
                    fecha = b.fecha ?: hoy,
                    hora = b.hora ?: anterior?.hora ?: java.time.LocalTime.NOON,
                    cliente = b.cliente,
                    tipo = b.tipo ?: Tipo.OTRO,
                    lugar = b.lugar,
                    colonia = b.colonia,
                    zona = b.zona,
                    telefono = b.telefono,
                    precio = b.precio,
                    anticipo = b.anticipo,
                    notas = b.notas,
                    cancelada = anterior?.cancelada ?: false,
                    anticipoAlCancelar = anterior?.anticipoAlCancelar,
                    textoOriginal = anterior?.textoOriginal ?: b.texto,
                )
                if (nueveDias) {
                    // Novena: 9 noches seguidas a la misma hora. El precio y el anticipo van en la primera.
                    (0 until 9).forEach { n ->
                        val noche = cita.copy(
                            id = cita.id + n,
                            fecha = cita.fecha.plusDays(n.toLong()),
                            precio = if (n == 0) cita.precio else null,
                            anticipo = if (n == 0) cita.anticipo else null,
                            notas = listOf("Novena, día ${n + 1} de 9", cita.notas).filter { it.isNotBlank() }.joinToString(". "),
                        )
                        citas.add(noche)
                    }
                    guardarTodo()
                } else {
                    reemplazar(cita)
                }
                vista = Vista.Detalle(cita.id)
            },
        )

        is Vista.Detalle -> {
            val c = citas.firstOrNull { it.id == v.id }
            if (c == null) {
                LaunchedEffect(v.id) { vista = Vista.Agenda }
            } else {
                PantallaDetalle(
                    c = c,
                    hoy = hoy,
                    nombre = nombre,
                    onAtras = { vista = Vista.Agenda },
                    onEditar = {
                        vista = Vista.Formulario(
                            Borrador(
                                fecha = c.fecha, hora = c.hora, cliente = c.cliente, tipo = c.tipo, lugar = c.lugar,
                                colonia = c.colonia, zona = c.zona, telefono = c.telefono, precio = c.precio,
                                anticipo = c.anticipo, notas = c.notas,
                            ),
                            c.id,
                        )
                    },
                    onMover = { fecha, hora ->
                        val diaAnterior = c.fecha
                        val movida = c.copy(fecha = fecha, hora = hora)
                        reemplazar(movida)
                        val avisos = buildList {
                            add(Aviso(movida.cliente, movida.telefono, Mensajes.citaMovida(movida, nombre, hoy)))
                            Agenda.otrasDelDia(citas, movida, diaAnterior).forEach { o ->
                                add(Aviso(o.cliente, o.telefono, Mensajes.cambioEnElDia(o, nombre, hoy)))
                            }
                        }
                        vista = Vista.Avisos(
                            titulo = "Cita movida",
                            explicacion = "Quedó ${Mensajes.cuando(fecha, hoy)} a las ${Mensajes.hora(hora)}. " +
                                "Avisa a ${movida.cliente} y, si hace falta, a los demás de ese día.",
                            avisos = avisos,
                        )
                    },
                    onBorrar = {
                        citas.removeAll { it.id == c.id }
                        guardarTodo()
                        vista = Vista.Agenda
                    },
                    onCancelar = { anticipo: Anticipo ->
                        val cancelada = c.copy(cancelada = true, anticipoAlCancelar = anticipo)
                        reemplazar(cancelada)
                        vista = Vista.Avisos(
                            titulo = "Cita cancelada",
                            explicacion = "Quedó tachada en tu agenda. Avísale a ${c.cliente}.",
                            avisos = listOf(Aviso(c.cliente, c.telefono, Mensajes.cancelacion(cancelada, nombre, hoy))),
                        )
                    },
                )
            }
        }

        is Vista.Avisos -> PantallaAvisos(v.titulo, v.explicacion, v.avisos) { vista = Vista.Agenda }

        Vista.Ajustes -> PantallaAjustes(
            nombre = nombre,
            primeraVez = nombre.isBlank(),
            onAtras = { vista = Vista.Agenda },
            onGuardar = { n ->
                almacen.nombre = n
                nombre = n.trim()
                vista = Vista.Agenda
            },
            onCopia = { "Copia de Mis citas (${Mensajes.fecha(hoy)}). Guárdala, no la borres:\n" + almacen.exportar(citas.toList()) },
            onRecuperar = { texto ->
                val copia = almacen.importar(texto)
                if (copia == null) {
                    "Eso no parece una copia de Mis citas."
                } else {
                    // Solo se agregan las que no están (mismo día, hora y cliente).
                    val nuevas = copia.filter { c -> citas.none { it.fecha == c.fecha && it.hora == c.hora && it.cliente == c.cliente } }
                    var siguiente = almacen.siguienteId(citas)
                    nuevas.forEach { c -> citas.add(c.copy(id = siguiente++)) }
                    guardarTodo()
                    if (nuevas.isEmpty()) "Ya tenías todas las citas de esa copia." else "Recuperé ${nuevas.size} citas."
                }
            },
        )
    }
}
