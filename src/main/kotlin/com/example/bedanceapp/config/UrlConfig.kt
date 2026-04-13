package com.example.bedanceapp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/** Holds application URL properties used across the app. */
@Component
class UrlConfig {

    @Value("\${app.base-url}")
    lateinit var baseUrl: String

}
