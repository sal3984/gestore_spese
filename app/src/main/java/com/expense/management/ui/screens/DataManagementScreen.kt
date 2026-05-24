package com.expense.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.ui.theme.gestoreSpeseTheme

@Composable
fun DataManagementScreen(
    modifier: Modifier = Modifier,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Esporta CSV
                Text(
                    stringResource(R.string.export_data_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                ) {
                    Icon(Icons.Default.Download, stringResource(R.string.export_csv))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.export_csv), style = MaterialTheme.typography.titleMedium)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )

                // Backup & Ripristino
                Text(
                    stringResource(R.string.backup_restore),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))

                BoxWithConstraints {
                    if (maxWidth > 340.dp) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Button(
                                onClick = onBackup,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(56.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            ) {
                                Icon(Icons.Default.CloudUpload, stringResource(R.string.backup))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }

                            Button(
                                onClick = onRestore,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(56.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            ) {
                                Icon(Icons.Default.CloudDownload, stringResource(R.string.restore))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.restore), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = onBackup,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            ) {
                                Icon(Icons.Default.CloudUpload, stringResource(R.string.backup))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.backup), style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = onRestore,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                            ) {
                                Icon(Icons.Default.CloudDownload, stringResource(R.string.restore))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.restore), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "DataManagement Light")
@Composable
private fun DataManagementPreview() {
    gestoreSpeseTheme(darkTheme = false, dynamicColor = false) {
        DataManagementScreen(onBackup = {}, onRestore = {}, onExportCsv = {})
    }
}

@Preview(showBackground = true, name = "DataManagement Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DataManagementPreviewDark() {
    gestoreSpeseTheme(darkTheme = true, dynamicColor = false) {
        DataManagementScreen(onBackup = {}, onRestore = {}, onExportCsv = {})
    }
}
