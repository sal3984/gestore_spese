package com.expense.management.ui.navigation

import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.expense.management.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(currentRoute: String?) {
    CenterAlignedTopAppBar(
        title = {
            val title =
                when (currentRoute) {
                    "dashboard" -> stringResource(R.string.dashboard)
                    "report" -> stringResource(R.string.report)
                    "categories" -> stringResource(R.string.categories_title)
                    "settings" -> stringResource(R.string.settings)
                    "data_management" -> stringResource(R.string.data_management)
                    "security" -> stringResource(R.string.security_usability)
                    "payment_methods" -> stringResource(R.string.payment_methods)
                    else -> stringResource(R.string.app_name)
                }
            Text(text = title, fontWeight = FontWeight.SemiBold)
        },
        colors =
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}
