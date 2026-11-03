package com.expense.management.domain.usecase

import com.expense.management.data.CategoryEntity
import com.expense.management.data.ExpenseRepository
import com.expense.management.data.TransactionType
import com.expense.management.domain.model.CATEGORIES
import com.expense.management.domain.model.DefaultCategoryNames
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Categorie predefinite al primo avvio")
class InitializeCategoriesUseCaseTest {

    private val names = DefaultCategoryNames { id -> "name:$id" }

    @Test
    fun `missing defaults are created with the names it receives`() = runBlocking {
        val repository = mockk<ExpenseRepository>(relaxed = true)
        coEvery { repository.getAllCategories() } returns emptyList()
        val inserted = slot<List<CategoryEntity>>()

        InitializeCategoriesUseCase(repository, names)()

        coVerify { repository.insertAllCategories(capture(inserted)) }
        assertEquals(CATEGORIES.map { "name:${it.id}" }, inserted.captured.map { it.label })
    }

    @Test
    fun `categories already saved keep their name`() = runBlocking {
        val repository = mockk<ExpenseRepository>(relaxed = true)
        coEvery { repository.getAllCategories() } returns listOf(CategoryEntity("food", "Cibo", "x", TransactionType.EXPENSE))
        val inserted = slot<List<CategoryEntity>>()

        InitializeCategoriesUseCase(repository, names)()

        coVerify { repository.insertAllCategories(capture(inserted)) }
        assertEquals(false, inserted.captured.any { it.id == "food" })
        coVerify(exactly = 0) { repository.updateCategory(any()) }
    }

    @Test
    fun `without names the historical italian labels are used`() = runBlocking {
        val repository = mockk<ExpenseRepository>(relaxed = true)
        coEvery { repository.getAllCategories() } returns emptyList()
        val inserted = slot<List<CategoryEntity>>()

        InitializeCategoriesUseCase(repository)()

        coVerify { repository.insertAllCategories(capture(inserted)) }
        assertEquals("Cibo", inserted.captured.first { it.id == "food" }.label)
    }
}
