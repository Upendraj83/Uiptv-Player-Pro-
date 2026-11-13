package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemePalette

data class TiweeExtendedColors(
  val backgroundEnd: Color,
  val cardElevated: Color,
  val cardBorder: Color,
  val liveRed: Color,
  val favoriteRose: Color,
  val syncedEmerald: Color,
  val warningAmber: Color,
  val isAmoled: Boolean,
)

val LocalTiweeColors = staticCompositionLocalOf {
  TiweeExtendedColors(
    backgroundEnd = DarkBackgroundEnd,
    cardElevated = DarkSurfaceElevated,
    cardBorder = DarkOutline,
    liveRed = TiweeLiveRed,
    favoriteRose = TiweeFavoriteRose,
    syncedEmerald = TiweeEmerald,
    warningAmber = TiweeAmber,
    isAmoled = false,
  )
}

private val TiweeDarkColorScheme =
  darkColorScheme(
    primary = TiweePurple,
    onPrimary = Color.White,
    primaryContainer = TiweePurpleDeep,
    onPrimaryContainer = Color.White,
    secondary = TiweePurpleBright,
    onSecondary = Color(0xFF1A0F2E),
    tertiary = TiweeFavoriteRose,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkOutline,
    error = TiweeLiveRed,
    onError = Color.White,
  )

private val TiweeAmoledColorScheme =
  darkColorScheme(
    primary = TiweePurple,
    onPrimary = Color.White,
    primaryContainer = TiweePurpleDeep,
    onPrimaryContainer = Color.White,
    secondary = TiweePurpleBright,
    onSecondary = Color(0xFF1A0F2E),
    tertiary = TiweeFavoriteRose,
    onTertiary = Color.White,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = AmoledOutline,
    error = TiweeLiveRed,
    onError = Color.White,
  )

val TiweeShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

@Composable
fun TiweeTheme(
  palette: ThemePalette = ThemePalette.DARK,
  content: @Composable () -> Unit,
) {
  val isAmoled = palette == ThemePalette.AMOLED
  val colorScheme = if (isAmoled) TiweeAmoledColorScheme else TiweeDarkColorScheme
  val extendedColors =
    if (isAmoled) {
      TiweeExtendedColors(
        backgroundEnd = Color(0xFF0A0A0A),
        cardElevated = AmoledSurfaceElevated,
        cardBorder = AmoledOutline,
        liveRed = TiweeLiveRed,
        favoriteRose = TiweeFavoriteRose,
        syncedEmerald = TiweeEmerald,
        warningAmber = TiweeAmber,
        isAmoled = true,
      )
    } else {
      TiweeExtendedColors(
        backgroundEnd = DarkBackgroundEnd,
        cardElevated = DarkSurfaceElevated,
        cardBorder = DarkOutline,
        liveRed = TiweeLiveRed,
        favoriteRose = TiweeFavoriteRose,
        syncedEmerald = TiweeEmerald,
        warningAmber = TiweeAmber,
        isAmoled = false,
      )
    }

  CompositionLocalProvider(LocalTiweeColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = TiweeTypography,
      shapes = TiweeShapes,
      content = content,
    )
  }
}
