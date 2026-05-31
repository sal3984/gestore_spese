package com.expense.management.ui.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.expense.management.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppDrawer(
    drawerState: DrawerState,
    navController: NavHostController,
    coroutineScope: CoroutineScope,
    currentRoute: String?,
    onExit: () -> Unit,
) {
    ModalDrawerSheet {
        androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        DrawerItem(
            label = stringResource(R.string.dashboard),
            selected = currentRoute == "dashboard",
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            onClick = {
                navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                coroutineScope.launch { drawerState.close() }
            },
        )
        DrawerItem(
            label = stringResource(R.string.data_management),
            selected = currentRoute == "data_management",
            icon = { Icon(Icons.Default.Backup, contentDescription = null) },
            onClick = {
                navController.navigate("data_management")
                coroutineScope.launch { drawerState.close() }
            },
        )
        DrawerItem(
            label = stringResource(R.string.security_usability),
            selected = currentRoute == "security",
            icon = { Icon(Icons.Default.Security, contentDescription = null) },
            onClick = {
                navController.navigate("security")
                coroutineScope.launch { drawerState.close() }
            },
        )
        DrawerItem(
            label = stringResource(R.string.payment_methods),
            selected = currentRoute == "payment_methods",
            icon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
            onClick = {
                navController.navigate("payment_methods")
                coroutineScope.launch { drawerState.close() }
            },
        )
        DrawerItem(
            label = stringResource(R.string.settings),
            selected = currentRoute == "settings",
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            onClick = {
                navController.navigate("settings")
                coroutineScope.launch { drawerState.close() }
            },
        )

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        )

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.exit)) },
            selected = false,
            onClick = onExit,
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = stringResource(R.string.exit)) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerItem(
    label: String,
    selected: Boolean,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = icon,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
