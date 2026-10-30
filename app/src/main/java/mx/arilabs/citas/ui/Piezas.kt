package mx.arilabs.citas.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Zona

/** Se encoge un poco al tocar: la tarjeta "responde" al dedo. */
@Composable
fun Modifier.alTocar(onClick: () -> Unit, etiqueta: String? = null): Modifier {
    val fuente = remember { MutableInteractionSource() }
    val apretado by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(if (apretado) 0.97f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "toque")
    return this
        .scale(escala)
        .clickable(interactionSource = fuente, indication = null, role = Role.Button, onClickLabel = etiqueta, onClick = onClick)
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    color: Color = Papel.hoja,
    radio: Dp = 22.dp,
    onClick: (() -> Unit)? = null,
    contenido: @Composable () -> Unit,
) {
    val mod = if (onClick != null) modifier.alTocar(onClick) else modifier
    Surface(
        modifier = mod,
        color = color,
        shape = RoundedCornerShape(radio),
        shadowElevation = if (color == Papel.hoja) 1.dp else 0.dp,
        tonalElevation = 0.dp,
    ) { contenido() }
}

/** Botón grande: al menos 56 dp de alto. */
@Composable
fun Boton(
    texto: String,
    modifier: Modifier = Modifier,
    primario: Boolean = false,
    activo: Boolean = true,
    icono: ImageVector? = null,
    colorFondo: Color = Papel.azul,
    onClick: () -> Unit,
) {
    val mod = modifier.heightIn(min = 56.dp)
    val cuerpo: @Composable RowScope.() -> Unit = {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (primario) {
        Button(
            onClick = onClick, enabled = activo, modifier = mod, shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorFondo),
            content = cuerpo,
        )
    } else {
        OutlinedButton(
            onClick = onClick, enabled = activo, modifier = mod, shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Papel.hoja, contentColor = Papel.azul),
            border = androidx.compose.foundation.BorderStroke(1.dp, Papel.linea),
            content = cuerpo,
        )
    }
}

/** Botón redondo con icono y texto debajo (Llamar, WhatsApp, Maps...). */
@Composable
fun AccionRedonda(texto: String, icono: ImageVector, color: Color = Papel.azul, activo: Boolean = true, onClick: () -> Unit) {
    val tinte = if (activo) color else Papel.cancelada
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .widthIn(min = 72.dp)
            .alTocar(onClick = { if (activo) onClick() }, etiqueta = texto),
    ) {
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(tinte.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icono, contentDescription = null, tint = tinte, modifier = Modifier.size(26.dp)) }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = if (activo) Papel.tinta else Papel.cancelada)
    }
}

@Composable
fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Título grande de pantalla, como en las apps de iPhone. */
@Composable
fun TituloGrande(titulo: String, subtitulo: String? = null, acciones: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitulo != null) {
                Text(subtitulo.uppercase(), style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
            }
            Text(titulo, style = MaterialTheme.typography.displaySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = acciones)
    }
}

/** Una cita en lista: hora grande, barra del color del tipo, cliente y lugar. */
@Composable
fun TarjetaCita(c: Cita, mostrarFecha: String? = null, onClick: () -> Unit) {
    val apagada = c.cancelada || c.hecha
    val colorTipo by animateColorAsState(if (apagada) Papel.cancelada else Papel.colorTipo(c.tipo), label = "tipo")
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(start = 0.dp, end = 12.dp, top = 14.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.padding(start = 10.dp).width(5.dp).height(52.dp).clip(RoundedCornerShape(3.dp)).background(colorTipo))
            Column(Modifier.widthIn(min = 84.dp).padding(start = 14.dp, end = 10.dp)) {
                val h = Mensajes.horaCorta(c.hora)
                Text(
                    h.substringBefore(" "),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (apagada) Papel.cancelada else Papel.tinta,
                    textDecoration = if (c.cancelada) TextDecoration.LineThrough else null,
                )
                Text(h.substringAfter(" "), style = MaterialTheme.typography.labelSmall, color = Papel.tenue)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                if (mostrarFecha != null) Text(mostrarFecha, style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                Text(
                    c.cliente.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (apagada) Papel.cancelada else Papel.tinta,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Etiqueta(c.tipo.nombre, colorTipo)
                    when {
                        c.cancelada -> Etiqueta("Cancelada", Papel.rojo)
                        c.hecha -> Etiqueta("Hecha", Papel.verde)
                        c.porCobrar() > 0 -> Etiqueta("Falta ${Mensajes.pesos(c.porCobrar())}", Papel.aviso)
                    }
                }
                val donde = listOf(c.colonia, if (c.zona != Zona.SIN_ZONA) c.zona.nombre else "").filter { it.isNotBlank() }.joinToString(" · ")
                if (donde.isNotBlank()) Text(donde, style = MaterialTheme.typography.bodySmall, color = Papel.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (c.hecha && !c.cancelada) {
                Icon(Icons.Filled.CheckCircle, contentDescription = "Hecha", tint = Papel.verde, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Papel.linea)
            }
        }
    }
}

@Composable
fun CajaAviso(lineas: List<String>, titulo: String = "Revisa esto antes de guardar") {
    if (lineas.isEmpty()) return
    Column(
        Modifier.fillMaxWidth().background(Papel.avisoSuave, RoundedCornerShape(18.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, color = Papel.aviso)
        lineas.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge, color = Papel.tinta) }
    }
}

/** Cuando no hay nada que mostrar: un texto amable y, si hace falta, el siguiente paso. */
@Composable
fun Vacio(titulo: String, texto: String, icono: ImageVector = Iconos.calendario) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Papel.azulSuave), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = Papel.azul, modifier = Modifier.size(30.dp))
        }
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = Papel.tenue, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun Seccion(titulo: String, extra: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, content = extra)
    }
}

/** Iniciales en un círculo de color, para la lista de clientes. */
@Composable
fun Iniciales(nombre: String, tam: Dp = 46.dp) {
    val limpio = nombre.split(' ').filter { it.isNotBlank() }
        .filterNot { it.lowercase().trimEnd('.') in setOf("doña", "dona", "don", "sra", "sr", "señora", "señor", "familia", "la", "el", "de", "del", "profe", "maestra", "maestro", "hermana", "hermano") }
    val ini = (limpio.take(2).joinToString("") { it.take(1).uppercase() }).ifBlank { "?" }
    val paleta = listOf(Color(0xFF2F5C93), Color(0xFF3F7A3A), Color(0xFF8A3A3A), Color(0xFF7A5A1E), Color(0xFF5A4A8A), Color(0xFF1E6A6A))
    val color = paleta[(nombre.lowercase().hashCode() and 0x7fffffff) % paleta.size]
    Box(Modifier.size(tam).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Text(ini, style = MaterialTheme.typography.titleSmall, color = color)
    }
}
