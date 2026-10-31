package mx.arilabs.citas.ui

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.arilabs.citas.datos.Licencia

/** Código de este teléfono: el ANDROID_ID (16 caracteres, propio de esta app en este teléfono). */
@SuppressLint("HardwareIds")
fun codigoTelefono(context: Context): String =
    Licencia.normalizar(Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "").padStart(16, '0').take(16)

@Composable
fun PantallaActivar(
    usadas: Int,
    vencio: java.time.LocalDate? = null,
    onAtras: (() -> Unit)?,
    onActivar: (String) -> String?,
) {
    val context = LocalContext.current
    val codigo = remember { codigoTelefono(context) }
    var clave by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding()) {
        if (onAtras != null) BarraAtras(onAtras)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Papel.azulSuave), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Lock, null, tint = Papel.azul, modifier = Modifier.size(30.dp))
            }
            Text(if (vencio != null) "Renueva tu licencia" else "Activa tu app", style = MaterialTheme.typography.displaySmall)
            Text(
                if (vencio != null) "Tu licencia anual venció el ${mx.arilabs.citas.datos.Mensajes.fecha(vencio)}. Tus citas siguen guardadas; para apuntar nuevas, pide tu clave del nuevo año."
                else if (usadas >= Licencia.CITAS_DE_PRUEBA) "Ya usaste las ${Licencia.CITAS_DE_PRUEBA} citas de prueba. Para seguir apuntando, activa Mis citas con tu clave."
                else "Estás usando la versión de prueba ($usadas de ${Licencia.CITAS_DE_PRUEBA} citas). Actívala para usarla sin límite.",
                style = MaterialTheme.typography.bodyLarge, color = Papel.tenue,
            )
            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. MANDA EL CÓDIGO DE ESTE TELÉFONO", style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                    Text(
                        Licencia.bonito(codigo),
                        fontFamily = FontFamily.Monospace, fontSize = 24.sp, color = Papel.azul,
                        modifier = Modifier.fillMaxWidth().background(Papel.azulSuave, RoundedCornerShape(14.dp)).padding(vertical = 14.dp, horizontal = 12.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Boton("Copiar", Modifier.weight(1f), icono = Iconos.copiar) { Acciones.copiar(context, Licencia.bonito(codigo)) }
                        Boton("WhatsApp", Modifier.weight(1f), primario = true, icono = Iconos.chat, colorFondo = Papel.verde) {
                            Acciones.whatsapp(context, (if (vencio != null) "Hola, quiero renovar Mis citas." else "Hola, quiero activar Mis citas.") + " El código de mi teléfono es: ${Licencia.bonito(codigo)}")
                        }
                    }
                }
            }
            Tarjeta(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. PEGA LA CLAVE QUE TE MANDARON", style = MaterialTheme.typography.labelMedium, color = Papel.tenue)
                    OutlinedTextField(
                        value = clave, onValueChange = { clave = it; error = "" },
                        placeholder = { Text("Clave de activación") }, maxLines = 4,
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    )
                    Boton("Pegar clave", Modifier.fillMaxWidth(), icono = Iconos.copiar) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val texto = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        // Del mensaje completo, quedarse con la clave (lo que tiene un punto en medio).
                        clave = Regex("""[A-Za-z0-9_-]+\.\d{8}\.[A-Za-z0-9_-]{60,}""").find(texto)?.value ?: texto.trim()
                        error = ""
                    }
                    if (error.isNotBlank()) Text(error, style = MaterialTheme.typography.bodyMedium, color = Papel.rojo)
                    Boton("Activar", Modifier.fillMaxWidth(), primario = true, activo = clave.isNotBlank()) {
                        error = onActivar(clave.trim()) ?: ""
                    }
                }
            }
            Text(
                "La licencia dura un año y solo sirve en este teléfono. Si cambias de teléfono, pide una nueva a Ari Labs.",
                style = MaterialTheme.typography.bodySmall, color = Papel.tenue,
            )
        }
    }
}
