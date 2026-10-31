package mx.arilabs.citas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import mx.arilabs.citas.datos.Agenda
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.lector.Colonias
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

private val margen = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp)
private val letraDia = listOf("L", "M", "M", "J", "V", "S", "D")

// ================================================================ Agenda

@Composable
fun SeccionAgenda(citas: List<Cita>, hoy: LocalDate, nombre: String, onVer: (Cita) -> Unit, onBuscar: () -> Unit, onAjustes: () -> Unit) {
    val context = LocalContext.current
    var dia by rememberSaveable { mutableStateOf(hoy.toEpochDay()) }
    val elegido = LocalDate.ofEpochDay(dia)
    var porZona by rememberSaveable { mutableStateOf(false) }
    var ahora by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(30_000); ahora = LocalDateTime.now() } }

    val proxima = Agenda.proxima(citas, ahora)
    val delDia = Agenda.delDia(citas, elegido, incluirCanceladas = true).let { l ->
        if (porZona) Agenda.porZona(l.filter { !it.cancelada }) + l.filter { it.cancelada } else l
    }
    val activas = delDia.filter { !it.cancelada }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(Mensajes.fecha(hoy).uppercase(), style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                    Text(saludo(ahora, nombre), style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onBuscar) { Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = Papel.azul) }
                IconButton(onClick = onAjustes) { Icon(Icons.Outlined.Settings, contentDescription = "Ajustes", tint = Papel.azul) }
            }
        }

        if (proxima != null) {
            item(key = "proxima") { TarjetaProxima(proxima, ahora, hoy, nombre) { onVer(proxima) } }
        }

        item(key = "semana") {
            TiraSemana(citas, hoy, elegido) { dia = it.toEpochDay() }
        }

        item(key = "cabeza-dia") {
            Seccion(
                titulo = when (elegido) {
                    hoy -> "Hoy"
                    hoy.plusDays(1) -> "Mañana"
                    else -> Mensajes.fecha(elegido).replaceFirstChar { it.uppercase() }
                },
            ) {
                if (activas.isNotEmpty()) {
                    val texto = Mensajes.avisoDelDia(activas, elegido, hoy)
                    IconButton(onClick = { Acciones.copiar(context, texto) }) { Icon(Iconos.copiar, "Copiar el día", tint = Papel.azul) }
                    IconButton(onClick = { Acciones.whatsapp(context, texto) }) { Icon(Iconos.chat, "Mandar el día por WhatsApp", tint = Papel.verde) }
                }
            }
        }

        if (activas.size > 1) {
            item(key = "orden") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = !porZona, onClick = { porZona = false }, label = { Text("Por hora") })
                    FilterChip(selected = porZona, onClick = { porZona = true }, label = { Text("Por zona") })
                    if (porZona) Text("Para no cruzar la ciudad", style = MaterialTheme.typography.bodySmall, color = Papel.tenue)
                }
            }
        }

        if (delDia.isEmpty()) {
            item(key = "vacio") {
                Tarjeta(Modifier.fillMaxWidth()) {
                    Vacio(
                        if (elegido == hoy) "Día libre" else "Nada apuntado",
                        "Toca “Nueva cita” y escríbela como te salga.",
                    )
                }
            }
        }
        items(delDia, key = { it.id }) { c ->
            Box(Modifier.animateItem()) { TarjetaCita(c) { onVer(c) } }
        }
    }
}

