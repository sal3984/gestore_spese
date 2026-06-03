package com.expense.management.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.expense.management.R
import com.expense.management.ui.screens.settingsSectionHeader

@Composable
fun AboutSettingsScreen() {
    val context = LocalContext.current
    val appVersion = remember {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        settingsSectionHeader(stringResource(R.string.about))

        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.privacy_policy),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    stringResource(R.string.privacy_policy_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.Description,
                    contentDescription = stringResource(R.string.privacy_policy),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            },
            modifier = Modifier.clickable {
                val url = "https://gist.github.com/sal3984/adc05b7037705f169aa6682b877ef581"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
        )

        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.github_repo),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    stringResource(R.string.github_repo_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.Code,
                    contentDescription = stringResource(R.string.github_repo),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            },
            modifier = Modifier.clickable {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, "https://github.com/sal3984/gestore_spese".toUri()),
                )
            },
        )

        HorizontalDivider(modifier = Modifier.padding(start = 56.dp))

        ListItem(
            headlineContent = {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            },
            supportingContent = {
                Text(
                    stringResource(R.string.app_version, appVersion),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
