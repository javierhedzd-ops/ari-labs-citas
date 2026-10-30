package mx.arilabs.citas

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import mx.arilabs.citas.datos.Almacen
import mx.arilabs.citas.datos.Cita
import mx.arilabs.citas.datos.Mensajes
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Aviso en el teléfono una hora antes de cada cita. Sin internet: lo programa el propio
 * teléfono con AlarmManager. Se vuelve a programar al abrir la app, al guardar y al
 * encender el teléfono.
 */
object Recordatorios {

    private const val CANAL = "citas"
    private const val ANTES_MIN = 60L

    fun reprogramar(context: Context, citas: List<Cita>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val prefs = context.getSharedPreferences("avisos", Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet())?.forEach { id ->
            id.toLongOrNull()?.let { am.cancel(intento(context, it, null)) }
        }
        val ahora = LocalDateTime.now()
        val programadas = mutableSetOf<String>()
        citas.filter { !it.cancelada }
            .map { it to LocalDateTime.of(it.fecha, it.hora).minusMinutes(ANTES_MIN) }
            .filter { (_, cuando) -> cuando.isAfter(ahora) && cuando.isBefore(ahora.plusDays(45)) }
            .forEach { (c, cuando) ->
                val ms = cuando.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intento(context, c.id, c))
                programadas += c.id.toString()
            }
        prefs.edit().putStringSet("ids", programadas).apply()
    }

    private fun intento(context: Context, id: Long, c: Cita?): PendingIntent {
        val i = Intent(context, AvisoReceiver::class.java).setAction("mx.arilabs.citas.AVISO.$id")
        if (c != null) {
            i.putExtra("id", c.id)
            i.putExtra("titulo", "${Mensajes.horaCorta(c.hora)} · ${c.tipo.nombre} con ${c.cliente}")
            i.putExtra("texto", listOf(c.lugar, c.colonia).filter { it.isNotBlank() }.joinToString(", ").ifBlank { "En una hora" })
        }
        return PendingIntent.getBroadcast(
            context, (id % Int.MAX_VALUE).toInt(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun crearCanal(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CANAL, "Avisos de citas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Una hora antes de cada cita"
            },
        )
    }

    fun mostrar(context: Context, id: Long, titulo: String, texto: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        crearCanal(context)
        val abrir = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = android.app.Notification.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_aviso)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(android.app.Notification.BigTextStyle().bigText(texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .setCategory(android.app.Notification.CATEGORY_REMINDER)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify((id % Int.MAX_VALUE).toInt(), n)
    }
}

class AvisoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", -1)
        if (id < 0) return
        Recordatorios.mostrar(context, id, intent.getStringExtra("titulo") ?: "Tienes una cita", intent.getStringExtra("texto") ?: "")
    }
}

/** Al encender el teléfono los avisos programados se borran: se vuelven a poner. */
class ArranqueReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        Recordatorios.reprogramar(context, Almacen(context).leer())
    }
}
