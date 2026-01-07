package com.example.bedanceapp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class UrlConfig {

    @Value("\${app.base-url}")
    lateinit var baseUrl: String

}
