package com.fungast.app.map

import com.fungast.app.data.Spot
import com.fungast.app.data.SpotCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class MapModelTest {
    private fun spot(vararg categories: SpotCategory) = Spot(
        name = "Bosco",
        categories = categories.toSet(),
        latitude = 45.0,
        longitude = 9.0,
        altitude = null,
        accuracyMeters = null,
        notes = "",
        createdAt = 0,
    )

    @Test
    fun markerImageIdIgnoresTheOrderCategoriesWereChosenIn() {
        assertEquals(
            spot(SpotCategory.MUSHROOMS, SpotCategory.CHESTNUTS).markerImageId,
            spot(SpotCategory.CHESTNUTS, SpotCategory.MUSHROOMS).markerImageId,
        )
        assertEquals("spot-MUSHROOMS+CHESTNUTS", spot(SpotCategory.CHESTNUTS, SpotCategory.MUSHROOMS).markerImageId)
    }

    @Test
    fun markerImageIdFallsBackToOtherWithoutCategories() {
        assertEquals("spot-OTHER", spot().markerImageId)
    }
}
