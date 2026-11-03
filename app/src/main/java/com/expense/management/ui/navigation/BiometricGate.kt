package com.expense.management.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
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

    // Anche al ri-blocco dopo il rientro dal background, non solo all'avvio
    LaunchedEffect(isBiometricEnabled, isAuthenticated) {
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

    // Il contenuto resta composto sotto la schermata di blocco: un modulo a meta' o un selettore di
    // file aperto non si perdono al ri-blocco. Coperto e nascosto all'accessibilita' finche' bloccato
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = if (isAuthenticated) Modifier else Modifier.clearAndSetSemantics { }) {
            content()
        }
        if (!isAuthenticated) {
            // Surface non cliccabile: copre tutto e non lascia passare i tocchi al contenuto sotto
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
            }
        }
    }
}
