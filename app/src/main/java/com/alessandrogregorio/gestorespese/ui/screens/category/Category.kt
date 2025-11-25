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
data class Category(val id: String, val label: String, val icon: String)

val CATEGORIES = listOf(
    Category("food", "Cibo \uD83C\uDF7D️", "\uD83C\uDF7D"),
    Category("transport", "Trasporti \uD83D\uDE97", "\uD83D\uDE97"),
    Category("housing", "Casa \uD83C\uDFE0", "\uD83C\uDFE0"),
    Category("entertainment", "Svago \uD83C\uDFC1", "\uD83C\uDFC1"),
    Category("salary", "Stipendio \uD83D\uDCB0", "\uD83D\uDCB0"),
    Category("income", "Entrata \uD83D\uDCB8", "\uD83D\uDCB8"),
    Category("other", "Altro \uD83C\uDFC6", "\uD83C\uDFC6")
)
