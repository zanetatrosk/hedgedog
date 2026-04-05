package com.example.bedanceapp.service.registration

import tools.jackson.databind.ObjectMapper

fun <T> String?.toObject(objectMapper: ObjectMapper, clazz: Class<T>): T? {
    return if (this.isNullOrBlank()) {
        null
    } else {
        try {
            objectMapper.readValue(this, clazz)
        } catch (_: Exception) {
            null
        }
    }
}

