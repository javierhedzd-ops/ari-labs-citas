package mx.arilabs.citas.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import mx.arilabs.citas.datos.Tipo

/** "Agenda de papel": fondo crema, tinta azul, letra grande. Pensado para leer sin lentes. */
object Papel {
    val fondo = Color(0xFFF7F2E8)
    val hoja = Color(0xFFFFFDF8)
    val tinta = Color(0xFF26231F)
    val tenue = Color(0xFF6A6259)
    val linea = Color(0xFFE3DACB)
    val azul = Color(0xFF1F3F66)
    val azulSuave = Color(0xFFE4EBF3)
    val aviso = Color(0xFF8A5A00)
    val avisoSuave = Color(0xFFFBF0D9)
    val cancelada = Color(0xFF9A928A)

    fun colorTipo(t: Tipo): Color = when (t) {
        Tipo.REZO -> Color(0xFF1F3F66)
        Tipo.NOVENA -> Color(0xFF3F5E2E)
        Tipo.VELORIO -> Color(0xFF3B3B3B)
        Tipo.MISA -> Color(0xFF6E2A2A)
        Tipo.OTRO -> Color(0xFF6A6259)
    }
}

private val colores = lightColorScheme(
    primary = Papel.azul,
    onPrimary = Color.White,
    primaryContainer = Papel.azulSuave,
    onPrimaryContainer = Papel.azul,
    background = Papel.fondo,
    onBackground = Papel.tinta,
    surface = Papel.hoja,
    onSurface = Papel.tinta,
    surfaceVariant = Papel.fondo,
    onSurfaceVariant = Papel.tenue,
    outline = Papel.linea,
    error = Color(0xFF9B2C2C),
)

private val letras = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, lineHeight = 34.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, lineHeight = 26.sp),
    bodyLarge = TextStyle(fontSize = 19.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun TemaCitas(contenido: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colores, typography = letras, content = contenido)
}
