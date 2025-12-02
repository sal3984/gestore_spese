package com.expense.management.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import com.expense.management.R
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
                // Note: Header columns could also be localized if needed, but CSV headers are often technical/fixed.
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
                    Toast.makeText(context, context.getString(R.string.export_success_csv, expenses.size), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.export_error, e.localizedMessage), Toast.LENGTH_LONG).show()
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
                    Toast.makeText(context, context.getString(R.string.backup_success), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.backup_error, e.localizedMessage), Toast.LENGTH_LONG).show()
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

                    // Normalizzazione date (da dd/MM/yyyy a ISO yyyy-MM-dd se necessario)
                    val normalizedTransactions = backupData.transactions.map { t ->
                        t.copy(
                            date = normalizeDate(t.date),
                            effectiveDate = normalizeDate(t.effectiveDate)
                        )
                    }
                    val normalizedBackupData = backupData.copy(transactions = normalizedTransactions)

                    viewModel.restoreData(normalizedBackupData)
                    withContext(Dispatchers.Main) {
                       Toast.makeText(context, context.getString(R.string.restore_success, normalizedBackupData.transactions.size, normalizedBackupData.categories.size), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                } catch (_: JsonSyntaxException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val type = object : TypeToken<List<TransactionEntity>>() {}.type
                val list: List<TransactionEntity> = Gson().fromJson(jsonString, type)

                // Normalizzazione date anche per backup legacy
                val normalizedList = list.map { t ->
                    t.copy(
                        date = normalizeDate(t.date),
                        effectiveDate = normalizeDate(t.effectiveDate)
                    )
                }

                viewModel.restoreLegacyData(normalizedList)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.restore_legacy_success, normalizedList.size), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.restore_error_file), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun normalizeDate(dateStr: String): String {
        return try {
            // Prova a parsare come ISO 8601 (yyyy-MM-dd)
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            // Se ha successo, è già nel formato corretto
            dateStr
        } catch (e: Exception) {
            try {
                // Prova a parsare con il vecchio formato dd/MM/yyyy
                val oldFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                LocalDate.parse(dateStr, oldFormatter).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e2: Exception) {
                // Se entrambi falliscono, restituisce la stringa originale
                dateStr
            }
        }
    }
}
