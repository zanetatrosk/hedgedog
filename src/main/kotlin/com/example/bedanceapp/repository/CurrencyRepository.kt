package com.example.bedanceapp.repository

import com.example.bedanceapp.model.Currency
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CurrencyRepository : JpaRepository<Currency, Long> {
    fun findByCode(code: String): Optional<Currency>
}

