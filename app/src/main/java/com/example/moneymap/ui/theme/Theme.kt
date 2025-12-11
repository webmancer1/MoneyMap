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
    primary = PrimaryGreen80,
    onPrimary = OnPrimaryGreen80,
    primaryContainer = PrimaryGreenContainer80,
    onPrimaryContainer = OnPrimaryGreenContainer80,
    secondary = Sage80,
    onSecondary = OnSage80,
    secondaryContainer = SageContainer80,
    onSecondaryContainer = OnSageContainer80,
    tertiary = Ocean80,
    onTertiary = OnOcean80,
    tertiaryContainer = OceanContainer80,
    onTertiaryContainer = OnOceanContainer80,
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
    inversePrimary = PrimaryGreen40
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen40,
    onPrimary = OnPrimaryGreen40,
    primaryContainer = PrimaryGreenContainer40,
    onPrimaryContainer = OnPrimaryGreenContainer40,
    secondary = Sage40,
    onSecondary = OnSage40,
    secondaryContainer = SageContainer40,
    onSecondaryContainer = OnSageContainer40,
    tertiary = Ocean40,
    onTertiary = OnOcean40,
    tertiaryContainer = OceanContainer40,
    onTertiaryContainer = OnOceanContainer40,
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
    inversePrimary = PrimaryGreen80
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
