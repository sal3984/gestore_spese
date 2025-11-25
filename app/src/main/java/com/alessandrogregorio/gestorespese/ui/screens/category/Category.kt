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
    Category("cibo", "Cibo & Spesa", "🍕"),
    Category("casa", "Casa & Bollette", "🏠"),
    Category("trasporti", "Trasporti", "🚗"),
    Category("salute", "Salute", "⚕️"),
    Category("divertimento", "Divertimento", "🎉"),
    Category("regalo", "Regalo", "\uD83C\uDF81"), // PACCHETTO REGALO (🎁)
    Category("stipendio", "Stipendio", "💰"),
    Category("altro", "Altro", "✨")
)
