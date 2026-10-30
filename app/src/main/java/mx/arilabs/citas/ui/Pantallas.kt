package mx.arilabs.citas.ui

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mx.arilabs.citas.datos.Agenda
import mx.arilabs.citas.datos.Anticipo
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Tipo
import mx.arilabs.citas.datos.Zona
import mx.arilabs.citas.lector.Borrador
import mx.arilabs.citas.lector.Colonias
import java.time.LocalDate
import java.time.LocalTime

private val margen = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

// ================================================================ Escribir

private val ejemplos = listOf(
    "sábado 3pm, novena, colonia Chuburná, doña Mari, 999 123 4567",
    "mañana a las 7 rezo con don Pedro en calle 60 x 45 centro, \$500, anticipo 200",
    "el 15 de noviembre misa 10am iglesia de Santiago, familia Pech",
)

@Composable
fun PantallaEscribir(inicial: String, onAtras: () -> Unit, onOrdenar: (String) -> Unit) {
    val context = LocalContext.current
    var texto by rememberSaveable { mutableStateOf(inicial) }
    val voz = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            val dicho = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (dicho.isNotBlank()) texto = listOf(texto.trim(), dicho).filter { it.isNotBlank() }.joinToString(", ")
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        BarraAtras(onAtras, "Cancelar")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Nueva cita", style = MaterialTheme.typography.displaySmall)
            Text("Escríbela o díctala como te salga. Yo la ordeno.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue)
            Tarjeta(Modifier.fillMaxWidth()) {
                Box {
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 170.dp),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        placeholder = { Text("Ejemplo: sábado 3pm, colonia Chuburná, doña Mari", style = MaterialTheme.typography.bodyLarge) },
                        shape = RoundedCornerShape(22.dp),
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(Papel.azul).alTocar({
                        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Dime la cita")
                        try { voz.launch(i) } catch (_: Exception) {
                            Toast.makeText(context, "Este teléfono no tiene dictado. Usa el micrófono del teclado.", Toast.LENGTH_LONG).show()
                        }
                    }, "Dictar con la voz"),
                    contentAlignment = Alignment.Center,
                ) { Icon(Iconos.microfono, contentDescription = "Dictar", tint = Color.White, modifier = Modifier.size(30.dp)) }
                Text("Toca y dime la cita", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue)
            }
            if (texto.isBlank()) {
                Text("O prueba un ejemplo:", style = MaterialTheme.typography.titleSmall, color = Papel.tenue)
                ejemplos.forEach { e ->
                    AssistChip(onClick = { texto = e }, label = { Text(e, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 6.dp)) }, shape = RoundedCornerShape(14.dp))
                }
            }
        }
        Boton("Ordenar", Modifier.fillMaxWidth().padding(16.dp), primario = true, activo = texto.isNotBlank()) { onOrdenar(texto) }
    }
}

