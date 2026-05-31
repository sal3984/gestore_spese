package com.expense.management.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MaterialYouLightScheme =
    lightColorScheme(
        primary = MyPrimaryLight,
        onPrimary = MyOnPrimaryLight,
        primaryContainer = MyPrimaryContainerLight,
        onPrimaryContainer = MyOnPrimaryContainerLight,
        secondary = MySecondaryLight,
        onSecondary = MyOnSecondaryLight,
        secondaryContainer = MySecondaryContainerLight,
        onSecondaryContainer = MyOnSecondaryContainerLight,
        tertiary = MyTertiaryLight,
        onTertiary = MyOnTertiaryLight,
        background = MyBackgroundLight,
        onBackground = MyOnBackgroundLight,
        surface = MySurfaceLight,
        onSurface = MyOnSurfaceLight,
        surfaceVariant = MySurfaceVariantLight,
        onSurfaceVariant = MyOnSurfaceVariantLight,
        outline = MyOutlineLight,
        error = MyErrorLight,
        onError = MyOnErrorLight,
    )

private val MaterialYouDarkScheme =
    darkColorScheme(
        primary = MyPrimaryDark,
        onPrimary = MyOnPrimaryDark,
        primaryContainer = MyPrimaryContainerDark,
        onPrimaryContainer = MyOnPrimaryContainerDark,
        secondary = MySecondaryDark,
        onSecondary = MyOnSecondaryDark,
        secondaryContainer = MySecondaryContainerDark,
        onSecondaryContainer = MyOnSecondaryContainerDark,
        tertiary = MyTertiaryDark,
        onTertiary = MyOnTertiaryDark,
        background = MyBackgroundDark,
        onBackground = MyOnBackgroundDark,
        surface = MySurfaceDark,
        onSurface = MyOnSurfaceDark,
        surfaceVariant = MySurfaceVariantDark,
        onSurfaceVariant = MyOnSurfaceVariantDark,
        outline = MyOutlineDark,
        error = MyErrorDark,
        onError = MyOnErrorDark,
    )

private val NordicLightScheme =
    lightColorScheme(
        primary = NordicPrimaryLight,
        onPrimary = NordicOnPrimaryLight,
        primaryContainer = NordicPrimaryContainerLight,
        onPrimaryContainer = NordicOnPrimaryContainerLight,
        secondary = NordicSecondaryLight,
        onSecondary = NordicOnSecondaryLight,
        secondaryContainer = NordicSecondaryContainerLight,
        onSecondaryContainer = NordicOnSecondaryContainerLight,
        tertiary = NordicTertiaryLight,
        onTertiary = NordicOnTertiaryLight,
        background = NordicBackgroundLight,
        onBackground = NordicOnBackgroundLight,
        surface = NordicSurfaceLight,
        onSurface = NordicOnSurfaceLight,
        surfaceVariant = NordicSurfaceVariantLight,
        onSurfaceVariant = NordicOnSurfaceVariantLight,
        outline = NordicOutlineLight,
        error = NordicErrorLight,
        onError = NordicOnErrorLight,
    )

private val NordicDarkScheme =
    darkColorScheme(
        primary = NordicPrimaryDark,
        onPrimary = NordicOnPrimaryDark,
        primaryContainer = NordicPrimaryContainerDark,
        onPrimaryContainer = NordicOnPrimaryContainerDark,
        secondary = NordicSecondaryDark,
        onSecondary = NordicOnSecondaryDark,
        secondaryContainer = NordicSecondaryContainerDark,
        onSecondaryContainer = NordicOnSecondaryContainerDark,
        tertiary = NordicTertiaryDark,
        onTertiary = NordicOnTertiaryDark,
        background = NordicBackgroundDark,
        onBackground = NordicOnBackgroundDark,
        surface = NordicSurfaceDark,
        onSurface = NordicOnSurfaceDark,
        surfaceVariant = NordicSurfaceVariantDark,
        onSurfaceVariant = NordicOnSurfaceVariantDark,
        outline = NordicOutlineDark,
        error = NordicErrorDark,
        onError = NordicOnErrorDark,
    )

