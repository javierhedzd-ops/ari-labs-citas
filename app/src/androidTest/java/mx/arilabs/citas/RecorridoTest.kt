package mx.arilabs.citas

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Recorre la app como lo haría una persona y saca una captura de cada pantalla en
 * /sdcard/capturas (el workflow las baja para revisarlas).
 */
@RunWith(AndroidJUnit4::class)
class RecorridoTest {

    @get:Rule(order = 0)
    val permiso: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val regla = createAndroidComposeRule<MainActivity>()

    private var n = 0

    private fun shell(cmd: String) {
        val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(cmd)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private fun foto(nombre: String) {
        regla.waitForIdle()
        Thread.sleep(900)
        n++
        shell("mkdir -p /sdcard/capturas")
        shell("screencap -p /sdcard/capturas/%02d-%s.png".format(n, nombre))
    }

    /** Espera a que el texto esté en pantalla (las animaciones pueden tardar) y lo toca. */
    private fun toca(texto: String) {
        regla.waitUntil(8_000) { regla.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(500)
        regla.waitForIdle()
        regla.onAllNodesWithText(texto)[0].performClick()
        regla.waitForIdle()
    }

    @Test
    fun recorrido() {
        foto("bienvenida")
        regla.onAllNodes(hasSetTextAction())[0].performTextInput("Don Javier")
        toca("Empezar")
        foto("agenda")

        regla.waitUntil(8_000) { regla.onAllNodesWithTag("nueva-cita", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        regla.onNodeWithTag("nueva-cita", useUnmergedTree = true).performClick()
        regla.waitForIdle()
        foto("nueva-cita")
        regla.onAllNodes(hasSetTextAction())[0].performTextInput("sábado 3pm novena colonia Chuburná doña Rosa 9991234567 \$600 anticipo 200")
        foto("nueva-cita-escrita")
        toca("Ordenar")
        foto("revisar")
        toca("Guardar")
        foto("detalle")

        toca("Ya se hizo")
        foto("detalle-hecha")
        toca("Atrás")
        foto("agenda-despues")

        toca("Clientes")
        toca("doña Rosa")
        foto("cliente")
        toca("Atrás")
        toca("Calendario"); foto("calendario")
        toca("Clientes"); foto("clientes")
        toca("Cobros"); foto("cobros")

        toca("Música"); foto("musica")
        toca("Agenda"); foto("final")
    }
}