// ================================================================ Revisar / editar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaFormulario(
    titulo: String,
    inicial: Borrador,
    citas: List<Cita>,
    editandoId: Long?,
    onAtras: () -> Unit,
    onGuardar: (Borrador, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var nueveDias by remember { mutableStateOf(false) }
    var fecha by remember { mutableStateOf(inicial.fecha) }
    var hora by remember { mutableStateOf(inicial.hora) }
    var cliente by remember { mutableStateOf(inicial.cliente) }
    var tipo by remember { mutableStateOf(inicial.tipo) }
    var lugar by remember { mutableStateOf(inicial.lugar) }
    var colonia by remember { mutableStateOf(inicial.colonia) }
    var zona by remember { mutableStateOf(inicial.zona) }
    var telefono by remember { mutableStateOf(inicial.telefono) }
    var precio by remember { mutableStateOf(inicial.precio?.toString() ?: "") }
    var anticipo by remember { mutableStateOf(inicial.anticipo?.toString() ?: "") }
    var notas by remember { mutableStateOf(inicial.notas) }

    val choque = if (fecha != null && hora != null) Agenda.choca(citas, fecha!!, hora!!, editandoId) else null
    val pendientes = buildList {
        addAll(inicial.dudas.filter { d ->
            when {
                d.startsWith("¿Qué día") -> fecha == null
                d.startsWith("¿A qué hora") -> hora == null
                d.startsWith("¿Las") -> hora == inicial.hora
                d.startsWith("¿Es rezo") -> tipo == null
                d.startsWith("¿Con quién") -> cliente.isBlank()
                d.startsWith("¿La cita es con") -> cliente == inicial.cliente
                d.startsWith("No conozco la colonia") -> zona == Zona.SIN_ZONA
                d.startsWith("¿Dónde") -> lugar.isBlank() && colonia.isBlank()
                else -> true
            }
        })
        if (choque != null) add("Ya tienes a ${choque.cliente} a esa hora.")
    }
    val listo = fecha != null && hora != null && cliente.isNotBlank()

    Column(Modifier.fillMaxSize().imePadding()) {
        BarraAtras(onAtras)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(titulo, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(horizontal = 4.dp))
            AnimatedVisibility(pendientes.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                CajaAviso(pendientes)
            }

            Grupo("Cuándo") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Boton(fecha?.let { Mensajes.fecha(it) } ?: "Escoger día", Modifier.weight(1f), icono = Iconos.calendario) {
                        val f = fecha ?: LocalDate.now()
                        DatePickerDialog(context, { _, a, m, d -> fecha = LocalDate.of(a, m + 1, d) }, f.year, f.monthValue - 1, f.dayOfMonth).show()
                    }
                }
                Boton(hora?.let { Mensajes.hora(it) } ?: "Escoger hora", Modifier.fillMaxWidth(), icono = Iconos.reloj) {
                    val h = hora ?: LocalTime.of(17, 0)
                    TimePickerDialog(context, { _, hh, mm -> hora = LocalTime.of(hh, mm) }, h.hour, h.minute, false).show()
                }
            }

            Grupo("Qué es") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Tipo.entries.forEach { t ->
                        FilterChip(
                            selected = tipo == t, onClick = { tipo = t },
                            label = { Text(t.nombre, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                if (tipo == Tipo.NOVENA && editandoId == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .toggleable(value = nueveDias, role = Role.Checkbox, onValueChange = { nueveDias = it }),
                    ) {
                        Checkbox(checked = nueveDias, onCheckedChange = null)
                        Text("Son 9 días seguidos: apuntar las 9 noches", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            Grupo("Con quién") {
                Campo("Cliente", cliente) { cliente = it }
                Campo("Teléfono", telefono, KeyboardType.Phone) { telefono = it }
            }

            Grupo("Dónde") {
                Campo("Dirección", lugar) { lugar = it }
                Campo("Colonia", colonia) {
                    colonia = it
                    val z = Colonias.zonaDe(it)
                    if (z != Zona.SIN_ZONA) zona = z
                }
                Text("Zona", style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Zona.entries.forEach { z ->
                        FilterChip(selected = zona == z, onClick = { zona = z }, label = { Text(z.nombre) }, modifier = Modifier.heightIn(min = 44.dp))
                    }
                }
            }

            Grupo("Dinero") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Campo("Precio \$", precio, KeyboardType.Number, Modifier.weight(1f)) { precio = it.filter(Char::isDigit) }
                    Campo("Anticipo \$", anticipo, KeyboardType.Number, Modifier.weight(1f)) { anticipo = it.filter(Char::isDigit) }
                }
            }

            Grupo("Notas") { Campo("Algo que no se te olvide", notas) { notas = it } }

            if (inicial.texto.isNotBlank()) {
                Text("Lo que escribiste: “${inicial.texto}”", style = MaterialTheme.typography.bodySmall, color = Papel.tenue, modifier = Modifier.padding(horizontal = 4.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
        Boton(
            if (listo) "Guardar" else "Falta día, hora o cliente",
            Modifier.fillMaxWidth().padding(16.dp),
            primario = true, activo = listo, icono = if (listo) Icons.Filled.Check else null,
        ) {
            onGuardar(
                inicial.copy(
                    fecha = fecha, hora = hora, cliente = cliente.trim(), tipo = tipo ?: Tipo.OTRO,
                    lugar = lugar.trim(), colonia = colonia.trim(), zona = zona, telefono = telefono.trim(),
                    precio = precio.toIntOrNull(), anticipo = anticipo.toIntOrNull(), notas = notas.trim(),
                ),
                nueveDias && tipo == Tipo.NOVENA && editandoId == null,
            )
        }
    }
}

@Composable
private fun Grupo(titulo: String, contenido: @Composable () -> Unit) {
    Tarjeta(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo.uppercase(), style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
            contenido()
        }
    }
}

@Composable
private fun Campo(
    etiqueta: String,
    valor: String,
    teclado: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier.fillMaxWidth(),
    onCambio: (String) -> Unit,
) {
    OutlinedTextField(
        value = valor, onValueChange = onCambio, label = { Text(etiqueta) },
        singleLine = teclado != KeyboardType.Text,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    )
}

// ================================================================ Detalle

@Composable
fun PantallaDetalle(
    c: Cita,
    hoy: LocalDate,
    nombre: String,
    onAtras: () -> Unit,
    onEditar: () -> Unit,
    onMover: (LocalDate, LocalTime) -> Unit,
    onCancelar: (Anticipo) -> Unit,
    onBorrar: () -> Unit,
    onHecha: (Boolean) -> Unit,
    onPagada: (Boolean) -> Unit,
    onCliente: () -> Unit,
) {
    val context = LocalContext.current
    var preguntarCancelar by remember { mutableStateOf(false) }
    var preguntarBorrar by remember { mutableStateOf(false) }
    val color = if (c.cancelada) Papel.cancelada else Papel.colorTipo(c.tipo)

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BarraAtras(onAtras)
                Spacer(Modifier.weight(1f))
                if (!c.cancelada) IconButton(onClick = onEditar) { Icon(Icons.Filled.Edit, "Editar", tint = Papel.azul) }
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth(), color = color, radio = 28.dp) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        (if (c.cancelada) "CANCELADA · " else if (c.hecha) "HECHA · " else "") + c.tipo.nombre.uppercase(),
                        style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f),
                    )
                    Text(c.cliente, style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        Mensajes.cuando(c.fecha, hoy).replaceFirstChar { it.uppercase() } + " · " + Mensajes.hora(c.hora),
                        style = MaterialTheme.typography.titleMedium, color = Color.White,
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AccionRedonda("Llamar", Icons.Filled.Call, activo = c.telefono.isNotBlank()) { Acciones.llamar(context, c.telefono) }
                AccionRedonda("WhatsApp", Iconos.chat, Papel.verde, activo = !c.cancelada) {
                    Acciones.whatsapp(context, Mensajes.recordatorio(c, nombre, hoy), c.telefono)
                }
                AccionRedonda("Llegar", Icons.Filled.Place) { Acciones.mapa(context, c.direccionParaMapa()) }
                AccionRedonda("Mover", Iconos.reloj, activo = !c.cancelada) {
                    DatePickerDialog(context, { _, a, m, d ->
                        val nueva = LocalDate.of(a, m + 1, d)
                        TimePickerDialog(context, { _, hh, mm -> onMover(nueva, LocalTime.of(hh, mm)) }, c.hora.hour, c.hora.minute, false).show()
                    }, c.fecha.year, c.fecha.monthValue - 1, c.fecha.dayOfMonth).show()
                }
            }
        }
        if (c.cancelada) {
            item { CajaAviso(listOf(c.anticipoAlCancelar?.nombre ?: "Sin anticipo"), "Cita cancelada") }
        } else {
            item {
                Tarjeta(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Interruptor("Ya se hizo", c.hecha) { onHecha(it) }
                        if ((c.precio ?: 0) > 0) {
                            HorizontalDivider(color = Papel.linea, modifier = Modifier.padding(horizontal = 16.dp))
                            Interruptor(if (c.pagada) "Pagada" else "Falta cobrar ${Mensajes.pesos(c.porCobrar())}", c.pagada) { onPagada(it) }
                        }
                    }
                }
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    Dato("Dirección", c.lugar)
                    Dato("Colonia", listOf(c.colonia, if (c.zona != Zona.SIN_ZONA) "zona ${c.zona.nombre}" else "").filter { it.isNotBlank() }.joinToString(" · "))
                    Dato("Teléfono", c.telefono)
                    Dato("Precio", c.precio?.let { Mensajes.pesos(it) } ?: "")
                    Dato("Anticipo", c.anticipo?.let { Mensajes.pesos(it) } ?: "")
                    Dato("Notas", c.notas)
                }
            }
        }
        item {
            Tarjeta(Modifier.fillMaxWidth(), onClick = onCliente) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Iniciales(c.cliente, 40.dp)
                    Text("Ver todo de ${c.cliente}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f).padding(start = 12.dp), color = Papel.azul)
                }
            }
        }
        if (c.textoOriginal.isNotBlank()) {
            item { Text("Lo que escribiste: “${c.textoOriginal}”", style = MaterialTheme.typography.bodySmall, color = Papel.tenue) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!c.cancelada) Boton("Cancelar cita", Modifier.weight(1f)) { preguntarCancelar = true }
                TextButton(onClick = { preguntarBorrar = true }, modifier = Modifier.heightIn(min = 56.dp)) {
                    Text("Borrar", style = MaterialTheme.typography.labelLarge, color = Papel.rojo)
                }
            }
        }
    }

    if (preguntarCancelar) {
        DialogoCancelar(hayAnticipo = (c.anticipo ?: 0) > 0, onCerrar = { preguntarCancelar = false }, onConfirmar = { preguntarCancelar = false; onCancelar(it) })
    }
    if (preguntarBorrar) {
        AlertDialog(
            onDismissRequest = { preguntarBorrar = false },
            title = { Text("¿Borrar esta cita?") },
            text = { Text("Se quita para siempre. Si el cliente canceló, mejor usa “Cancelar cita”.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = { TextButton(onClick = { preguntarBorrar = false; onBorrar() }) { Text("Sí, borrar", color = Papel.rojo) } },
            dismissButton = { TextButton(onClick = { preguntarBorrar = false }) { Text("No") } },
        )
    }
}

@Composable
private fun Interruptor(texto: String, valor: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).toggleable(value = valor, role = Role.Switch, onValueChange = onCambio).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = valor, onCheckedChange = null)
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(etiqueta.uppercase(), style = MaterialTheme.typography.labelSmall, color = Papel.tenue)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DialogoCancelar(hayAnticipo: Boolean, onCerrar: () -> Unit, onConfirmar: (Anticipo) -> Unit) {
    var opcion by remember { mutableStateOf(if (hayAnticipo) Anticipo.SE_QUEDA else Anticipo.SIN_ANTICIPO) }
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("¿Cancelar la cita?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No se borra: queda en tu agenda como cancelada.", style = MaterialTheme.typography.bodyLarge)
                if (hayAnticipo) {
                    Text("¿Qué pasa con el anticipo?", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    listOf(Anticipo.SE_QUEDA, Anticipo.SE_DEVUELVE, Anticipo.OTRA_FECHA).forEach { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(value = opcion == a, role = Role.RadioButton, onValueChange = { opcion = a }),
                        ) {
                            RadioButton(selected = opcion == a, onClick = null)
                            Text(a.nombre, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirmar(opcion) }) { Text("Sí, cancelar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("No") } },
    )
}

// ================================================================ Avisos

data class Aviso(val para: String, val telefono: String, val texto: String)

@Composable
fun PantallaAvisos(titulo: String, explicacion: String, avisos: List<Aviso>, onListo: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().imePadding()) {
        LazyColumn(Modifier.weight(1f), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Column(Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Papel.verdeSuave), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Check, null, tint = Papel.verde, modifier = Modifier.size(30.dp))
                    }
                    Text(titulo, style = MaterialTheme.typography.displaySmall)
                    Text(explicacion, style = MaterialTheme.typography.bodyLarge, color = Papel.tenue)
                }
            }
            if (avisos.isEmpty()) item { Text("No hay a quién avisar.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue) }
            items(avisos) { a ->
                var texto by remember(a) { mutableStateOf(a.texto) }
                Tarjeta(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Iniciales(a.para, 36.dp)
                            Text(a.para.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
                        }
                        OutlinedTextField(value = texto, onValueChange = { texto = it }, textStyle = MaterialTheme.typography.bodyLarge, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Boton("WhatsApp", Modifier.weight(1f), primario = true, icono = Iconos.chat, colorFondo = Papel.verde) { Acciones.whatsapp(context, texto, a.telefono) }
                            Boton("Copiar", Modifier.weight(1f), icono = Iconos.copiar) { Acciones.copiar(context, texto) }
                        }
                    }
                }
            }
        }
        Boton("Listo", Modifier.fillMaxWidth().padding(16.dp), primario = true, onClick = onListo)
    }
}