private val CyberpunkLightScheme =
    lightColorScheme(
        primary = CyberPrimaryLight,
        onPrimary = CyberOnPrimaryLight,
        primaryContainer = CyberPrimaryContainerLight,
        onPrimaryContainer = CyberOnPrimaryContainerLight,
        secondary = CyberSecondaryLight,
        onSecondary = CyberOnSecondaryLight,
        secondaryContainer = CyberSecondaryContainerLight,
        onSecondaryContainer = CyberOnSecondaryContainerLight,
        tertiary = CyberTertiaryLight,
        onTertiary = CyberOnTertiaryLight,
        background = CyberBackgroundLight,
        onBackground = CyberOnBackgroundLight,
        surface = CyberSurfaceLight,
        onSurface = CyberOnSurfaceLight,
        surfaceVariant = CyberSurfaceVariantLight,
        onSurfaceVariant = CyberOnSurfaceVariantLight,
        outline = CyberOutlineLight,
        error = CyberErrorLight,
        onError = CyberOnErrorLight,
    )

private val CyberpunkDarkScheme =
    darkColorScheme(
        primary = CyberPrimaryDark,
        onPrimary = CyberOnPrimaryDark,
        primaryContainer = CyberPrimaryContainerDark,
        onPrimaryContainer = CyberOnPrimaryContainerDark,
        secondary = CyberSecondaryDark,
        onSecondary = CyberOnSecondaryDark,
        secondaryContainer = CyberSecondaryContainerDark,
        onSecondaryContainer = CyberOnSecondaryContainerDark,
        tertiary = CyberTertiaryDark,
        onTertiary = CyberOnTertiaryDark,
        background = CyberBackgroundDark,
        onBackground = CyberOnBackgroundDark,
        surface = CyberSurfaceDark,
        onSurface = CyberOnSurfaceDark,
        surfaceVariant = CyberSurfaceVariantDark,
        onSurfaceVariant = CyberOnSurfaceVariantDark,
        outline = CyberOutlineDark,
        error = CyberErrorDark,
        onError = CyberOnErrorDark,
    )

private val CorporateLightScheme =
    lightColorScheme(
        primary = CorpPrimaryLight,
        onPrimary = CorpOnPrimaryLight,
        primaryContainer = CorpPrimaryContainerLight,
        onPrimaryContainer = CorpOnPrimaryContainerLight,
        secondary = CorpSecondaryLight,
        onSecondary = CorpOnSecondaryLight,
        secondaryContainer = CorpSecondaryContainerLight,
        onSecondaryContainer = CorpOnSecondaryContainerLight,
        tertiary = CorpTertiaryLight,
        onTertiary = CorpOnTertiaryLight,
        background = CorpBackgroundLight,
        onBackground = CorpOnBackgroundLight,
        surface = CorpSurfaceLight,
        onSurface = CorpOnSurfaceLight,
        surfaceVariant = CorpSurfaceVariantLight,
        onSurfaceVariant = CorpOnSurfaceVariantLight,
        outline = CorpOutlineLight,
        error = CorpErrorLight,
        onError = CorpOnErrorLight,
    )

private val CorporateDarkScheme =
    darkColorScheme(
        primary = CorpPrimaryDark,
        onPrimary = CorpOnPrimaryDark,
        primaryContainer = CorpPrimaryContainerDark,
        onPrimaryContainer = CorpOnPrimaryContainerDark,
        secondary = CorpSecondaryDark,
        onSecondary = CorpOnSecondaryDark,
        secondaryContainer = CorpSecondaryContainerDark,
        onSecondaryContainer = CorpOnSecondaryContainerDark,
        tertiary = CorpTertiaryDark,
        onTertiary = CorpOnTertiaryDark,
        background = CorpBackgroundDark,
        onBackground = CorpOnBackgroundDark,
        surface = CorpSurfaceDark,
        onSurface = CorpOnSurfaceDark,
        surfaceVariant = CorpSurfaceVariantDark,
        onSurfaceVariant = CorpOnSurfaceVariantDark,
        outline = CorpOutlineDark,
        error = CorpErrorDark,
        onError = CorpOnErrorDark,
    )

@Composable
fun AppTheme(
    appStyle: AppStyle = AppStyle.MATERIAL_YOU,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme =
        when (appStyle) {
            AppStyle.MATERIAL_YOU -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                } else {
                    if (darkTheme) MaterialYouDarkScheme else MaterialYouLightScheme
                }
            }
            AppStyle.NORDIC -> if (darkTheme) NordicDarkScheme else NordicLightScheme
            AppStyle.CYBERPUNK -> if (darkTheme) CyberpunkDarkScheme else CyberpunkLightScheme
            AppStyle.CORPORATE -> if (darkTheme) CorporateDarkScheme else CorporateLightScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

@Deprecated("Use AppTheme instead", ReplaceWith("AppTheme(AppStyle.MATERIAL_YOU, darkTheme, content = content)"))
@Composable
fun gestoreSpeseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) = AppTheme(appStyle = AppStyle.MATERIAL_YOU, darkTheme = darkTheme, content = content)
