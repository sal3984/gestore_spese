package com.alessandrogregorio.gestorespese.ui.screens.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

// Definiamo le Categorie qui per utilità grafica
// Modello dati per la categoria con icona
data class Category(val id: String, val label: String, val icon: String, val type: String) // AGGIUNTO: type

val CATEGORIES = listOf(
    // USCITE (expense)
    Category("food", "Cibo \uD83C\uDF7D️", "\uD83C\uDF7D", "expense"),
    Category("transport", "Trasporti \uD83D\uDE97", "\uD83D\uDE97", "expense"),
    Category("housing", "Casa \uD83C\uDFE0", "\uD83C\uDFE0", "expense"),
    Category("entertainment", "Svago \uD83C\uDFC1", "\uD83C\uDFC1", "expense"),
    Category("bills", "Bollette \uD83D\uDCCB", "\uD83D\uDCCB", "expense"),
    Category("health", "Salute \u2764\uFE0F", "\u2764\uFE0F", "expense"),
    Category("shopping", "Shopping \uD83D\uDED2", "\uD83D\uDED2", "expense"),
    Category("other", "Altro \u2753", "\u2753", "expense"),

    // ENTRATE (income)
    Category("salary", "Stipendio \uD83D\uDCB0", "\uD83D\uDCB0", "income"),
    Category("bonifico", "Bonifico \uD83D\uDCB3", "\uD83D\uDCB3", "income"), // NUOVA
    Category("gift", "Regalo \uD83C\uDF81", "\uD83C\uDF81", "income"), // NUOVA
    Category("refund", "Rimborso \u21A9\uFE0F", "\u21A9\uFE0F", "income"), // NUOVA
    Category("investment", "Investimenti \uD83D\uDCCA", "\uD83D\uDCCA", "income"),
)