// ================================================================ Buscar

@Composable
fun PantallaBuscar(citas: List<Cita>, hoy: LocalDate, onAtras: () -> Unit, onVer: (Cita) -> Unit) {
    var buscar by rememberSaveable { mutableStateOf("") }
    val q = Colonias.normalizar(buscar)
    val lista = citas
        .filter { c -> q.isBlank() || Colonias.normalizar(listOf(c.cliente, c.colonia, c.lugar, c.telefono, c.tipo.nombre, c.notas).joinToString(" ")).contains(q) }
        .sortedWith(compareBy<Cita> { it.fecha.isBefore(hoy) }.thenBy { if (it.fecha.isBefore(hoy)) -it.fecha.toEpochDay() else it.fecha.toEpochDay() }.thenBy { it.hora })
    Column(Modifier.fillMaxSize()) {
        BarraAtras(onAtras)
        OutlinedTextField(
            value = buscar, onValueChange = { buscar = it }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Nombre, colonia, teléfono…") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.weight(1f), contentPadding = margen, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (lista.isEmpty()) item { Vacio(if (citas.isEmpty()) "Aún no tienes citas" else "No encontré nada", "Prueba con otra palabra.") }
            items(lista, key = { it.id }) { c ->
                TarjetaCita(c, mostrarFecha = Mensajes.cuando(c.fecha, hoy).replaceFirstChar { it.uppercase() } + if (c.fecha.isBefore(hoy)) " · ya pasó" else "") { onVer(c) }
            }
        }
    }
}

