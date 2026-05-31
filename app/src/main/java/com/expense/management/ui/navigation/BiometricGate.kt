package com.expense.management.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.expense.management.R
import com.expense.management.utils.BiometricUtils

@Composable
fun BiometricGate(
    isBiometricEnabled: Boolean,
    isAuthenticated: Boolean,
    onUnlock: () -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(isBiometricEnabled) {
        if (isBiometricEnabled && !isAuthenticated) {
            BiometricUtils.authenticateUser(
                context = context,
                onSuccess = onUnlock,
                onError = { },
            )
        } else {
            onUnlock()
        }
    }

    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.app_blocked),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.authenticate_to_access),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(
                onClick = {
                    BiometricUtils.authenticateUser(
                        context = context,
                        onSuccess = onUnlock,
                        onError = { },
                    )
                },
                modifier = Modifier.padding(top = 24.dp),
            ) {
                Text(stringResource(R.string.unlock))
            }
        }
    } else {
        content()
    }
}
