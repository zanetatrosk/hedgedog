package com.example.bedanceapp.service

import com.example.bedanceapp.model.Currency
import com.example.bedanceapp.repository.CurrencyRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@DisplayName("CurrenciesService Tests")
class CurrenciesServiceTest {

    @Mock private lateinit var currenciesRepository: CurrencyRepository

    private lateinit var service: CurrenciesService

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        service = CurrenciesService(currenciesRepository)
    }

    @Test
    fun `findAll returns all currencies`() {
        val list = listOf(Currency(code = "EUR", name = "Euro", symbol = "EUR"))
        whenever(currenciesRepository.findAll()).thenReturn(list)

        assertEquals(list, service.findAll())
    }
}

