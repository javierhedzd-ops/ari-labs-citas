package mx.arilabs.citas

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mx.arilabs.citas.datos.Agenda
import mx.arilabs.citas.datos.Almacen
import mx.arilabs.citas.datos.Anticipo
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Ejemplos
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.lector.Borrador
import mx.arilabs.citas.lector.LectorDeCitas
import mx.arilabs.citas.ui.Aviso
import mx.arilabs.citas.ui.Iconos
import mx.arilabs.citas.ui.Papel
import mx.arilabs.citas.ui.PantallaAjustes
import mx.arilabs.citas.ui.PantallaAvisos
import mx.arilabs.citas.ui.PantallaBienvenida
import mx.arilabs.citas.ui.PantallaBuscar
import mx.arilabs.citas.ui.PantallaCliente
import mx.arilabs.citas.ui.PantallaDetalle
import mx.arilabs.citas.ui.PantallaEscribir
import mx.arilabs.citas.ui.PantallaFormulario
import mx.arilabs.citas.ui.PantallaElegirCanciones
import mx.arilabs.citas.ui.PantallaTocar
import mx.arilabs.citas.ui.SeccionRepertorio
import mx.arilabs.citas.ui.SeccionAgenda
import mx.arilabs.citas.ui.SeccionCalendario
import mx.arilabs.citas.ui.SeccionClientes
import mx.arilabs.citas.ui.SeccionCobros
import mx.arilabs.citas.ui.TemaCitas
import java.time.LocalDate
import java.time.LocalTime

private enum class Pestana(val nombre: String) { AGENDA("Agenda"), CALENDARIO("Calendario"), REPERTORIO("Música"), CLIENTES("Clientes"), COBROS("Cobros") }

private sealed interface Vista {
    val nivel: Int
    data object Bienvenida : Vista { override val nivel = 0 }
    data object Inicio : Vista { override val nivel = 0 }
    data class Escribir(val texto: String = "") : Vista { override val nivel = 1 }
    data class Formulario(val borrador: Borrador, val editandoId: Long?) : Vista { override val nivel = 2 }
    data class Detalle(val id: Long) : Vista { override val nivel = 1 }
    data class Cliente(val nombre: String) : Vista { override val nivel = 1 }
    data class Avisos(val titulo: String, val explicacion: String, val avisos: List<Aviso>) : Vista { override val nivel = 3 }
    data object Ajustes : Vista { override val nivel = 1 }
    data object Buscar : Vista { override val nivel = 1 }
    data class ElegirCanciones(val id: Long) : Vista { override val nivel = 2 }
    data class Tocar(val id: Long) : Vista { override val nivel = 2 }
}

