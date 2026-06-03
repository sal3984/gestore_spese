package com.expense.management.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
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

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                DashboardWidget.entries.forEachIndexed { index, widget ->
                    ListItem(
                        headlineContent = {
                            Text(
                                when (widget) {
                                    DashboardWidget.SUMMARY_CARDS -> stringResource(R.string.widget_summary_cards)
                                    DashboardWidget.CREDIT_CARD_INFO -> stringResource(R.string.widget_credit_card_info)
                                    DashboardWidget.BNPL_PROJECTIONS -> stringResource(R.string.widget_bnpl_projections)
                                    DashboardWidget.TRANSACTION_LIST -> stringResource(R.string.widget_transaction_list)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        leadingContent = {
                            Icon(
                                when (widget) {
                                    DashboardWidget.SUMMARY_CARDS -> Icons.Default.Summarize
                                    DashboardWidget.CREDIT_CARD_INFO -> Icons.Default.CreditCard
                                    DashboardWidget.BNPL_PROJECTIONS -> Icons.AutoMirrored.Filled.TrendingUp
                                    DashboardWidget.TRANSACTION_LIST -> Icons.AutoMirrored.Filled.ListAlt
                                },
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
                    if (index < DashboardWidget.entries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        settingsSectionHeader(stringResource(R.string.customize_csv_export))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                EXPORT_COLUMN_MAP.entries.forEachIndexed { index, (key, displayName) ->
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
                    if (index < EXPORT_COLUMN_MAP.entries.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                    }
                }
            }
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
