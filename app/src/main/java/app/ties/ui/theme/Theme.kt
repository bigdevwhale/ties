package app.ties.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.ties.R
import app.ties.data.Circle
import app.ties.domain.Status

/** Warm, paper-like palette: the app is about people, not productivity. */
object Ties {
    val Bg = Color(0xFFF7F2EB)
    val Surface = Color(0xFFFFFFFF)
    val Surface2 = Color(0xFFFBF8F3)
    val Line = Color(0xFFEAE2D6)
    val Ink = Color(0xFF231F1C)
    val Muted = Color(0xFF726A62)
    val Faint = Color(0xFFA79F96)
    val Coral = Color(0xFFD9623F)
    val CoralSoft = Color(0xFFF8E3DA)
    val Amber = Color(0xFFD08F22)
    val AmberSoft = Color(0xFFF9EDD5)
    val Moss = Color(0xFF5B8A68)
    val MossSoft = Color(0xFFE3EEE5)
    val OnCoral = Color(0xFFFFFFFF)

    val RLg = RoundedCornerShape(24.dp)
    val RMd = RoundedCornerShape(18.dp)
    val RSm = RoundedCornerShape(12.dp)

    fun circle(c: Circle): Color = when (c) {
        Circle.FAMILY -> Color(0xFFD9623F)
        Circle.FRIENDS -> Color(0xFF3F7CA3)
        Circle.CHILDHOOD -> Color(0xFF8A68B8)
        Circle.WORK -> Color(0xFF5B8A68)
        Circle.OTHER -> Color(0xFF4F7480)
    }

    fun status(s: Status): Color = when (s) {
        Status.OVERDUE, Status.DUE -> Coral
        Status.SOON -> Amber
        Status.OK -> Moss
    }

    fun statusSoft(s: Status): Color = when (s) {
        Status.OVERDUE, Status.DUE -> CoralSoft
        Status.SOON -> AmberSoft
        Status.OK -> MossSoft
    }
}

val Inter = FontFamily(
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

object TiesType {
    val Title = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 28.sp, letterSpacing = (-0.025).em, lineHeight = 32.sp, color = Ties.Ink)
    val Sub = TextStyle(fontFamily = Inter, fontSize = 14.5.sp, lineHeight = 21.sp, color = Ties.Muted)
    val Eyebrow = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = Ties.Faint)
    val Strong = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp, letterSpacing = (-0.01).em, color = Ties.Ink)
    val Body = TextStyle(fontFamily = Inter, fontSize = 15.sp, lineHeight = 22.sp, color = Ties.Ink)
    val Muted = TextStyle(fontFamily = Inter, fontSize = 13.sp, lineHeight = 18.sp, color = Ties.Muted)
    val Small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Ties.Faint)
    val Button = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
fun TiesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Ties.Coral,
            onPrimary = Ties.OnCoral,
            primaryContainer = Ties.CoralSoft,
            onPrimaryContainer = Ties.Ink,
            secondaryContainer = Ties.CoralSoft,
            onSecondaryContainer = Ties.Ink,
            background = Ties.Bg,
            surface = Ties.Surface,
            surfaceContainerHigh = Ties.Surface,
            surfaceContainerLow = Ties.Surface2,
            onBackground = Ties.Ink,
            onSurface = Ties.Ink,
            onSurfaceVariant = Ties.Muted,
            outline = Ties.Line,
            outlineVariant = Ties.Line,
        ),
        typography = MaterialTheme.typography.copy(
            bodyLarge = TiesType.Body,
            bodyMedium = TiesType.Body.copy(fontSize = 14.sp),
            labelLarge = TiesType.Button,
        ),
        content = content,
    )
}
