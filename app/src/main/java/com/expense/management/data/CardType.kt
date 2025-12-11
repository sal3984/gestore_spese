package com.expense.management.data

import com.google.gson.annotations.SerializedName

enum class CardType {
    @SerializedName("saldo")
    SALDO,          // A saldo: plafond ripristinato interamente al pagamento

    @SerializedName("revolving")
    REVOLVING       // Revolving/Rateale: plafond ripristinato man mano che si pagano le rate
}
