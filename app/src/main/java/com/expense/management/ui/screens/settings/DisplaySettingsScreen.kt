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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.domain.model.DashboardWidget
import com.expense.management.ui.screens.EXPORT_COLUMN_MAP
import com.expense.management.ui.screens.settingsSectionHeader
import com.expense.management.ui.theme.gestoreSpeseTheme

@Composable
fun DisplaySettingsScreen(
    enabledWidgets: Set<DashboardWidget>,
    csvExportColumns: Set<String>,
    onEnabledWidgetsChange: (Set<DashboardWidget>) -> Unit,
    onCsvExportColumnsChange: (Set<String>) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        settingsSectionHeader(stringResource(R.string.dashboard_widgets))

        DashboardWidget.entries.forEach { widget ->
            ListItem(
                headlineContent = {
                    Text(
                        when (widget) {
                            DashboardWidget.SUMMARY_CARDS -> "Summary Cards"
                            DashboardWidget.CREDIT_CARD_INFO -> "Credit Card Info"
                            DashboardWidget.BNPL_PROJECTIONS -> "BNPL Projections"
                            DashboardWidget.TRANSACTION_LIST -> "Transaction List"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = widget.name,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    Checkbox(
                        checked = enabledWidgets.contains(widget),
                        onCheckedChange = { isChecked ->
                            onEnabledWidgetsChange(
                                if (isChecked) enabledWidgets + widget else enabledWidgets - widget,
                            )
                        },
                    )
                },
                modifier = Modifier
                    .clickable {
                        onEnabledWidgetsChange(
                            if (enabledWidgets.contains(widget)) {
                                enabledWidgets - widget
                            } else {
                                enabledWidgets + widget
                            },
                        )
                    }
                    .heightIn(min = 48.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        settingsSectionHeader(stringResource(R.string.customize_csv_export))

        EXPORT_COLUMN_MAP.entries.forEach { (key, displayName) ->
            ListItem(
                headlineContent = {
                    Text(displayName, style = MaterialTheme.typography.bodyLarge)
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = displayName,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                },
                trailingContent = {
                    Checkbox(
                        checked = csvExportColumns.contains(key),
                        onCheckedChange = { isChecked ->
                            onCsvExportColumnsChange(
                                if (isChecked) csvExportColumns + key else csvExportColumns - key,
                            )
                        },
                    )
                },
                modifier = Modifier
                    .clickable {
                        onCsvExportColumnsChange(
                            if (csvExportColumns.contains(key)) {
                                csvExportColumns - key
                            } else {
                                csvExportColumns + key
                            },
                        )
                    }
                    .heightIn(min = 48.dp),
            )
        }
    }
}

@Preview(showBackground = true, name = "Display Settings Light")
@Composable
private fun DisplaySettingsPreviewLight() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        DisplaySettingsScreen(
            enabledWidgets = DashboardWidget.entries.toSet(),
            csvExportColumns = EXPORT_COLUMN_MAP.keys,
            onEnabledWidgetsChange = {},
            onCsvExportColumnsChange = {},
        )
    }
}

@Preview(showBackground = true, name = "Display Settings Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DisplaySettingsPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        DisplaySettingsScreen(
            enabledWidgets = DashboardWidget.entries.toSet(),
            csvExportColumns = EXPORT_COLUMN_MAP.keys,
            onEnabledWidgetsChange = {},
            onCsvExportColumnsChange = {},
        )
    }
}
