package mx.arilabs.citas.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.arilabs.citas.datos.Tipo

/**
 * Diseño "app de sistema": fondo gris cálido, tarjetas blancas con sombra suave, azul
 * marino de acento y un color por tipo de cita. Letra grande para leer sin lentes.
 */
object Papel {
    val fondo = Color(0xFFF4F3EF)
    val hoja = Color(0xFFFFFFFF)
    val tinta = Color(0xFF1B1B1F)
    val tenue = Color(0xFF6B6B73)
    val linea = Color(0xFFE6E3DD)
    val azul = Color(0xFF1F3A5F)
    val azulVivo = Color(0xFF2F5C93)
    val azulSuave = Color(0xFFE3EAF3)
    val verde = Color(0xFF2E7D4F)
    val verdeSuave = Color(0xFFE2F1E7)
    val aviso = Color(0xFF8A5A00)
    val avisoSuave = Color(0xFFFBF0D9)
    val rojo = Color(0xFFB3261E)
    val cancelada = Color(0xFF9A968F)

    fun colorTipo(t: Tipo): Color = when (t) {
        Tipo.REZO -> Color(0xFF2F5C93)
        Tipo.NOVENA -> Color(0xFF3F7A3A)
        Tipo.VELORIO -> Color(0xFF4A4A52)
        Tipo.MISA -> Color(0xFF8A3A3A)
        Tipo.OTRO -> Color(0xFF7A6A55)
    }

    fun suaveTipo(t: Tipo): Color = colorTipo(t).copy(alpha = 0.12f)
}

private val colores = lightColorScheme(
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

private val letras = Typography(
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

private val formas = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TemaCitas(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = letras, shapes = formas, content = contenido)
}
