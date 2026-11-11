package com.example.habittracker.domain.tags

import java.util.Locale

/**
 * Правила тегов, общие для формы и репозитория.
 *
 * Имена сравниваем без учёта регистра: «Здоровье» и «здоровье» — один тег.
 * Длину режем по code points, чтобы не разорвать эмодзи.
 */
object TagRules {
    const val MAX_TAGS = 8
    const val MAX_NAME_LENGTH = 24

    enum class Error { EMPTY, TOO_LONG, LIMIT, DUPLICATE }

    /**
     * Приводит список к тому, что реально попадёт в БД: trim, без пустых,
     * без повторов (case-insensitive), не длиннее лимита, не больше MAX_TAGS.
     */
    fun normalize(raw: List<String>): List<String> =
        raw.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.limitCodePoints(MAX_NAME_LENGTH) }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .take(MAX_TAGS)
            .toList()

    /** Проверка нового тега перед добавлением в выбранные. */
    fun validateNew(raw: String, selected: List<String>): Error? {
        val name = raw.trim()
        if (name.isEmpty()) return Error.EMPTY
        if (name.codePointCount(0, name.length) > MAX_NAME_LENGTH) return Error.TOO_LONG
        if (selected.size >= MAX_TAGS) return Error.LIMIT
        if (selected.any { it.equals(name, ignoreCase = true) }) return Error.DUPLICATE
        return null
    }

    private fun String.limitCodePoints(max: Int): String =
        if (codePointCount(0, length) <= max) this
        else substring(0, offsetByCodePoints(0, max))
}
