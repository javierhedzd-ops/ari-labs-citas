package mx.arilabs.citas.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.arilabs.citas.datos.Tipo

/**
 * Diseño "app de sistema": claro (gris cálido, tarjetas blancas, azul marino) u oscuro
 * (casi negro, tarjetas grafito, azul hielo). Letra grande para leer sin lentes.
 * Los colores cambian solos con el modo: TemaCitas fija [oscuro].
 */
object Papel {
    var oscuro by mutableStateOf(false)
        internal set

    private fun c(claro: Long, osc: Long) = Color(if (oscuro) osc else claro)

    val fondo get() = c(0xFFF4F3EF, 0xFF0D0F12)
    val hoja get() = c(0xFFFFFFFF, 0xFF171A1F)
    val tinta get() = c(0xFF1B1B1F, 0xFFEEF1F4)
    val tenue get() = c(0xFF6B6B73, 0xFF959CA6)
    val linea get() = c(0xFFE6E3DD, 0xFF2A2F37)
    /** Acento: textos, iconos y botones principales. */
    val azul get() = c(0xFF1F3A5F, 0xFF9ED8EE)
    /** Lo que va encima de [azul] (texto del botón principal). */
    val sobreAzul get() = c(0xFFFFFFFF, 0xFF0B1A24)
    /** Azul profundo de fondo con texto blanco encima (próxima cita, día escogido). */
    val relleno get() = c(0xFF1F3A5F, 0xFF22466D)
    val azulVivo get() = c(0xFF2F5C93, 0xFF7FC4E4)
    val azulSuave get() = c(0xFFE3EAF3, 0xFF1A2733)
    val verde get() = c(0xFF2E7D4F, 0xFF6FCF97)
    val verdeSuave get() = c(0xFFE2F1E7, 0xFF15261C)
    val aviso get() = c(0xFF8A5A00, 0xFFE8B65A)
    val avisoSuave get() = c(0xFFFBF0D9, 0xFF2A2112)
    val rojo get() = c(0xFFB3261E, 0xFFF2867D)
    val cancelada get() = c(0xFF9A968F, 0xFF6C7179)
    /** Fondo de los avisos de abajo ("Deshacer"). */
    val snack get() = c(0xFF1B1B1F, 0xFF2B3038)

    /** Color fuerte del tipo, para fondos con texto blanco encima. */
    fun tipoFondo(t: Tipo): Color = when (t) {
        Tipo.REZO -> Color(0xFF2F5C93)
        Tipo.NOVENA -> Color(0xFF3F7A3A)
        Tipo.VELORIO -> Color(0xFF4A4A52)
        Tipo.MISA -> Color(0xFF8A3A3A)
        Tipo.BODA -> Color(0xFF9A6B1F)
        Tipo.XV -> Color(0xFFA03A78)
        Tipo.FIESTA -> Color(0xFF1E7A7A)
        Tipo.OTRO -> Color(0xFF7A6A55)
    }

    /** Color del tipo para textos y etiquetas: en oscuro se aclara para que se lea. */
    fun colorTipo(t: Tipo): Color = legible(tipoFondo(t))

    /** Aclara un color oscuro cuando la app está en modo oscuro. */
    fun legible(color: Color): Color = if (oscuro) androidx.compose.ui.graphics.lerp(color, Color.White, 0.42f) else color

    fun suaveTipo(t: Tipo): Color = colorTipo(t).copy(alpha = 0.12f)
}

private fun colores(oscuro: Boolean) = if (oscuro) darkColorScheme(
    primary = Papel.azul,
    onPrimary = Papel.sobreAzul,
    primaryContainer = Papel.azulSuave,
    onPrimaryContainer = Papel.azul,
    secondaryContainer = Papel.azulSuave,
    onSecondaryContainer = Papel.azul,
    background = Papel.fondo,
    onBackground = Papel.tinta,
    surface = Papel.fondo,
    onSurface = Papel.tinta,
    surfaceContainer = Papel.hoja,
    surfaceContainerLow = Papel.hoja,
    surfaceContainerHigh = Papel.hoja,
    surfaceContainerHighest = Color(0xFF20242A),
    surfaceVariant = Color(0xFF20242A),
    onSurfaceVariant = Papel.tenue,
    outline = Papel.linea,
    outlineVariant = Papel.linea,
    error = Papel.rojo,
) else lightColorScheme(
    primary = Papel.azul,
    onPrimary = Color.White,
    primaryContainer = Papel.azulSuave,
    onPrimaryContainer = Papel.azul,
    secondaryContainer = Papel.azulSuave,
    onSecondaryContainer = Papel.azul,
    background = Papel.fondo,
    onBackground = Papel.tinta,
    surface = Papel.fondo,
    onSurface = Papel.tinta,
    surfaceContainer = Papel.hoja,
    surfaceContainerLow = Papel.hoja,
    surfaceContainerHigh = Papel.hoja,
    surfaceVariant = Color(0xFFECEAE4),
    onSurfaceVariant = Papel.tenue,
    outline = Papel.linea,
    outlineVariant = Papel.linea,
    error = Papel.rojo,
)

/** Plus Jakarta Sans (OFL), variable: un solo archivo para todos los pesos. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun peso(w: Int) = androidx.compose.ui.text.font.Font(
    mx.arilabs.citas.R.font.plus_jakarta,
    weight = FontWeight(w),
    variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(w)),
)

val Jakarta = androidx.compose.ui.text.font.FontFamily(peso(400), peso(500), peso(600), peso(700), peso(800))

private val letrasBase = Typography(
    displaySmall = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold, lineHeight = 25.sp),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 21.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
)

private val letras = letrasBase.run {
    fun TextStyle.j() = copy(fontFamily = Jakarta)
    copy(
        displaySmall = displaySmall.j().copy(fontWeight = FontWeight.ExtraBold), headlineMedium = headlineMedium.j(),
        titleLarge = titleLarge.j(), titleMedium = titleMedium.j(), titleSmall = titleSmall.j(),
        bodyLarge = bodyLarge.j(), bodyMedium = bodyMedium.j(), bodySmall = bodySmall.j(),
        labelLarge = labelLarge.j(), labelMedium = labelMedium.j(), labelSmall = labelSmall.j(),
    )
}

private val formas = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

/** Apariencia que escoge el usuario en Ajustes. */
enum class Apariencia(val nombre: String) { AUTO("Automático"), CLARO("Claro"), OSCURO("Oscuro") }

@Composable
fun TemaCitas(apariencia: Apariencia = Apariencia.AUTO, contenido: @Composable () -> Unit) {
    val oscuro = when (apariencia) {
        Apariencia.AUTO -> androidx.compose.foundation.isSystemInDarkTheme()
        Apariencia.CLARO -> false
        Apariencia.OSCURO -> true
    }
    // Se fija antes de dibujar los hijos, que leen los colores de Papel.
    if (Papel.oscuro != oscuro) Papel.oscuro = oscuro
    MaterialTheme(colorScheme = colores(oscuro), typography = letras, shapes = formas, content = contenido)
}

/** Estilo de los calendarios y relojes del sistema (DatePicker, TimePicker), según el modo. */
fun estiloDialogo(): Int = if (Papel.oscuro) mx.arilabs.citas.R.style.Dialogo_Citas_Oscuro else mx.arilabs.citas.R.style.Dialogo_Citas
