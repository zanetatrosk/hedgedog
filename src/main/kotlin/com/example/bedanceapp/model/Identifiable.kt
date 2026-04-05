package com.example.bedanceapp.model

import java.util.UUID

interface Identifiable {
    val id: UUID?
    val name: String
}

fun Identifiable.toCodebook(): CodebookItem = CodebookItem(
    id = id?.toString().orEmpty(),
    name = name
)

fun <T : Identifiable> Iterable<T>.toCodebookList(): List<CodebookItem> = map { it.toCodebook() }
