package com.example.bedanceapp.service

import com.example.bedanceapp.model.Currency
import com.example.bedanceapp.repository.CurrencyRepository
import org.springframework.stereotype.Service

@Service
class CurrenciesService(private val currenciesRepository: CurrencyRepository) {
    fun findAll(): List<Currency> = currenciesRepository.findAll()
}
