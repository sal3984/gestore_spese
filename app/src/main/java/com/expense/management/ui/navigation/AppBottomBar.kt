package com.expense.management.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.expense.management.R

@Composable
fun AppBottomBar(
    navController: NavHostController,
    currentRoute: String?,
) {
    NavigationBar(tonalElevation = 8.dp) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = stringResource(R.string.dashboard)) },
            label = { Text(stringResource(R.string.dashboard)) },
            selected = currentRoute == "dashboard",
            onClick = {
                navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Download, contentDescription = stringResource(R.string.report)) },
            label = { Text(stringResource(R.string.report)) },
            selected = currentRoute == "report",
            onClick = {
                navController.navigate("report") {
                    popUpTo("dashboard") { saveState = true }
                    restoreState = true
                    launchSingleTop = true
                }
            },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Category, contentDescription = stringResource(R.string.categories_title)) },
            label = { Text(stringResource(R.string.categories_title)) },
            selected = currentRoute == "categories",
            onClick = {
                navController.navigate("categories") {
                    popUpTo("dashboard") { saveState = true }
                    restoreState = true
                    launchSingleTop = true
                }
            },
        )
    }
}
