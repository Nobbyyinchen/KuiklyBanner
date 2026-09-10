package com.hzbank.kuikly.banner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoopingBannerModelTest {

    @Test
    fun emptyItemsProduceNoPhysicalItems() {
        assertTrue(buildLoopItemPairs(emptyList<String>()).isEmpty())
        assertEquals(-1, normalizeBannerIndex(0, 0))
        assertEquals(-1, toLogicalBannerIndex(0, 0))
    }

    @Test
    fun singleItemDoesNotCreateShadowItems() {
        assertEquals(
            listOf(BannerItemPair("A", "A")),
            buildLoopItemPairs(listOf("A")),
        )
        assertEquals(0, toLogicalBannerIndex(10, 1))
        assertEquals(0, toPhysicalBannerIndex(10, 1))
    }

    @Test
    fun multipleItemsCreateHeadAndTwoTailShadowItems() {
        val pairs = buildLoopItemPairs(listOf("A", "B", "C"))

        assertEquals(listOf("C", "A", "B", "C", "A", "B"), pairs.map { it.current })
        assertEquals(listOf("A", "B", "C", "A", "B", "C"), pairs.map { it.next })
    }

    @Test
    fun physicalIndexesMapBackToLogicalIndexes() {
        val mapped = (0..5).map { toLogicalBannerIndex(it, 3) }
        assertEquals(listOf(2, 0, 1, 2, 0, 1), mapped)
    }

    @Test
    fun logicalIndexesAreNormalizedBeforePhysicalMapping() {
        assertEquals(3, toPhysicalBannerIndex(-1, 3))
        assertEquals(1, toPhysicalBannerIndex(3, 3))
        assertEquals(2, toPhysicalBannerIndex(4, 3))
    }
}

