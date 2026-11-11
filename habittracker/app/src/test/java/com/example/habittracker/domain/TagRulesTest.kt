package com.example.habittracker.domain

import com.example.habittracker.domain.tags.TagRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagRulesTest {

    @Test
    fun normalize_trimsDropsBlanksAndDedupesIgnoringCase() {
        assertEquals(
            listOf("Здоровье", "утро"),
            TagRules.normalize(listOf(" Здоровье ", "", "здоровье", "утро")),
        )
    }

    @Test
    fun normalize_keepsAtMostMaxTags() {
        val raw = (1..(TagRules.MAX_TAGS + 3)).map { "тег-$it" }
        assertEquals(TagRules.MAX_TAGS, TagRules.normalize(raw).size)
    }

    @Test
    fun normalize_limitsNameLengthWithoutSplittingEmoji() {
        val emoji = "🙂"
        val result = TagRules.normalize(listOf(emoji.repeat(30))).single()
        assertEquals(emoji.repeat(TagRules.MAX_NAME_LENGTH), result)
    }

    @Test
    fun validateNew_rejectsEmptyTooLongDuplicateAndOverLimit() {
        assertEquals(TagRules.Error.EMPTY, TagRules.validateNew("   ", emptyList()))
        assertEquals(
            TagRules.Error.TOO_LONG,
            TagRules.validateNew("x".repeat(TagRules.MAX_NAME_LENGTH + 1), emptyList()),
        )
        assertEquals(
            TagRules.Error.DUPLICATE,
            TagRules.validateNew("Работа", listOf("работа")),
        )
        assertEquals(
            TagRules.Error.LIMIT,
            TagRules.validateNew("новый", (1..TagRules.MAX_TAGS).map { "тег-$it" }),
        )
    }

    @Test
    fun validateNew_acceptsFreshName() {
        assertNull(TagRules.validateNew("  спорт  ", listOf("работа")))
    }
}
