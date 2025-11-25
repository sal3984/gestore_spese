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
data class Category(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val defaultType: String
)

val CATEGORIES = listOf(
    Category("food", "Cibo", Icons.Default.Fastfood, "EXPENSE"),
    Category("home", "Casa", Icons.Default.Home, "EXPENSE"),
    Category("transport", "Trasporti", Icons.Default.DirectionsCar, "EXPENSE"),
    Category("fun", "Svago", Icons.Default.SportsEsports, "EXPENSE"),
    Category("salary", "Stipendio", Icons.Default.Work, "INCOME"),
    Category("invest", "Investimenti", Icons.Default.TrendingUp, "INCOME"),
    Category("other_exp", "Altro (Uscita)", Icons.Default.RemoveCircle, "EXPENSE"),
    Category("other_inc", "Altro (Entrata)", Icons.Default.AddCircle, "INCOME"),
)