// ================================================================ Bienvenida y ajustes

@Composable
fun PantallaBienvenida(onEmpezar: (String, Boolean) -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var ejemplo by rememberSaveable { mutableStateOf(true) }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)).background(Papel.azul), contentAlignment = Alignment.Center) {
            Icon(Iconos.calendario, null, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Text("Mis citas", style = MaterialTheme.typography.displaySmall)
        Text("Tu agenda de trabajo, ordenada sola.", style = MaterialTheme.typography.titleMedium, color = Papel.tenue)
        listOf(
            Triple(Iconos.chat, "Escribe como hablas", "“sábado 3pm, Chuburná, doña Mari” y la app la ordena."),
            Triple(Icons.Filled.Place, "Llega sin perderte", "Maps y WhatsApp a un toque en cada cita."),
            Triple(Iconos.reloj, "Nunca se te pasa", "Te avisa una hora antes. Funciona sin internet."),
        ).forEach { (ic, t, d) ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Papel.azulSuave), contentAlignment = Alignment.Center) { Icon(ic, null, tint = Papel.azul) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(t, style = MaterialTheme.typography.titleSmall)
                    Text(d, style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
                }
            }
        }
        OutlinedTextField(
            value = nombre, onValueChange = { nombre = it }, singleLine = true,
            label = { Text("¿Cómo firmas tus mensajes?") }, placeholder = { Text("Ejemplo: Don Javier") },
            shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth().toggleable(value = ejemplo, role = Role.Switch, onValueChange = { ejemplo = it }),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Ver citas de ejemplo", style = MaterialTheme.typography.bodyLarge)
                Text("Para conocer la app. Las borras en Ajustes.", style = MaterialTheme.typography.bodySmall, color = Papel.tenue)
            }
            Switch(checked = ejemplo, onCheckedChange = null)
        }
        Boton("Empezar", Modifier.fillMaxWidth(), primario = true, activo = nombre.isNotBlank()) { onEmpezar(nombre.trim(), ejemplo) }
    }
}

