package com.fungast.app.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun categoriesRoundTripInFixedOrder() {
        val text = converters.categoriesToText(setOf(SpotCategory.OTHER, SpotCategory.MUSHROOMS))

        assertEquals("MUSHROOMS,OTHER", text)
        assertEquals(setOf(SpotCategory.MUSHROOMS, SpotCategory.OTHER), converters.textToCategories(text))
    }

    @Test
    fun singleCategoryFromVersionOneIsRead() {
        assertEquals(setOf(SpotCategory.CHESTNUTS), converters.textToCategories("CHESTNUTS"))
    }

    @Test
    fun unknownCategoriesBecomeOther() {
        assertEquals(setOf(SpotCategory.OTHER), converters.textToCategories("TRUFFLES"))
    }

    @Test
    fun dateRoundTrip() {
        val date = LocalDate(2026, 10, 20)

        assertEquals("2026-10-20", converters.dateToText(date))
        assertEquals(date, converters.textToDate("2026-10-20"))
        assertEquals(null, converters.textToDate(null))
    }
}
