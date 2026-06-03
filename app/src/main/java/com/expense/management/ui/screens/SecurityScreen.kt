package com.expense.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.ui.theme.gestoreSpeseTheme

@Composable
fun securityScreen(
    modifier: Modifier = Modifier,
    isBiometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        settingsSectionHeader(stringResource(R.string.security_usability))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column {
                settingsSwitchItem(
                    icon = Icons.Default.Security,
                    title = stringResource(R.string.app_lock),
                    subtitle = stringResource(R.string.app_lock_desc),
                    checked = isBiometricEnabled,
                    onCheckedChange = onBiometricEnabledChange,
                )
            }
        }
    }
}

@Composable
fun settingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .padding(8.dp),
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        modifier =
        Modifier
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
    )
}

@Preview(showBackground = true, name = "Security Light")
@Composable
private fun SecurityPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        securityScreen(isBiometricEnabled = false, onBiometricEnabledChange = {})
    }
}

@Preview(showBackground = true, name = "Security Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SecurityPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        securityScreen(isBiometricEnabled = false, onBiometricEnabledChange = {})
    }
}
