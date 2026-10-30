package mx.arilabs.citas.ui

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.arilabs.citas.datos.Cancion
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Momento
import mx.arilabs.citas.datos.Repertorio
import mx.arilabs.citas.lector.Colonias

// ================================================================ Repertorio (pestaña)

@Composable
fun SeccionRepertorio(repertorio: List<Cancion>, onGuardar: (Cancion) -> Unit, onBorrar: (Long) -> Unit) {
    var buscar by rememberSaveable { mutableStateOf("") }
    var editando by remember { mutableStateOf<Cancion?>(null) }
    val q = Colonias.normalizar(buscar)
    val lista = repertorio.filter { q.isBlank() || Colonias.normalizar(it.titulo + " " + it.tono + " " + it.momento.nombre).contains(q) }
    val porMomento = Momento.entries.associateWith { m -> lista.filter { it.momento == m }.sortedBy { it.titulo } }.filterValues { it.isNotEmpty() }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Repertorio", style = MaterialTheme.typography.displaySmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { editando = Cancion(id = 0, titulo = "") }) { Icon(Icons.Filled.Add, "Agregar canción", tint = Papel.azul) }
            }
            Text("${repertorio.size} canciones", style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
        }
        item {
            OutlinedTextField(
                value = buscar, onValueChange = { buscar = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Buscar canción, tono o momento") },
                shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            )
        }
        if (lista.isEmpty()) {
            item { Tarjeta(Modifier.fillMaxWidth()) { Vacio("Sin canciones", "Toca + para agregar la primera.", Iconos.musica) } }
        }
        porMomento.forEach { (m, canciones) ->
            item(key = "m-${m.name}") { Seccion(m.nombre) }
            items(canciones, key = { it.id }) { can ->
                Tarjeta(Modifier.fillMaxWidth().animateItem(), onClick = { editando = can }) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Papel.azulSuave), contentAlignment = Alignment.Center) {
                            Icon(Iconos.musica, null, tint = Papel.azul, modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(can.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (can.tono.isNotBlank() || can.notas.isNotBlank()) {
                                Text(listOf(if (can.tono.isNotBlank()) "Tono ${can.tono}" else "", can.notas).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Papel.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }

    editando?.let { c ->
        DialogoCancion(
            inicial = c,
            onCerrar = { editando = null },
            onGuardar = { onGuardar(it); editando = null },
            onBorrar = if (c.id != 0L) ({ onBorrar(c.id); editando = null }) else null,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogoCancion(inicial: Cancion, onCerrar: () -> Unit, onGuardar: (Cancion) -> Unit, onBorrar: (() -> Unit)?) {
    var titulo by remember { mutableStateOf(inicial.titulo) }
    var tono by remember { mutableStateOf(inicial.tono) }
    var notas by remember { mutableStateOf(inicial.notas) }
    var momento by remember { mutableStateOf(inicial.momento) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text(if (inicial.id == 0L) "Nueva canción" else "Editar canción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = tono, onValueChange = { tono = it }, label = { Text("Tono (Do, Re, Sol…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = notas, onValueChange = { notas = it }, label = { Text("Notas (capo, ritmo…)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Momento", style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Momento.entries.forEach { m -> FilterChip(selected = momento == m, onClick = { momento = m }, label = { Text(m.nombre) }) }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = titulo.isNotBlank(), onClick = { onGuardar(inicial.copy(titulo = titulo.trim(), tono = tono.trim(), notas = notas.trim(), momento = momento)) }) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onBorrar != null) TextButton(onClick = onBorrar) { Text("Borrar", color = Papel.rojo) }
                TextButton(onClick = onCerrar) { Text("Cancelar") }
            }
        },
    )
}

// ================================================================ Elegir canciones de un evento

@Composable
fun PantallaElegirCanciones(c: Cita, repertorio: List<Cancion>, onAtras: () -> Unit, onListo: (List<Long>) -> Unit) {
    val elegidas = remember { mutableStateListOf<Long>().apply { addAll(c.canciones.filter { id -> repertorio.any { it.id == id } }) } }
    val sugeridos = Repertorio.momentosDe(c.tipo)
    val orden = sugeridos + Momento.entries.filter { it !in sugeridos }
    Column(Modifier.fillMaxSize()) {
        BarraAtras(onAtras, "Cancelar")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text("Canciones", style = MaterialTheme.typography.displaySmall)
                Text("${c.tipo.nombre} de ${c.cliente}. Marca las que vas a tocar.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue)
            }
            if (elegidas.isNotEmpty()) {
                item { Seccion("En este orden") }
                items(elegidas.toList(), key = { "o-$it" }) { id ->
                    val can = repertorio.first { it.id == id }
                    val i = elegidas.indexOf(id)
                    Tarjeta(Modifier.fillMaxWidth().animateItem()) {
                        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = Papel.azul)
                            Text(can.titulo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(enabled = i > 0, onClick = { elegidas.removeAt(i); elegidas.add(i - 1, id) }) { Icon(Icons.Filled.KeyboardArrowUp, "Subir") }
                            IconButton(enabled = i < elegidas.lastIndex, onClick = { elegidas.removeAt(i); elegidas.add(i + 1, id) }) { Icon(Icons.Filled.KeyboardArrowDown, "Bajar") }
                            IconButton(onClick = { elegidas.remove(id) }) { Icon(Icons.Filled.Close, "Quitar", tint = Papel.tenue) }
                        }
                    }
                }
            }
            orden.forEach { m ->
                val deEste = repertorio.filter { it.momento == m }.sortedBy { it.titulo }
                if (deEste.isNotEmpty()) {
                    item(key = "t-${m.name}") { Seccion(m.nombre + if (m in sugeridos) " · sugerido" else "") }
                    items(deEste, key = { "c-${it.id}" }) { can ->
                        val marcada = can.id in elegidas
                        Tarjeta(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                    .toggleable(value = marcada, role = Role.Checkbox, onValueChange = { if (it) elegidas.add(can.id) else elegidas.remove(can.id) })
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = marcada, onCheckedChange = null)
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(can.titulo, style = MaterialTheme.typography.bodyLarge)
                                    if (can.tono.isNotBlank()) Text("Tono ${can.tono}", style = MaterialTheme.typography.bodySmall, color = Papel.tenue)
                                }
                            }
                        }
                    }
                }
            }
        }
        Boton("Listo · ${elegidas.size} canciones", Modifier.fillMaxWidth().padding(16.dp), primario = true) { onListo(elegidas.toList()) }
    }
}

// ================================================================ Modo tocar

/** Pantalla negra, letra gigante, la pantalla no se apaga. Toca a la derecha para la siguiente. */
@Composable
fun PantallaTocar(c: Cita, canciones: List<Cancion>, onSalir: () -> Unit) {
    val vista = LocalView.current
    DisposableEffect(Unit) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
    var i by rememberSaveable { mutableIntStateOf(0) }
    val hielo = Color(0xFF9ED8EE)
    Box(Modifier.fillMaxSize().background(Color.Black).safeDrawingPadding()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${c.tipo.nombre.uppercase()} · ${c.cliente.uppercase()}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8C939A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${Mensajes.horaCorta(c.hora)} a ${Mensajes.horaCorta(c.termina())}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8C939A))
                }
                IconButton(onClick = onSalir) { Icon(Icons.Filled.Close, "Salir", tint = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            AnimatedContent(
                targetState = i,
                transitionSpec = {
                    val adelante = targetState > initialState
                    (slideInHorizontally(tween(260)) { if (adelante) it / 3 else -it / 3 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { if (adelante) -it / 3 else it / 3 } + fadeOut(tween(180)))
                },
                label = "cancion",
            ) { k ->
                val can = canciones.getOrNull(k)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${k + 1} DE ${canciones.size}" + (can?.let { " · ${it.momento.nombre.uppercase()}" } ?: ""), color = hielo, style = MaterialTheme.typography.labelLarge)
                    Text(can?.titulo ?: "", color = Color.White, fontSize = 44.sp, lineHeight = 50.sp, fontWeight = FontWeight.Bold)
                    if (can != null && can.tono.isNotBlank()) Text("Tono ${can.tono}", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                    if (can != null && can.notas.isNotBlank()) Text(can.notas, color = Color(0xFFB9BFC5), fontSize = 20.sp)
                }
            }
            Spacer(Modifier.weight(1f))
            canciones.getOrNull(i + 1)?.let { sig ->
                Text("Sigue: ${sig.titulo}" + if (sig.tono.isNotBlank()) " (${sig.tono})" else "", color = Color(0xFF8C939A), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(14.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BotonTocar("Anterior", Icons.AutoMirrored.Filled.KeyboardArrowLeft, i > 0, Modifier.weight(1f)) { i-- }
                BotonTocar("Siguiente", Icons.AutoMirrored.Filled.KeyboardArrowRight, i < canciones.lastIndex, Modifier.weight(1.4f), primario = true) { i++ }
            }
        }
    }
}

@Composable
private fun BotonTocar(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector, activo: Boolean, modifier: Modifier, primario: Boolean = false, onClick: () -> Unit) {
    val fondo = if (primario) Color(0xFF9ED8EE) else Color(0xFF1A1D20)
    val tinta = if (primario) Color(0xFF0E0F11) else Color.White
    Row(
        modifier.heightIn(min = 76.dp).clip(RoundedCornerShape(22.dp)).background(if (activo) fondo else Color(0xFF15171A))
            .alTocar({ if (activo) onClick() }, texto),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, null, tint = if (activo) tinta else Color(0xFF4A4F55))
        Text(texto, color = if (activo) tinta else Color(0xFF4A4F55), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    }
}
