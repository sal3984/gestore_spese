package com.expense.management.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.ui.screens.settingsSectionHeader
import com.expense.management.ui.theme.AppStyle

@Composable
fun ThemeSettingsScreen(
    currentThemeMode: String,
    currentAppStyle: AppStyle,
    onThemeModeChange: (String) -> Unit,
    onAppStyleChange: (AppStyle) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        settingsSectionHeader(stringResource(R.string.theme))

        val themeModes = listOf("system", "light", "dark")
        val themeLabels = mapOf(
            "system" to stringResource(R.string.theme_system),
            "light" to stringResource(R.string.theme_light),
            "dark" to stringResource(R.string.theme_dark),
        )
        themeModes.forEach { mode ->
            ListItem(
                headlineContent = {
                    Text(
                        themeLabels[mode] ?: mode,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.BrightnessMedium,
                        contentDescription = themeLabels[mode] ?: mode,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    RadioButton(selected = mode == currentThemeMode, onClick = null)
                },
                modifier = Modifier
                    .clickable { onThemeModeChange(mode) }
                    .heightIn(min = 48.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        settingsSectionHeader(stringResource(R.string.app_style))

        AppStyle.entries.forEach { style ->
            ListItem(
                headlineContent = {
                    Text(
                        when (style) {
                            AppStyle.MATERIAL_YOU -> stringResource(R.string.style_material_you)
                            AppStyle.NORDIC -> stringResource(R.string.style_nordic)
                            AppStyle.CYBERPUNK -> stringResource(R.string.style_cyberpunk)
                            AppStyle.CORPORATE -> stringResource(R.string.style_corporate)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = when (style) {
                            AppStyle.MATERIAL_YOU -> stringResource(R.string.style_material_you)
                            AppStyle.NORDIC -> stringResource(R.string.style_nordic)
                            AppStyle.CYBERPUNK -> stringResource(R.string.style_cyberpunk)
                            AppStyle.CORPORATE -> stringResource(R.string.style_corporate)
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    RadioButton(selected = style == currentAppStyle, onClick = null)
                },
                modifier = Modifier
                    .clickable { onAppStyleChange(style) }
                    .heightIn(min = 48.dp),
            )
        }
    }
}
