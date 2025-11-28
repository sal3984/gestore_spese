package com.expense.management.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expense.management.R

@Composable
fun DataManagementScreen(
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onExportCsv: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // Esporta CSV
                Text(stringResource(R.string.export_data_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.export_csv_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onExportCsv,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Download, null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(stringResource(R.string.export_csv))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // Backup & Ripristino
                Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(R.string.restore), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onBackup,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Green
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.backup))
                    }

                    Button(
                        onClick = onRestore,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)), // Orange
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.restore))
                    }
                }
            }
        }
    }
}
