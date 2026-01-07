package com.example.moneymap.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryNavy80,
    onPrimary = OnPrimaryNavy80,
    primaryContainer = PrimaryNavyContainer80,
    onPrimaryContainer = OnPrimaryNavyContainer80,
    secondary = Gold80,
    onSecondary = OnGold80,
    secondaryContainer = GoldContainer80,
    onSecondaryContainer = OnGoldContainer80,
    tertiary = Slate80,
    onTertiary = OnSlate80,
    tertiaryContainer = SlateContainer80,
    onTertiaryContainer = OnSlateContainer80,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inversePrimary = PrimaryNavy40
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryNavy40,
    onPrimary = OnPrimaryNavy40,
    primaryContainer = PrimaryNavyContainer40,
    onPrimaryContainer = OnPrimaryNavyContainer40,
    secondary = Gold40,
    onSecondary = OnGold40,
    secondaryContainer = GoldContainer40,
    onSecondaryContainer = OnGoldContainer40,
    tertiary = Slate40,
    onTertiary = OnSlate40,
    tertiaryContainer = SlateContainer40,
    onTertiaryContainer = OnSlateContainer40,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inversePrimary = PrimaryNavy80
)

@Composable
fun MoneyMapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            
            // Sync window background with theme background to prevent lag during transition
            window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))
            
            // Force transparent system bars to work with Edge-to-Edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
