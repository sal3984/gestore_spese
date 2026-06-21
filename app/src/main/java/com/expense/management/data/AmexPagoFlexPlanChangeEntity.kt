package com.expense.management.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "amex_pagoflex_plan_changes",
    foreignKeys = [
        ForeignKey(
            entity = AmexPagoFlexPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["planId"]),
    ],
)
data class AmexPagoFlexPlanChangeEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val changedAt: String,
    val previousInstallmentCount: Int,
    val newInstallmentCount: Int,
    val previousInstallmentAmount: Double,
    val newInstallmentAmount: Double,
    val remainingDebtAtChange: Double,
    val reason: String? = null,
)
