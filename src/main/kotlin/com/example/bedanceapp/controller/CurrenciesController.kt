package com.example.bedanceapp.controller

import com.example.bedanceapp.model.Currency
import com.example.bedanceapp.service.CurrenciesService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/currencies")
@CrossOrigin(origins = ["http://localhost:3000", "http://10.0.0.67:3000/"])
class CurrenciesController(private val currenciesService: CurrenciesService) {

    @GetMapping
    fun getAllCurrencies(): List<Currency> {
        return currenciesService.findAll()
    }
}
