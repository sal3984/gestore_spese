package com.expense.management.wear.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(
                onSendTransaction = { amount, description ->
                    sendTransactionToPhone(amount, description)
                }
            )
        }
    }

    private fun sendTransactionToPhone(amount: String, description: String) {
        val payload = "$amount|$description"
        val data = payload.toByteArray(StandardCharsets.UTF_8)
        val messagePath = "/add_transaction"

        val capabilityClient = Wearable.getCapabilityClient(this)
        val messageClient = Wearable.getMessageClient(this)

        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        scope.launch {
            try {
                val nodes = capabilityClient
                    .getCapability("expense_management_transcription", CapabilityClient.FILTER_REACHABLE)
                    .await()
                    .nodes

                // Se non troviamo nodi tramite capability, proviamo a prendere tutti i nodi connessi
                val finalNodes = if (nodes.isEmpty()) {
                     Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                } else {
                    nodes
                }

                if (finalNodes.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Nessun telefono connesso", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                // Invia a tutti i nodi connessi (di solito è uno solo, il telefono)
                for (node in finalNodes) {
                    messageClient.sendMessage(node.id, messagePath, data).await()
                }

                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Inviato!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Errore invio", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun WearApp(onSendTransaction: (String, String) -> Unit) {
    MaterialTheme {
        var amount by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("Spesa da Watch") }

        val listState = rememberScalingLazyListState()

        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(
                    text = "Aggiungi Spesa",
                    style = MaterialTheme.typography.title3,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                Text(
                    text = if (amount.isEmpty()) "0" else amount,
                    style = MaterialTheme.typography.display3,
                    color = MaterialTheme.colors.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            item {
                // Tastierino numerico
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf(".", "0", "DEL")
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (row in rows) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (key in row) {
                                CompactButton(
                                    onClick = {
                                        if (key == "DEL") {
                                            if (amount.isNotEmpty()) {
                                                amount = amount.dropLast(1)
                                            }
                                        } else {
                                            if (key == "." && amount.contains(".")) return@CompactButton
                                            amount += key
                                        }
                                    },
                                    modifier = Modifier.width(40.dp).height(32.dp)
                                ) {
                                    Text(text = key)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (amount.isNotEmpty()) {
                            onSendTransaction(amount, description)
                            amount = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Invia")
                }
            }
        }
    }
}
