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
    primary = PrimaryTeal80,
    onPrimary = OnPrimaryTeal80,
    primaryContainer = PrimaryTealContainer80,
    onPrimaryContainer = OnPrimaryTealContainer80,
    secondary = SecondaryTerracotta80,
    onSecondary = OnSecondaryTerracotta80,
    secondaryContainer = SecondaryTerracottaContainer80,
    onSecondaryContainer = OnSecondaryTerracottaContainer80,
    tertiary = TertiarySand80,
    onTertiary = OnTertiarySand80,
    tertiaryContainer = TertiarySandContainer80,
    onTertiaryContainer = OnTertiarySandContainer80,
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
    inversePrimary = PrimaryTeal40
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal40,
    onPrimary = OnPrimaryTeal40,
    primaryContainer = PrimaryTealContainer40,
    onPrimaryContainer = OnPrimaryTealContainer40,
    secondary = SecondaryTerracotta40,
    onSecondary = OnSecondaryTerracotta40,
    secondaryContainer = SecondaryTerracottaContainer40,
    onSecondaryContainer = OnSecondaryTerracottaContainer40,
    tertiary = TertiarySand40,
    onTertiary = OnTertiarySand40,
    tertiaryContainer = TertiarySandContainer40,
    onTertiaryContainer = OnTertiarySandContainer40,
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
    inversePrimary = PrimaryTeal80
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
