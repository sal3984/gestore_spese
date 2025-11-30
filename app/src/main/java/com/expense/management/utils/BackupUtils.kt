package com.expense.management.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.expense.management.data.TransactionEntity
import com.expense.management.viewmodel.BackupData
import com.expense.management.viewmodel.ExpenseViewModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object BackupUtils {

    fun performCsvExport(context: Context, viewModel: ExpenseViewModel, uri: Uri, currencySymbol: String, dateFormat: String) {
        val formatter = DateTimeFormatter.ofPattern(dateFormat)
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            try {
                val expenses = viewModel.getExpensesForExport()
                val csvHeader = "ID,Data,Descrizione,Importo (${currencySymbol} - Convertito),Importo Originale,Valuta Originale,Categoria,Tipo,Carta di Credito,Data Addebito\n"
                val csvContent = StringBuilder(csvHeader)
                expenses.forEach { t ->
                    val dateStr = try {
                        LocalDate.parse(t.date, DateTimeFormatter.ISO_LOCAL_DATE).format(formatter)
                    } catch (_: Exception) {
                        t.date
                    }
                    val effectiveDateStr = try {
                        LocalDate.parse(t.effectiveDate, DateTimeFormatter.ISO_LOCAL_DATE).format(formatter)
                    } catch (_: Exception) {
                        t.effectiveDate
                    }

                    csvContent.append(
                        "${t.id}," +
                            "\"$dateStr\"," +
                            "\"${t.description.replace('\"', '\'')}\"," +
                            "${String.format(Locale.US, "%.2f", t.amount)}," +
                            "${String.format(Locale.US, "%.2f", t.originalAmount)}," +
                            "\"${t.originalCurrency}\"," +
                            "${t.categoryId}," +
                            "${t.type}," +
                            "${if (t.isCreditCard) "Sì" else "No"}," +
                            "\"$effectiveDateStr\"\n"
                    )
                }

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvContent.toString())
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Esportate ${expenses.size} spese in CSV!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore durante l'esportazione: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun performBackup(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            try {
                val allData = viewModel.getAllForBackup()
                val json = Gson().toJson(allData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(json)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Backup salvato con successo!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore durante il backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun performRestore(context: Context, viewModel: ExpenseViewModel, uri: Uri) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val sb = StringBuilder()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.forEachLine { sb.append(it) }
                    }
                }
                val jsonString = sb.toString()

                try {
                     val backupData = Gson().fromJson(jsonString, BackupData::class.java)
                    viewModel.restoreData(backupData)
                    withContext(Dispatchers.Main) {
                       Toast.makeText(context, "Ripristino completato: ${backupData.transactions.size} movimenti e ${backupData.categories.size} categorie.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                } catch (_: JsonSyntaxException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val type = object : TypeToken<List<TransactionEntity>>() {}.type
                val list: List<TransactionEntity> = Gson().fromJson(jsonString, type)
                viewModel.restoreLegacyData(list)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Ripristinati ${list.size} movimenti (formato vecchio)! I dati appariranno a breve.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Errore Ripristino: File non valido o corrotto", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
