package mx.arilabs.citas.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Lo que sale de la app: WhatsApp, Maps, llamar y copiar. La app no usa internet;
 * estas acciones abren las otras apps del teléfono.
 */
object Acciones {

    fun copiar(context: Context, texto: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Cita", texto))
        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
    }

    /** Con número abre el chat de esa persona; sin número deja escoger a quién mandarlo. */
    fun whatsapp(context: Context, texto: String, telefono: String = "") {
        val digitos = telefono.filter(Char::isDigit).takeLast(10)
        if (digitos.length == 10) {
            val uri = Uri.parse("https://wa.me/52$digitos?text=" + Uri.encode(texto))
            // Primero WhatsApp Business: es donde el tecladista habla con sus clientes.
            if (abrir(context, Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp.w4b"))) return
            if (abrir(context, Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp"))) return
            if (abrir(context, Intent(Intent.ACTION_VIEW, uri))) return
        }
        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
        if (abrir(context, Intent(envio).setPackage("com.whatsapp.w4b"))) return
        if (abrir(context, Intent(envio).setPackage("com.whatsapp"))) return
        if (abrir(context, Intent.createChooser(envio, "Mandar con…"))) return
        copiar(context, texto)
    }

    fun mapa(context: Context, direccion: String) {
        // Si el cliente mandó su ubicación, se abre ese punto exacto.
        if (direccion.startsWith("http")) {
            if (abrir(context, Intent(Intent.ACTION_VIEW, Uri.parse(direccion)))) return
        }
        val uri = Uri.parse("geo:0,0?q=" + Uri.encode(direccion))
        if (abrir(context, Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))) return
        if (abrir(context, Intent(Intent.ACTION_VIEW, uri))) return
        Toast.makeText(context, "No encontré una app de mapas", Toast.LENGTH_SHORT).show()
    }

    fun llamar(context: Context, telefono: String) {
        val t = telefono.filter(Char::isDigit)
        if (t.isEmpty()) return
        abrir(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$t")))
    }

    private fun abrir(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