@Composable
fun PantallaAjustes(
    nombre: String,
    hayEjemplos: Boolean,
    onAtras: () -> Unit,
    onNombre: (String) -> Unit,
    onCopia: () -> String,
    onRecuperar: (String) -> String,
    onEjemplos: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    var valor by rememberSaveable { mutableStateOf(nombre) }
    var pegado by remember { mutableStateOf("") }
    var resultado by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        BarraAtras(onAtras)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Ajustes", style = MaterialTheme.typography.displaySmall)
            Grupo("Tu firma") {
                Campo("Tu nombre en los mensajes", valor) { valor = it }
                Boton("Guardar nombre", Modifier.fillMaxWidth(), activo = valor.isNotBlank() && valor.trim() != nombre) { onNombre(valor.trim()); Toast.makeText(context, "Guardado", Toast.LENGTH_SHORT).show() }
            }
            Grupo("Copia de tus citas") {
                Text("Si cambias o pierdes el teléfono, con la copia recuperas todo. Mándatela por WhatsApp de vez en cuando.", style = MaterialTheme.typography.bodyMedium)
                Boton("Mandarme la copia", Modifier.fillMaxWidth(), primario = true, icono = Iconos.chat, colorFondo = Papel.verde) { Acciones.whatsapp(context, onCopia()) }
                OutlinedTextField(value = pegado, onValueChange = { pegado = it }, label = { Text("Pega aquí una copia") }, maxLines = 4, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Boton("Recuperar la copia", Modifier.fillMaxWidth(), activo = pegado.isNotBlank()) { resultado = onRecuperar(pegado); pegado = "" }
                if (resultado.isNotBlank()) Text(resultado, style = MaterialTheme.typography.bodyLarge, color = Papel.azul)
            }
            if (hayEjemplos) {
                Grupo("Citas de ejemplo") {
                    Text("Tienes citas de ejemplo para conocer la app.", style = MaterialTheme.typography.bodyMedium)
                    Boton("Quitar las citas de ejemplo", Modifier.fillMaxWidth()) { onEjemplos(false) }
                }
            } else {
                Grupo("Demostración") {
                    Text("Llena la agenda con citas de ejemplo para enseñarla.", style = MaterialTheme.typography.bodyMedium)
                    Boton("Poner citas de ejemplo", Modifier.fillMaxWidth()) { onEjemplos(true) }
                }
            }
            Text(
                "Mis citas · Ari Labs · Todo se guarda solo en este teléfono. La app no usa internet.",
                style = MaterialTheme.typography.bodySmall, color = Papel.tenue, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            )
        }
    }
}
