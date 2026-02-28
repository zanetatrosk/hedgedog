package com.example.bedanceapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class BeDanceAppApplication

fun main(args: Array<String>) {
    runApplication<BeDanceAppApplication>(*args)
}
