/*
 * MIT License
 * Copyright (c) 2026 KuiklyBanner contributors
 */
package com.hzbank.kuikly.banner

internal const val LOOP_HEAD_ITEM_COUNT = 1
internal const val LOOP_TAIL_ITEM_COUNT = 2

internal data class BannerItemPair<T>(
    val current: T,
    val next: T,
)

/**
 * Builds the physical PageList sequence used by both banner implementations.
 *
 * For more than one item the sequence is:
 * [last, all real items, first, second].
 */
internal fun <T> buildLoopItemPairs(items: List<T>): List<BannerItemPair<T>> {
    if (items.isEmpty()) return emptyList()
    if (items.size == 1) return listOf(BannerItemPair(items[0], items[0]))

    val pairs = ArrayList<BannerItemPair<T>>(items.size + LOOP_HEAD_ITEM_COUNT + LOOP_TAIL_ITEM_COUNT)
    pairs += BannerItemPair(items.last(), items.first())
    items.indices.forEach { index ->
        pairs += BannerItemPair(items[index], items[(index + 1) % items.size])
    }
    repeat(LOOP_TAIL_ITEM_COUNT) { index ->
        pairs += BannerItemPair(
            current = items[index % items.size],
            next = items[(index + 1) % items.size],
        )
    }
    return pairs
}

internal fun normalizeBannerIndex(index: Int, itemCount: Int): Int {
    if (itemCount <= 0) return -1
    val remainder = index % itemCount
    return if (remainder >= 0) remainder else remainder + itemCount
}

internal fun toLogicalBannerIndex(physicalIndex: Int, itemCount: Int): Int {
    if (itemCount <= 0) return -1
    if (itemCount == 1) return 0
    return normalizeBannerIndex(physicalIndex - LOOP_HEAD_ITEM_COUNT, itemCount)
}

internal fun toPhysicalBannerIndex(logicalIndex: Int, itemCount: Int): Int {
    if (itemCount <= 1) return 0
    return normalizeBannerIndex(logicalIndex, itemCount) + LOOP_HEAD_ITEM_COUNT
}

