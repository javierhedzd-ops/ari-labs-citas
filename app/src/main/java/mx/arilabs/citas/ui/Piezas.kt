package mx.arilabs.citas.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import mx.arilabs.citas.datos.Zona

/** Botón grande: al menos 56 dp de alto para dedos y vista cansada. */
@Composable
fun Boton(texto: String, modifier: Modifier = Modifier, primario: Boolean = false, activo: Boolean = true, onClick: () -> Unit) {
    val mod = modifier.heightIn(min = 56.dp)
    if (primario) {
        Button(onClick = onClick, enabled = activo, modifier = mod, shape = RoundedCornerShape(14.dp)) {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = activo,
            modifier = mod,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, Papel.azul),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Papel.azul),
        ) {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun Encabezado(titulo: String, atras: (() -> Unit)? = null, extra: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (atras != null) {
            TextButton(onClick = atras, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("‹ Atrás", style = MaterialTheme.typography.labelLarge, color = Papel.azul)
            }
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Text(
            titulo,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        extra?.invoke()
    }
}

@Composable
fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun TarjetaCita(c: Cita, onClick: () -> Unit) {
    val tenue = c.cancelada
    Surface(
        color = Papel.hoja,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Papel.linea),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Ver la cita", onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.width(92.dp)) {
                Text(
                    Mensajes.horaCorta(c.hora),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (tenue) Papel.cancelada else Papel.tinta,
                    textDecoration = if (tenue) TextDecoration.LineThrough else null,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Etiqueta(c.tipo.nombre, if (tenue) Papel.cancelada else Papel.colorTipo(c.tipo))
                    if (tenue) Etiqueta("Cancelada", Papel.cancelada)
                }
                Text(
                    c.cliente.ifBlank { "Sin nombre" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (tenue) Papel.cancelada else Papel.tinta,
                )
                val donde = listOf(c.colonia, c.lugar).filter { it.isNotBlank() }.joinToString(" · ")
                if (donde.isNotBlank()) {
                    Text(donde, style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
                }
                if (c.zona != Zona.SIN_ZONA) {
                    Text("Zona ${c.zona.nombre}", style = MaterialTheme.typography.bodyMedium, color = Papel.tenue)
                }
            }
        }
    }
}

@Composable
fun CajaAviso(lineas: List<String>) {
    if (lineas.isEmpty()) return
    Column(
        Modifier
            .fillMaxWidth()
            .background(Papel.avisoSuave, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Revisa esto antes de guardar", style = MaterialTheme.typography.titleMedium, color = Papel.aviso)
        lineas.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge, color = Papel.tinta) }
    }
}