private fun saludo(ahora: LocalDateTime, nombre: String): String {
    val parte = when (ahora.hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    val primero = nombre.split(' ').firstOrNull { it.isNotBlank() && it.lowercase().trimEnd('.') !in setOf("don", "doña", "sr", "sra") } ?: ""
    return if (primero.isBlank()) parte else "$parte, $primero"
}

@Composable
private fun TarjetaProxima(c: Cita, ahora: LocalDateTime, hoy: LocalDate, nombre: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val cuando = LocalDateTime.of(c.fecha, c.hora)
    val minutos = Duration.between(ahora, cuando).toMinutes()
    val falta = when {
        minutos <= 0 -> "Ahora"
        minutos < 60 -> "En $minutos min"
        minutos < 24 * 60 -> "En ${minutos / 60} h ${minutos % 60} min"
        else -> Mensajes.cuando(c.fecha, hoy).replaceFirstChar { it.uppercase() }
    }
    Tarjeta(Modifier.fillMaxWidth(), color = Papel.relleno, radio = 26.dp, onClick = onClick) {
        Column(
            Modifier
                .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(Papel.relleno, androidx.compose.ui.graphics.lerp(Papel.relleno, Papel.tipoFondo(c.tipo), 0.55f))))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Iconos.reloj, null, tint = Color(0xFFB9CCE6), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("TU PRÓXIMA CITA · ${falta.uppercase()}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFB9CCE6))
            }
            Text(Mensajes.hora(c.hora).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "${c.tipo.nombre} · ${c.cliente}",
                style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            val donde = listOf(c.lugar, c.colonia).filter { it.isNotBlank() }.joinToString(", ")
            if (donde.isNotBlank()) Text(donde, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD6E1F0), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BotonClaro("Cómo llegar", Icons.Filled.Place, Modifier.weight(1f)) { Acciones.mapa(context, c.direccionParaMapa()) }
                if (c.telefono.isNotBlank()) {
                    BotonClaro("Avisar", Iconos.chat, Modifier.weight(1f)) {
                        Acciones.whatsapp(context, Mensajes.recordatorio(c, nombre, hoy), c.telefono)
                    }
                }
            }
        }
    }
}

@Composable
private fun BotonClaro(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .alTocar(onClick, texto)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

/** Los 14 días que vienen, para tocar uno y ver sus citas. */
@Composable
private fun TiraSemana(citas: List<Cita>, hoy: LocalDate, elegido: LocalDate, onElegir: (LocalDate) -> Unit) {
    val dias = (0L until 14L).map { hoy.plusDays(it) }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(dias, key = { it.toEpochDay() }) { d ->
            val sel = d == elegido
            val n = citas.count { it.fecha == d && !it.cancelada }
            val fondo by animateColorAsState(if (sel) Papel.relleno else Papel.hoja, tween(220), label = "dia")
            val texto by animateColorAsState(if (sel) Color.White else Papel.tinta, tween(220), label = "dia-t")
            Tarjeta(color = fondo, radio = 18.dp, onClick = { onElegir(d) }) {
                Column(
                    Modifier.width(56.dp).padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(letraDia[d.dayOfWeek.value - 1], style = MaterialTheme.typography.labelSmall, color = if (sel) Color(0xFFB9CCE6) else Papel.tenue)
                    Text("${d.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = texto)
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.height(6.dp)) {
                        repeat(minOf(n, 3)) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (sel) Color.White else Papel.azulVivo))
                        }
                    }
                }
            }
        }
    }
}

// ================================================================ Calendario

