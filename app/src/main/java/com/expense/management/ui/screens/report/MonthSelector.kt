package com.expense.management.ui.screens.report

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.intl.Locale as ComposeLocale

private fun String.capitalizeFirstLetter(locale: java.util.Locale = java.util.Locale.getDefault()): String {
    return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(
    modifier: Modifier = Modifier,
    selectedMonth: YearMonth,
    onMonthSelected: (YearMonth) -> Unit,
    label: String,
) {
    val locale = ComposeLocale.current.platformLocale
    var expanded by remember { mutableStateOf(false) }
    val months = remember {
        (-24..0).map { YearMonth.now().plusMonths(it.toLong()) }.sortedByDescending { it }
    }
    val shortFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val fullFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedMonth.format(shortFormatter).capitalizeFirstLetter(locale),
            onValueChange = { },
            readOnly = true,
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            months.forEach { month ->
                DropdownMenuItem(
                    text = {
                        Text(
                            month.format(fullFormatter).capitalizeFirstLetter(locale),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    },
                    onClick = {
                        onMonthSelected(month)
                        expanded = false
                    },
                )
            }
        }
    }
}