class MainActivity : ComponentActivity() {
    /** Texto compartido desde WhatsApp (u otra app) que falta convertir en cita. */
    private val compartido = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) recibir(intent)
        val almacen = Almacen(applicationContext)
        setContent {
            TemaCitas {
                Box(Modifier.fillMaxSize().background(Papel.fondo)) {
                    App(almacen, compartido.value) { compartido.value = null }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        recibir(intent)
    }

    private fun recibir(intent: android.content.Intent?) {
        if (intent?.action != android.content.Intent.ACTION_SEND) return
        val texto = intent.getStringExtra(android.content.Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (texto.isNotEmpty()) compartido.value = texto
    }
}

@Composable
private fun App(almacen: Almacen, compartido: String?, onCompartidoUsado: () -> Unit) {
    val context = LocalContext.current
    val citas = remember { mutableStateListOf<Cita>().apply { addAll(almacen.leer()) } }
    var nombre by remember { mutableStateOf(almacen.nombre) }
    val repertorio = remember { mutableStateListOf<mx.arilabs.citas.datos.Cancion>().apply { addAll(almacen.leerRepertorio()) } }
    val equipo = remember { almacen.equipo }
    var vista by remember { mutableStateOf<Vista>(if (almacen.nombre.isBlank()) Vista.Bienvenida else Vista.Inicio) }
    var pestana by remember { mutableStateOf(Pestana.AGENDA) }
    // De dónde venimos, para que "Atrás" regrese al lugar correcto.
    val historial = remember { mutableStateListOf<Vista>() }
    val hoy = LocalDate.now()

    fun ir(v: Vista) { historial.add(vista); vista = v }
    fun atras() { vista = if (historial.isNotEmpty()) historial.removeAt(historial.lastIndex) else Vista.Inicio }
    fun aInicio() { historial.clear(); vista = Vista.Inicio }

    fun guardarTodo() {
        almacen.guardar(citas.toList())
        Recordatorios.reprogramar(context, citas.filter { !Ejemplos.esEjemplo(it) })
    }
    fun reemplazar(c: Cita) {
        val i = citas.indexOfFirst { it.id == c.id }
        if (i >= 0) citas[i] = c else citas.add(c)
        guardarTodo()
    }

    val pedirAvisos = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(nombre.isNotBlank()) {
        Recordatorios.crearCanal(context)
        Recordatorios.reprogramar(context, citas.filter { !Ejemplos.esEjemplo(it) })
        // Solo si falta: pedirlo de nuevo abre una ventana del sistema encima de la app.
        if (nombre.isNotBlank() && Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) pedirAvisos.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Un mensaje compartido desde WhatsApp abre "Revisa la cita" ya ordenada.
    LaunchedEffect(compartido, nombre.isNotBlank()) {
        if (compartido != null && nombre.isNotBlank()) {
            historial.clear()
            historial.add(Vista.Inicio)
            vista = Vista.Formulario(LectorDeCitas.leer(compartido, LocalDate.now()), null)
            onCompartidoUsado()
        }
    }

    BackHandler(enabled = vista != Vista.Inicio && vista != Vista.Bienvenida) { atras() }
    BackHandler(enabled = vista == Vista.Inicio && pestana != Pestana.AGENDA) { pestana = Pestana.AGENDA }

    AnimatedContent(
        targetState = vista,
        transitionSpec = {
            val adelante = targetState.nivel >= initialState.nivel
            if (adelante) {
                (slideInHorizontally(tween(320)) { it / 4 } + fadeIn(tween(260))) togetherWith (fadeOut(tween(200)))
            } else {
                fadeIn(tween(260)) togetherWith (slideOutHorizontally(tween(300)) { it / 4 } + fadeOut(tween(220)))
            }
        },
        contentKey = { it::class },
        label = "vista",
    ) { v ->
        Box(Modifier.fillMaxSize().background(Papel.fondo)) {
            when (v) {
                Vista.Bienvenida -> Box(Modifier.safeDrawingPadding()) {
                    PantallaBienvenida { n, ejemplo ->
                        almacen.nombre = n
                        nombre = n
                        if (ejemplo && citas.none { Ejemplos.esEjemplo(it) }) { citas.addAll(Ejemplos.crear(hoy)); guardarTodo() }
                        aInicio()
                    }
                }

                Vista.Inicio -> Inicio(
                    citas = citas, hoy = hoy, nombre = nombre, pestana = pestana,
                    onPestana = { pestana = it },
                    onNueva = { ir(Vista.Escribir()) },
                    onVer = { ir(Vista.Detalle(it.id)) },
                    onCliente = { ir(Vista.Cliente(it)) },
                    onBuscar = { ir(Vista.Buscar) },
                    onAjustes = { ir(Vista.Ajustes) },
                    onPagada = { reemplazar(it.copy(pagada = true)) },
                    repertorio = repertorio,
                    onGuardarCancion = { can ->
                        val nueva = if (can.id == 0L) can.copy(id = (repertorio.maxOfOrNull { it.id } ?: 0L) + 1) else can
                        val i = repertorio.indexOfFirst { it.id == nueva.id }
                        if (i >= 0) repertorio[i] = nueva else repertorio.add(nueva)
                        almacen.guardarRepertorio(repertorio.toList())
                    },
                    onBorrarCancion = { id ->
                        repertorio.removeAll { it.id == id }
                        almacen.guardarRepertorio(repertorio.toList())
                    },
                )

                is Vista.Escribir -> Box(Modifier.safeDrawingPadding()) {
                    PantallaEscribir(v.texto, onAtras = { atras() }, onOrdenar = { t -> ir(Vista.Formulario(LectorDeCitas.leer(t, hoy), null)) })
                }

                is Vista.Formulario -> Box(Modifier.safeDrawingPadding()) {
                    PantallaFormulario(
                        titulo = if (v.editandoId == null) "Revisa la cita" else "Editar cita",
                        inicial = v.borrador, citas = citas, editandoId = v.editandoId,
                        onAtras = { atras() },
                        onGuardar = { b, nueveDias ->
                            val anterior = v.editandoId?.let { id -> citas.firstOrNull { it.id == id } }
                            val cita = Cita(
                                id = anterior?.id ?: almacen.siguienteId(citas),
                                fecha = b.fecha ?: hoy,
                                hora = b.hora ?: anterior?.hora ?: LocalTime.NOON,
                                cliente = b.cliente, tipo = b.tipo ?: Tipo.OTRO,
                                lugar = b.lugar, colonia = b.colonia, zona = b.zona, telefono = b.telefono,
                                precio = b.precio, anticipo = b.anticipo, notas = b.notas,
                                cancelada = anterior?.cancelada ?: false,
                                anticipoAlCancelar = anterior?.anticipoAlCancelar,
                                textoOriginal = anterior?.textoOriginal ?: b.texto,
                                hecha = anterior?.hecha ?: false,
                                pagada = anterior?.pagada ?: false,
                                duracion = (b.duracion ?: (b.tipo ?: Tipo.OTRO).duracion).coerceIn(15, mx.arilabs.citas.datos.DURACION_MAXIMA),
                                canciones = anterior?.canciones ?: emptyList(),
                                mapa = b.mapa.ifBlank { anterior?.mapa ?: "" },
                                equipoListo = anterior?.equipoListo ?: emptySet(),
                            )
                            if (nueveDias) {
                                // Novena: 9 noches seguidas a la misma hora. El precio y el anticipo van en la primera.
                                (0 until 9).forEach { n ->
                                    citas.add(
                                        cita.copy(
                                            id = cita.id + n,
                                            fecha = cita.fecha.plusDays(n.toLong()),
                                            precio = if (n == 0) cita.precio else null,
                                            anticipo = if (n == 0) cita.anticipo else null,
                                            notas = listOf("Novena, día ${n + 1} de 9", cita.notas).filter { it.isNotBlank() }.joinToString(". "),
                                        ),
                                    )
                                }
                                guardarTodo()
                            } else {
                                reemplazar(cita)
                            }
                            historial.clear()
                            historial.add(Vista.Inicio)
                            vista = Vista.Detalle(cita.id)
                        },
                    )
                }

                is Vista.Detalle -> {
                    val c = citas.firstOrNull { it.id == v.id }
                    if (c == null) {
                        LaunchedEffect(v.id) { aInicio() }
                    } else Box(Modifier.safeDrawingPadding()) {
                        PantallaDetalle(
                            c = c, hoy = hoy, nombre = nombre,
                            onAtras = { atras() },
                            onEditar = {
                                ir(Vista.Formulario(
                                    Borrador(
                                        fecha = c.fecha, hora = c.hora, cliente = c.cliente, tipo = c.tipo, lugar = c.lugar,
                                        colonia = c.colonia, zona = c.zona, telefono = c.telefono, precio = c.precio,
                                        anticipo = c.anticipo, notas = c.notas, duracion = c.duracion, mapa = c.mapa,
                                    ),
                                    c.id,
                                ))
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
                                ir(Vista.Avisos(
                                    "Cita movida",
                                    "Quedó ${Mensajes.cuando(fecha, hoy)} a las ${Mensajes.hora(hora)}. Avisa a ${movida.cliente} y, si hace falta, a los demás de ese día.",
                                    avisos,
                                ))
                            },
                            onCancelar = { anticipo: Anticipo ->
                                val cancelada = c.copy(cancelada = true, anticipoAlCancelar = anticipo)
                                reemplazar(cancelada)
                                ir(Vista.Avisos(
                                    "Cita cancelada",
                                    "Quedó en tu agenda como cancelada. Avísale a ${c.cliente}.",
                                    listOf(Aviso(c.cliente, c.telefono, Mensajes.cancelacion(cancelada, nombre, hoy))),
                                ))
                            },
                            onBorrar = {
                                citas.removeAll { it.id == c.id }
                                guardarTodo()
                                atras()
                            },
                            onHecha = { reemplazar(c.copy(hecha = it)) },
                            onPagada = { reemplazar(c.copy(pagada = it)) },
                            onCliente = { ir(Vista.Cliente(c.cliente)) },
                            repertorio = repertorio,
                            equipo = equipo,
                            onElegirCanciones = { ir(Vista.ElegirCanciones(c.id)) },
                            onTocar = { ir(Vista.Tocar(c.id)) },
                            onEquipo = { cosa, listo -> reemplazar(c.copy(equipoListo = if (listo) c.equipoListo + cosa else c.equipoListo - cosa)) },
                        )
                    }
                }

                is Vista.Cliente -> Box(Modifier.safeDrawingPadding()) {
                    PantallaCliente(
                        nombre = v.nombre, citas = citas, hoy = hoy,
                        onAtras = { atras() },
                        onVer = { ir(Vista.Detalle(it.id)) },
                        onNueva = { cl -> ir(Vista.Escribir(listOf(cl.nombre, cl.colonia, cl.telefono).filter { it.isNotBlank() }.joinToString(", ") + ", ")) },
                    )
                }

                is Vista.Avisos -> Box(Modifier.safeDrawingPadding()) {
                    PantallaAvisos(v.titulo, v.explicacion, v.avisos) { atras() }
                }

                Vista.Ajustes -> Box(Modifier.safeDrawingPadding()) {
                    PantallaAjustes(
                        nombre = nombre,
                        hayEjemplos = citas.any { Ejemplos.esEjemplo(it) },
                        onAtras = { atras() },
                        onNombre = { n -> almacen.nombre = n; nombre = n },
                        onCopia = { "Copia de Mis citas (${Mensajes.fecha(hoy)}). Guárdala, no la borres:\n" + almacen.exportar(citas.filter { !Ejemplos.esEjemplo(it) }) },
                        onRecuperar = { texto ->
                            val copia = almacen.importar(texto)
                            if (copia == null) {
                                "Eso no parece una copia de Mis citas."
                            } else {
                                val nuevas = copia.filter { c -> citas.none { it.fecha == c.fecha && it.hora == c.hora && it.cliente == c.cliente } }
                                var siguiente = almacen.siguienteId(citas)
                                nuevas.forEach { c -> citas.add(c.copy(id = siguiente++)) }
                                guardarTodo()
                                if (nuevas.isEmpty()) "Ya tenías todas las citas de esa copia." else "Recuperé ${nuevas.size} citas."
                            }
                        },
                        bienvenida = Mensajes.bienvenida(nombre),
                        onEjemplos = { poner ->
                            citas.removeAll { Ejemplos.esEjemplo(it) }
                            if (poner) citas.addAll(Ejemplos.crear(hoy))
                            guardarTodo()
                        },
                    )
                }

                is Vista.ElegirCanciones -> {
                    val c = citas.firstOrNull { it.id == v.id }
                    if (c == null) LaunchedEffect(v.id) { atras() } else Box(Modifier.safeDrawingPadding()) {
                        PantallaElegirCanciones(c, repertorio, onAtras = { atras() }, onListo = { ids -> reemplazar(c.copy(canciones = ids)); atras() })
                    }
                }

                is Vista.Tocar -> {
                    val c = citas.firstOrNull { it.id == v.id }
                    if (c == null) LaunchedEffect(v.id) { atras() } else {
                        PantallaTocar(c, c.canciones.mapNotNull { id -> repertorio.firstOrNull { it.id == id } }, onSalir = { atras() })
                    }
                }

                Vista.Buscar -> Box(Modifier.safeDrawingPadding()) {
                    PantallaBuscar(citas, hoy, onAtras = { atras() }, onVer = { ir(Vista.Detalle(it.id)) })
                }
            }
        }
    }
}

@Composable
private fun Inicio(
    citas: List<Cita>,
    hoy: LocalDate,
    nombre: String,
    pestana: Pestana,
    onPestana: (Pestana) -> Unit,
    onNueva: () -> Unit,
    onVer: (Cita) -> Unit,
    onCliente: (String) -> Unit,
    onBuscar: () -> Unit,
    onAjustes: () -> Unit,
    onPagada: (Cita) -> Unit,
    repertorio: List<mx.arilabs.citas.datos.Cancion>,
    onGuardarCancion: (mx.arilabs.citas.datos.Cancion) -> Unit,
    onBorrarCancion: (Long) -> Unit,
) {
    Scaffold(
        containerColor = Papel.fondo,
        bottomBar = {
            NavigationBar(containerColor = Papel.hoja, tonalElevation = 0.dp) {
                Pestana.entries.forEach { p ->
                    val icono: ImageVector = when (p) {
                        Pestana.AGENDA -> Icons.Outlined.Home
                        Pestana.CALENDARIO -> Iconos.calendario
                        Pestana.REPERTORIO -> Iconos.musica
                        Pestana.CLIENTES -> Icons.Filled.Person
                        Pestana.COBROS -> Iconos.dinero
                    }
                    NavigationBarItem(
                        selected = pestana == p,
                        onClick = { onPestana(p) },
                        icon = { Icon(icono, contentDescription = null) },
                        label = { Text(p.nombre, style = androidx.compose.material3.MaterialTheme.typography.labelSmall, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Papel.azulSuave, selectedIconColor = Papel.azul, selectedTextColor = Papel.azul),
                    )
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(pestana == Pestana.AGENDA || pestana == Pestana.CALENDARIO || pestana == Pestana.CLIENTES, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                ExtendedFloatingActionButton(
                    onClick = onNueva,
                    modifier = Modifier.testTag("nueva-cita"),
                    containerColor = Papel.azul,
                    contentColor = androidx.compose.ui.graphics.Color.White,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva cita") },
                )
            }
        },
    ) { relleno ->
        AnimatedContent(
            targetState = pestana,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            modifier = Modifier.padding(relleno),
            label = "pestana",
        ) { p ->
            when (p) {
                Pestana.AGENDA -> SeccionAgenda(citas, hoy, nombre, onVer, onBuscar, onAjustes)
                Pestana.CALENDARIO -> SeccionCalendario(citas, hoy, onVer)
                Pestana.REPERTORIO -> SeccionRepertorio(repertorio, onGuardarCancion, onBorrarCancion)
                Pestana.CLIENTES -> SeccionClientes(citas, hoy, onCliente)
                Pestana.COBROS -> SeccionCobros(citas, hoy, onVer, onPagada)
            }
        }
    }
}

