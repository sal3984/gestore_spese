package com.expense.management.utils

fun parseAmountInput(input: String): Double? = input.replace(',', '.').toDoubleOrNull()
