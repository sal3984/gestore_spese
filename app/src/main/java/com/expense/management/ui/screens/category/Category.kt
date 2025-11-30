package com.expense.management.ui.screens.category

// Definiamo le Categorie qui per utilità grafica
// Modello dati per la categoria con icona
data class Category(
    val id: String,
    val label: String,
    val icon: String,
    val type: String,
) // AGGIUNTO: type

// Nota: Le label qui sono usate solo per il mapping interno/default se non si usa il contesto di composizione.
// Per la UI, utilizzeremo le stringhe localizzate mappate dinamicamente nelle schermate.
// L'ideale sarebbe ristrutturare CATEGORIES per usare resource IDs invece di stringhe dirette,
// ma per mantenere compatibilità con il resto del codice, useremo un mapper nella UI.

val CATEGORIES =
    listOf(
        // USCITE (expense)
        Category("food", "Cibo", "\uD83C\uDF7D", "expense"),
        Category("transport", "Trasporti", "\uD83D\uDE97", "expense"),
        Category("housing", "Casa", "\uD83C\uDFE0", "expense"),
        Category("entertainment", "Svago", "\uD83C\uDFC1", "expense"),
        Category("bills", "Bollette", "\uD83D\uDCCB", "expense"),
        Category("health", "Salute", "\u2764\uFE0F", "expense"),
        Category("shopping", "Shopping", "\uD83D\uDED2", "expense"),
        Category("other", "Altro", "\u2753", "expense"),
        // ENTRATE (income)
        Category("salary", "Stipendio", "\uD83D\uDCB0", "income"),
        Category("bonifico", "Bonifico", "\uD83D\uDCB3", "income"),
        Category("gift", "Regalo", "\uD83C\uDF81", "income"),
        Category("refund", "Rimborso", "\u21A9\uFE0F", "income"),
        Category("investment", "Investimenti", "\uD83D\uDCCA", "income"),
    )
