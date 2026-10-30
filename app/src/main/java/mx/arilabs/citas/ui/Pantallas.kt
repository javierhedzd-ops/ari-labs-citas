package mx.arilabs.citas.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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

// ---------------------------------------------------------------- Agenda

@Composable
fun PantallaAgenda(
    citas: List<Cita>,
    hoy: LocalDate,
    onNueva: () -> Unit,
    onVer: (Cita) -> Unit,
    onAjustes: () -> Unit,
) {
    var porZona by remember { mutableStateOf(false) }
    val deHoy = Agenda.delDia(citas, hoy).let { if (porZona) Agenda.porZona(it) else it }
    val proximos = Agenda.proximos7(citas, hoy)

    Column(Modifier.fillMaxSize()) {
        Encabezado("Mis citas", extra = {
            TextButton(onClick = onAjustes, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Ajustes", style = MaterialTheme.typography.labelLarge, color = Papel.azul)
            }
        })
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = margen,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Hoy", style = MaterialTheme.typography.titleLarge)
                Text(Mensajes.fecha(hoy), style = MaterialTheme.typography.bodyLarge, color = Papel.tenue)
            }
            if (deHoy.isEmpty()) {
                item { Text("No tienes citas hoy.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue) }
            } else {
                if (deHoy.size > 1) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !porZona, onClick = { porZona = false }, label = { Text("Por hora") })
                            FilterChip(selected = porZona, onClick = { porZona = true }, label = { Text("Por zona") })
                        }
                        if (porZona) {
                            Text(
                                "Orden para no cruzar la ciudad. Las horas no cambian.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Papel.tenue,
                            )
                        }
                    }
                }
                items(deHoy, key = { it.id }) { c -> TarjetaCita(c) { onVer(c) } }
                item { BotonesDelDia(deHoy, hoy, hoy) }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Próximos 7 días", style = MaterialTheme.typography.titleLarge)
            }
            if (proximos.isEmpty()) {
                item { Text("Nada agendado esta semana.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue) }
            }
            proximos.forEach { (dia, delDia) ->
                item(key = "dia-$dia") {
                    Text(
                        Mensajes.cuando(dia, hoy).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(delDia, key = { it.id }) { c -> TarjetaCita(c) { onVer(c) } }
                item(key = "botones-$dia") { BotonesDelDia(delDia, dia, hoy) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        Boton(
            "Nueva cita",
            primario = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onClick = onNueva,
        )
    }
}

@Composable
private fun BotonesDelDia(citas: List<Cita>, dia: LocalDate, hoy: LocalDate) {
    val context = LocalContext.current
    val texto = Mensajes.avisoDelDia(citas, dia, hoy)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Boton("Copiar el día", Modifier.weight(1f)) { Acciones.copiar(context, texto) }
        Boton("WhatsApp", Modifier.weight(1f)) { Acciones.whatsapp(context, texto) }
    }
}

// ---------------------------------------------------------------- Escribir

@Composable
fun PantallaEscribir(onAtras: () -> Unit, onOrdenar: (String) -> Unit) {
    var texto by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Encabezado("Nueva cita", atras = onAtras)
        Column(
            Modifier.weight(1f).padding(margen),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Escríbela como te salga. Yo la ordeno.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                placeholder = {
                    Text(
                        "Ejemplo: sábado 3pm, novena, colonia Chuburná, calle 21 x 30, doña Mari, 999 123 4567, \$600, anticipo 200",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
            )
        }
        Boton(
            "Ordenar",
            primario = true,
            activo = texto.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) { onOrdenar(texto) }
    }
}

// ---------------------------------------------------------------- Formulario

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PantallaFormulario(
    titulo: String,
    inicial: Borrador,
    citas: List<Cita>,
    editandoId: Long?,
    onAtras: () -> Unit,
    onGuardar: (Borrador) -> Unit,
) {
    val context = LocalContext.current
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
                d.startsWith("¿Es rezo") -> tipo == null
                d.startsWith("¿Con quién") -> cliente.isBlank()
                d.startsWith("No conozco la colonia") -> zona == Zona.SIN_ZONA
                d.startsWith("¿Dónde") -> lugar.isBlank() && colonia.isBlank()
                else -> true
            }
        })
        if (choque != null) add("Ya tienes a ${choque.cliente} a esa hora.")
    }
    val listo = fecha != null && hora != null && cliente.isNotBlank()

    Column(Modifier.fillMaxSize()) {
        Encabezado(titulo, atras = onAtras)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(margen),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CajaAviso(pendientes)

            Text("Día y hora", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Boton(fecha?.let { Mensajes.fecha(it) } ?: "Escoger día", Modifier.weight(1f)) {
                    val f = fecha ?: LocalDate.now()
                    DatePickerDialog(context, { _, a, m, d -> fecha = LocalDate.of(a, m + 1, d) }, f.year, f.monthValue - 1, f.dayOfMonth).show()
                }
            }
            Boton(hora?.let { Mensajes.hora(it) } ?: "Escoger hora", Modifier.fillMaxWidth()) {
                val h = hora ?: LocalTime.of(17, 0)
                TimePickerDialog(context, { _, hh, mm -> hora = LocalTime.of(hh, mm) }, h.hour, h.minute, false).show()
            }

            Text("Tipo", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tipo.entries.forEach { t ->
                    FilterChip(
                        selected = tipo == t,
                        onClick = { tipo = t },
                        label = { Text(t.nombre, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            Campo("Cliente", cliente) { cliente = it }
            Campo("Dirección", lugar) { lugar = it }
            Campo("Colonia", colonia) {
                colonia = it
                val z = Colonias.zonaDe(it)
                if (z != Zona.SIN_ZONA) zona = z
            }

            Text("Zona", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Zona.entries.forEach { z ->
                    FilterChip(
                        selected = zona == z,
                        onClick = { zona = z },
                        label = { Text(z.nombre, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            Campo("Teléfono", telefono, KeyboardType.Phone) { telefono = it }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Campo("Precio \$", precio, KeyboardType.Number, Modifier.weight(1f)) { precio = it.filter(Char::isDigit) }
                Campo("Anticipo \$", anticipo, KeyboardType.Number, Modifier.weight(1f)) { anticipo = it.filter(Char::isDigit) }
            }
            Campo("Notas", notas) { notas = it }

            if (inicial.texto.isNotBlank()) {
                Text("Lo que escribiste: “${inicial.texto}”", style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
            }
            Spacer(Modifier.height(8.dp))
        }
        Boton(
            if (listo) "Guardar" else "Falta día, hora o cliente",
            primario = true,
            activo = listo,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            onGuardar(
                inicial.copy(
                    fecha = fecha, hora = hora, cliente = cliente.trim(), tipo = tipo ?: Tipo.OTRO,
                    lugar = lugar.trim(), colonia = colonia.trim(), zona = zona, telefono = telefono.trim(),
                    precio = precio.toIntOrNull(), anticipo = anticipo.toIntOrNull(), notas = notas.trim(),
                ),
            )
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
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = teclado != KeyboardType.Text,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = modifier,
    )
}

// ---------------------------------------------------------------- Detalle

@Composable
fun PantallaDetalle(
    c: Cita,
    hoy: LocalDate,
    nombre: String,
    onAtras: () -> Unit,
    onEditar: () -> Unit,
    onMover: (LocalDate, LocalTime) -> Unit,
    onCancelar: (Anticipo) -> Unit,
) {
    val context = LocalContext.current
    var preguntarCancelar by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Encabezado(c.tipo.nombre, atras = onAtras)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(margen),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (c.cancelada) {
                CajaAviso(listOf("Cita cancelada. " + (c.anticipoAlCancelar?.nombre ?: "")))
            }
            Text(c.cliente, style = MaterialTheme.typography.headlineMedium)
            Text(
                Mensajes.cuando(c.fecha, hoy).replaceFirstChar { it.uppercase() } + " a las " + Mensajes.hora(c.hora),
                style = MaterialTheme.typography.titleMedium,
            )
            Dato("Dirección", c.lugar)
            Dato("Colonia", listOf(c.colonia, if (c.zona != Zona.SIN_ZONA) "zona ${c.zona.nombre}" else "").filter { it.isNotBlank() }.joinToString(", "))
            Dato("Teléfono", c.telefono)
            Dato("Precio", c.precio?.let { Mensajes.pesos(it) } ?: "")
            Dato("Anticipo", c.anticipo?.let { Mensajes.pesos(it) } ?: "")
            c.falta()?.takeIf { it > 0 && !c.cancelada }?.let { Dato("Falta cobrar", Mensajes.pesos(it)) }
            Dato("Notas", c.notas)

            Spacer(Modifier.height(8.dp))
            Boton("Cómo llegar (Maps)", Modifier.fillMaxWidth(), primario = true) { Acciones.mapa(context, c.direccionParaMapa()) }
            if (!c.cancelada) {
                Boton("Recordatorio por WhatsApp", Modifier.fillMaxWidth()) {
                    Acciones.whatsapp(context, Mensajes.recordatorio(c, nombre, hoy), c.telefono)
                }
            }
            if (c.telefono.isNotBlank()) {
                Boton("Llamar", Modifier.fillMaxWidth()) { Acciones.llamar(context, c.telefono) }
            }
            if (!c.cancelada) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Boton("Mover", Modifier.weight(1f)) {
                        DatePickerDialog(context, { _, a, m, d ->
                            val nuevaFecha = LocalDate.of(a, m + 1, d)
                            TimePickerDialog(context, { _, hh, mm -> onMover(nuevaFecha, LocalTime.of(hh, mm)) }, c.hora.hour, c.hora.minute, false).show()
                        }, c.fecha.year, c.fecha.monthValue - 1, c.fecha.dayOfMonth).show()
                    }
                    Boton("Editar", Modifier.weight(1f), onClick = onEditar)
                }
                Boton("Cancelar la cita", Modifier.fillMaxWidth()) { preguntarCancelar = true }
            }
            if (c.textoOriginal.isNotBlank()) {
                Text("Lo que escribiste: “${c.textoOriginal}”", style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (preguntarCancelar) {
        DialogoCancelar(
            hayAnticipo = (c.anticipo ?: 0) > 0,
            onCerrar = { preguntarCancelar = false },
            onConfirmar = { preguntarCancelar = false; onCancelar(it) },
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    if (valor.isBlank()) return
    Column {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
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
                Text("No se borra: queda en tu agenda tachada.", style = MaterialTheme.typography.bodyLarge)
                if (hayAnticipo) {
                    Text("¿Qué pasa con el anticipo?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                    listOf(Anticipo.SE_QUEDA, Anticipo.SE_DEVUELVE, Anticipo.OTRA_FECHA).forEach { a ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            RadioButton(selected = opcion == a, onClick = { opcion = a })
                            Text(a.nombre, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirmar(opcion) }) { Text("Sí, cancelar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("No") } },
    )
}

// ---------------------------------------------------------------- Avisos

data class Aviso(val para: String, val telefono: String, val texto: String)

/** Mensajes listos para mandar después de mover o cancelar. Cada uno se puede corregir. */
@Composable
fun PantallaAvisos(titulo: String, explicacion: String, avisos: List<Aviso>, onListo: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Encabezado(titulo)
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = margen,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { Text(explicacion, style = MaterialTheme.typography.bodyLarge) }
            if (avisos.isEmpty()) {
                item { Text("No hay a quién avisar.", style = MaterialTheme.typography.bodyLarge, color = Papel.tenue) }
            }
            items(avisos) { a ->
                var texto by remember(a) { mutableStateOf(a.texto) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(a.para.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { texto = it },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Boton("WhatsApp", Modifier.weight(1f), primario = true) { Acciones.whatsapp(context, texto, a.telefono) }
                        Boton("Copiar", Modifier.weight(1f)) { Acciones.copiar(context, texto) }
                    }
                }
            }
        }
        Boton("Listo", Modifier.fillMaxWidth().padding(16.dp), primario = true, onClick = onListo)
    }
}

// ---------------------------------------------------------------- Ajustes

@Composable
fun PantallaAjustes(nombre: String, primeraVez: Boolean, onAtras: () -> Unit, onGuardar: (String) -> Unit) {
    var valor by remember { mutableStateOf(nombre) }
    Column(Modifier.fillMaxSize()) {
        Encabezado(if (primeraVez) "Para empezar" else "Ajustes", atras = if (primeraVez) null else onAtras)
        Column(Modifier.weight(1f).padding(margen), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "¿Cómo firmas tus mensajes? Va al inicio de cada aviso: “Le escribe …”.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = valor,
                onValueChange = { valor = it },
                label = { Text("Tu nombre") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Todo se guarda solo en este teléfono. La app no usa internet.",
                style = MaterialTheme.typography.bodyMedium,
                color = Papel.tenue,
            )
        }
        Boton("Guardar", Modifier.fillMaxWidth().padding(16.dp), primario = true, activo = valor.isNotBlank()) { onGuardar(valor) }
    }
}
