package com.expense.management.domain.usecase

import com.expense.management.domain.model.CardExposure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("CalculateCardExposureUseCase")
class CalculateCardExposureUseCaseTest {

    private val useCase = CalculateCardExposureUseCase()

    @Test
    fun `available is the limit minus what is still to be charged`() {
        val result = useCase.execute(3000.0, 750.5)

        assertEquals(750.5, result.exposure)
        assertEquals(2249.5, result.available)
        assertEquals(0.2502f, result.utilization, 0.0001f)
    }

    @Test
    fun `credits beyond purchases do not raise the limit`() {
        assertEquals(CardExposure(exposure = 0.0, available = 3000.0, utilization = 0f), useCase.execute(3000.0, -40.0))
    }

    @Test
    fun `over the limit nothing is available and utilization goes past one`() {
        val result = useCase.execute(1000.0, 1200.0)

        assertEquals(0.0, result.available)
        assertEquals(1.2f, result.utilization, 0.0001f)
    }

    @Test
    fun `a card without a limit has zero utilization`() {
        assertEquals(0f, useCase.execute(0.0, 100.0).utilization)
    }

    @Test
    fun `amounts are rounded to the cent`() {
        val result = useCase.execute(100.0, 0.1 + 0.2)

        assertEquals(0.3, result.exposure)
        assertEquals(99.7, result.available)
    }
}