@Composable
fun SeccionCalendario(citas: List<Cita>, hoy: LocalDate, onVer: (Cita) -> Unit) {
    var mes by rememberSaveable { mutableStateOf(YearMonth.from(hoy).toString()) }
    val ym = YearMonth.parse(mes)
    var elegido by rememberSaveable { mutableStateOf(hoy.toEpochDay()) }
    val dia = LocalDate.ofEpochDay(elegido)
    val delDia = Agenda.delDia(citas, dia, incluirCanceladas = true)
    val nombresMes = listOf("Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre")

    LazyColumn(Modifier.fillMaxSize(), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TituloSeccion("Calendario") }
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { mes = ym.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior", tint = Papel.azul) }
                        AnimatedContent(
                            targetState = ym,
                            transitionSpec = {
                                val adelante = targetState > initialState
                                (slideInHorizontally { if (adelante) it / 3 else -it / 3 } + fadeIn()) togetherWith
                                    (slideOutHorizontally { if (adelante) -it / 3 else it / 3 } + fadeOut())
                            },
                            modifier = Modifier.weight(1f),
                            label = "mes",
                        ) { m ->
                            Text("${nombresMes[m.monthValue - 1]} ${m.year}", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        }
                        IconButton(onClick = { mes = ym.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente", tint = Papel.azul) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        letraDia.forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall, color = Papel.tenue) }
                    }
                    val primero = ym.atDay(1)
                    val hueco = primero.dayOfWeek.value - 1
                    val total = hueco + ym.lengthOfMonth()
                    val filas = (total + 6) / 7
                    for (f in 0 until filas) {
                        Row(Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val n = f * 7 + col - hueco + 1
                                Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                    if (n in 1..ym.lengthOfMonth()) {
                                        val d = ym.atDay(n)
                                        val sel = d == dia
                                        val cuantas = citas.count { it.fecha == d && !it.cancelada }
                                        val fondo by animateColorAsState(
                                            when { sel -> Papel.relleno; d == hoy -> Papel.azulSuave; else -> Color.Transparent }, tween(200), label = "c",
                                        )
                                        Column(
                                            Modifier.fillMaxSize().clip(CircleShape).background(fondo).alTocar({ elegido = d.toEpochDay() }, "Día $n"),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                        ) {
                                            Text("$n", style = MaterialTheme.typography.bodyMedium, fontWeight = if (d == hoy || sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Color.White else Papel.tinta)
                                            Box(Modifier.size(5.dp).clip(CircleShape).background(if (cuantas == 0) Color.Transparent else if (sel) Color.White else Papel.azulVivo))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Seccion(Mensajes.fecha(dia).replaceFirstChar { it.uppercase() }) }
        if (delDia.isEmpty()) item { Text("Sin citas este día.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue) }
        items(delDia, key = { it.id }) { c -> Box(Modifier.animateItem()) { TarjetaCita(c) { onVer(c) } } }
    }
}

@Composable
private fun TituloSeccion(t: String) {
    Text(t, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 18.dp, bottom = 4.dp))
}

// ================================================================ Clientes

@Composable
fun SeccionClientes(citas: List<Cita>, hoy: LocalDate, onCliente: (String) -> Unit) {
    var buscar by rememberSaveable { mutableStateOf("") }
    val q = Colonias.normalizar(buscar)
    val lista = Agenda.clientes(citas, hoy).filter { q.isBlank() || Colonias.normalizar(it.nombre + " " + it.colonia + " " + it.telefono).contains(q) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TituloSeccion("Clientes") }
        item {
            OutlinedTextField(
                value = buscar, onValueChange = { buscar = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                placeholder = { Text("Buscar por nombre, colonia o teléfono") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (lista.isEmpty()) {
            item { Tarjeta(Modifier.fillMaxWidth()) { Vacio(if (citas.isEmpty()) "Aún no hay clientes" else "No encontré a nadie", "Cada persona con la que apuntes una cita aparece aquí.") } }
        }
        items(lista, key = { Colonias.normalizar(it.nombre) }) { cl ->
            Tarjeta(Modifier.fillMaxWidth().animateItem(), onClick = { onCliente(cl.nombre) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Iniciales(cl.nombre)
                    Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(cl.nombre, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val cuenta = cl.citas.count { !it.cancelada }
                        val linea = buildList {
                            add(if (cuenta == 1) "1 cita" else "$cuenta citas")
                            cl.proxima?.let { add("próxima ${Mensajes.cuando(it.fecha, hoy).removePrefix("el ")}") }
                                ?: cl.ultima?.let { add("última ${Mensajes.fecha(it.fecha)}") }
                        }.joinToString(" · ")
                        Text(linea, style = MaterialTheme.typography.bodySmall, color = Papel.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Papel.linea)
                }
            }
        }
    }
}

@Composable
fun PantallaCliente(nombre: String, citas: List<Cita>, hoy: LocalDate, onAtras: () -> Unit, onVer: (Cita) -> Unit, onNueva: (Agenda.Cliente) -> Unit) {
    val context = LocalContext.current
    val cl = Agenda.clientes(citas, hoy).firstOrNull { Colonias.normalizar(it.nombre) == Colonias.normalizar(nombre) }
    if (cl == null) { LaunchedEffect(Unit) { onAtras() }; return }
    val pagado = cl.citas.sumOf { it.cobrado() }
    val debe = cl.citas.sumOf { it.porCobrar() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BarraAtras(onAtras) }
        item {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Iniciales(cl.nombre, 84.dp)
                Text(cl.nombre, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                if (cl.colonia.isNotBlank()) Text(cl.colonia, style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AccionRedonda("Llamar", Icons.Filled.Call, activo = cl.telefono.isNotBlank()) { Acciones.llamar(context, cl.telefono) }
                AccionRedonda("WhatsApp", Iconos.chat, Papel.verde, activo = cl.telefono.isNotBlank()) { Acciones.whatsapp(context, "Buenas, ${cl.nombre}.", cl.telefono) }
                AccionRedonda("Nueva cita", Iconos.calendario) { onNueva(cl) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Cifra("Pagado", Mensajes.pesos(pagado), Papel.verde, Modifier.weight(1f))
                Cifra("Debe", Mensajes.pesos(debe), if (debe > 0) Papel.aviso else Papel.tenue, Modifier.weight(1f))
            }
        }
        item { Seccion("Historial") }
        items(cl.citas, key = { it.id }) { c -> TarjetaCita(c, mostrarFecha = Mensajes.cuando(c.fecha, hoy).replaceFirstChar { it.uppercase() }) { onVer(c) } }
    }
}

@Composable
fun BarraAtras(onAtras: () -> Unit, titulo: String = "Atrás") {
    TextButton(onClick = onAtras, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Papel.azul)
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = Papel.azul)
    }
}

@Composable
fun Cifra(titulo: String, valor: String, color: Color, modifier: Modifier = Modifier) {
    Tarjeta(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(titulo.uppercase(), style = MaterialTheme.typography.labelSmall, color = Papel.tenue)
            Text(valor, style = MaterialTheme.typography.headlineMedium, color = color, maxLines = 1)
        }
    }
}

// ================================================================ Cobros

@Composable
fun SeccionCobros(citas: List<Cita>, hoy: LocalDate, onVer: (Cita) -> Unit, onPagada: (Cita) -> Unit) {
    var mes by rememberSaveable { mutableStateOf(false) }
    val desde = if (mes) hoy.withDayOfMonth(1) else hoy.minusDays((hoy.dayOfWeek.value - 1).toLong())
    val hasta = if (mes) hoy.withDayOfMonth(hoy.lengthOfMonth()) else desde.plusDays(6)
    val r = Agenda.cobros(citas, desde, hasta)
    val todasPendientes = citas.filter { it.porCobrar() > 0 && !it.fecha.isAfter(hoy) }.sortedWith(compareBy({ it.fecha }, { it.hora }))

    LazyColumn(Modifier.fillMaxSize(), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TituloSeccion("Cobros") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !mes, onClick = { mes = false }, label = { Text("Esta semana") })
                FilterChip(selected = mes, onClick = { mes = true }, label = { Text("Este mes") })
            }
        }
        item {
            AnimatedContent(targetState = r, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "cifras") { rr ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Cifra("Cobrado", Mensajes.pesos(rr.cobrado), Papel.verde, Modifier.weight(1f))
                        Cifra("Por cobrar", Mensajes.pesos(rr.porCobrar), if (rr.porCobrar > 0) Papel.aviso else Papel.tenue, Modifier.weight(1f))
                    }
                    Text(
                        "${rr.citas} citas del " + (if (desde.month == hasta.month) "${desde.dayOfMonth}" else Mensajes.fecha(desde).substringAfter(" ")) +
                            " al ${Mensajes.fecha(hasta).substringAfter(" ")}",
                        style = MaterialTheme.typography.bodyMedium, color = Papel.tenue,
                    )
                }
            }
        }
        item { Seccion("Ya pasaron y falta cobrar") }
        if (todasPendientes.isEmpty()) {
            item { Tarjeta(Modifier.fillMaxWidth()) { Vacio("Todo cobrado", "Cuando una cita ya pasó y falta dinero, aparece aquí.", Iconos.dinero) } }
        }
        items(todasPendientes, key = { it.id }) { c ->
            Tarjeta(Modifier.fillMaxWidth().animateItem(), onClick = { onVer(c) }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(c.cliente, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${c.tipo.nombre} · ${Mensajes.cuando(c.fecha, hoy)}", style = MaterialTheme.typography.bodySmall, color = Papel.tenue)
                        Text("Falta ${Mensajes.pesos(c.porCobrar())}", style = MaterialTheme.typography.titleSmall, color = Papel.aviso)
                    }
                    Boton("Cobrado", primario = true, colorFondo = Papel.verde) { onPagada(c) }
                }
            }
        }
    }
}
