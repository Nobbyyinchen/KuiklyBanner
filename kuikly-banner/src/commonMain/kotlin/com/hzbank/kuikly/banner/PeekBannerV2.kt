/*
 * MIT License
 * Copyright (c) 2026 KuiklyBanner contributors
 */
package com.hzbank.kuikly.banner

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.global.GlobalFunctionRef
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.PageList
import com.tencent.kuikly.core.views.PageListEvent
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.ScrollParams

/**
 * A looping PageList that can expose part of the adjacent item.
 */
class PeekBannerV2View : ComposeView<PeekBannerV2Attr, PeekBannerV2Event>() {

    var pageListRef: ViewRef<PageListView<*, *>>? = null
        private set

    var currentPageIndex: Int = 0
        private set

    private var timeoutTaskCallbackId: GlobalFunctionRef = ""
    private var isDragging = false
    private var isAutoPlaying = false
    private var isViewLoaded = false

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            PageList {
                ref { ctx.pageListRef = it }
                attr {
                    scrollEnable(ctx.attr.scrollEnable)
                    if (ctx.attr.pageItemWidth > 0f) {
                        pageItemWidth(ctx.attr.pageItemWidth)
                    }
                    if (ctx.attr.pageItemHeight > 0f) {
                        pageItemHeight(ctx.attr.pageItemHeight)
                    }
                    // PageList.pageItemWidth/pageItemHeight also set the viewport size.
                    // Apply an explicit viewport last so a wider viewport can reveal the next item.
                    if (ctx.attr.width > 0f) {
                        width(ctx.attr.width)
                    }
                    if (ctx.attr.height > 0f) {
                        height(ctx.attr.height)
                    }
                    val physicalIndex = toPhysicalBannerIndex(ctx.attr.defaultPageIndex, ctx.attr.itemCount)
                    defaultPageIndex(physicalIndex)
                    firstContentLoadMaxIndex(physicalIndex + 2)
                    pageDirection(ctx.attr.isHorizontal)
                    showScrollerIndicator(false)
                    overflow(false)
                    keepItemAlive(true)
                }

                ctx.attr.lazyCreateItemsTask?.let { apply(it) }

                event {
                    scroll { ctx.resetContentOffsetIfNeeded(it) }
                    pageIndexDidChanged { ctx.onPhysicalPageChanged(it as JSONObject) }
                    dragBegin {
                        ctx.isDragging = true
                        ctx.stopLoopPlayIfNeeded()
                    }
                    dragEnd {
                        ctx.isDragging = false
                        ctx.startLoopPlayIfNeeded()
                    }
                }
            }
        }
    }

    override fun createAttr() = PeekBannerV2Attr()

    override fun createEvent() = PeekBannerV2Event()

    override fun viewDidLoad() {
        super.viewDidLoad()
        isViewLoaded = true
        currentPageIndex = normalizeBannerIndex(attr.defaultPageIndex, attr.itemCount).coerceAtLeast(0)
        startLoopPlayIfNeeded()
    }

    override fun didRemoveFromParentView() {
        isViewLoaded = false
        stopLoopPlayIfNeeded()
        pageListRef = null
        super.didRemoveFromParentView()
    }

    fun startLoopPlayIfNeeded() {
        if (attr.itemCount > 1 && attr.loopPlayIntervalTimeMs > 0 && !isAutoPlaying && isViewLoaded) {
            scheduleNextPage()
        }
    }

    fun stopLoopPlayIfNeeded() {
        if (timeoutTaskCallbackId.isNotEmpty()) {
            GlobalFunctions.destroyGlobalFunction(pagerId, timeoutTaskCallbackId)
            timeoutTaskCallbackId = ""
        }
        isAutoPlaying = false
    }

    internal fun restartLoopPlayIfNeeded() {
        stopLoopPlayIfNeeded()
        startLoopPlayIfNeeded()
    }

    fun scrollToPage(index: Int, animation: Boolean = false) {
        val pageListView = pageListRef?.view ?: return
        val viewWidth = pageListView.flexNode.layoutFrame.width
        val viewHeight = pageListView.flexNode.layoutFrame.height
        if (pageListView.renderView != null && !isDragging && viewWidth > 0f && viewHeight > 0f) {
            pageListView.scrollToPageIndex(
                toPhysicalBannerIndex(index, attr.itemCount),
                animation,
                null,
            )
        }
    }

    private fun scheduleNextPage() {
        isAutoPlaying = true
        timeoutTaskCallbackId = setTimeout(pagerId, attr.loopPlayIntervalTimeMs) {
            timeoutTaskCallbackId = ""
            if (attr.loopPlayIntervalTimeMs > 0 && isViewLoaded) {
                scrollToPage(currentPageIndex + 1, true)
                scheduleNextPage()
            } else {
                isAutoPlaying = false
            }
        }
    }

    private fun onPhysicalPageChanged(data: JSONObject) {
        val logicalIndex = toLogicalBannerIndex(data.optInt("index"), attr.itemCount)
        if (logicalIndex < 0 || logicalIndex == currentPageIndex) return

        currentPageIndex = logicalIndex
        data.put("index", logicalIndex)
        event.onFireEvent(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, data)
    }

    private fun resetContentOffsetIfNeeded(params: ScrollParams) {
        if (attr.itemCount <= 1) return

        if (attr.isHorizontal) {
            val itemWidth = attr.pageItemWidth.takeIf { it > 0f } ?: params.viewWidth
            resetHorizontalOffset(params.offsetX, itemWidth)
        } else {
            val itemHeight = attr.pageItemHeight.takeIf { it > 0f } ?: params.viewHeight
            resetVerticalOffset(params.offsetY, itemHeight)
        }
    }

    private fun resetHorizontalOffset(offsetX: Float, itemWidth: Float) {
        if (itemWidth <= 0f) return
        val cycleWidth = attr.itemCount * itemWidth
        when {
            offsetX <= 0.1f -> pageListRef?.view?.setContentOffset(offsetX + cycleWidth, 0f, false)
            offsetX + 1f >= (attr.itemCount + LOOP_HEAD_ITEM_COUNT) * itemWidth ->
                pageListRef?.view?.setContentOffset(offsetX - cycleWidth, 0f, false)
        }
    }

    private fun resetVerticalOffset(offsetY: Float, itemHeight: Float) {
        if (itemHeight <= 0f) return
        val cycleHeight = attr.itemCount * itemHeight
        when {
            offsetY <= 0.1f -> pageListRef?.view?.setContentOffset(0f, offsetY + cycleHeight, false)
            offsetY + 1f >= (attr.itemCount + LOOP_HEAD_ITEM_COUNT) * itemHeight ->
                pageListRef?.view?.setContentOffset(0f, offsetY - cycleHeight, false)
        }
    }
}

class PeekBannerV2Attr : ComposeAttr() {
    var width: Float by observable(0f)
    var height: Float by observable(0f)
    var defaultPageIndex: Int by observable(0)
    var isHorizontal: Boolean by observable(true)
    var pageItemWidth: Float by observable(0f)
    var pageItemHeight: Float by observable(0f)
    var scrollEnable: Boolean by observable(true)

    /** Autoplay interval in milliseconds. A value less than or equal to zero disables autoplay. */
    var loopPlayIntervalTimeMs: Int = 3_000
        set(value) {
            val normalizedValue = value.coerceAtLeast(0)
            if (normalizedValue != field) {
                field = normalizedValue
                (view() as? PeekBannerV2View)?.restartLoopPlayIfNeeded()
            }
        }

    internal var itemCount: Int = 0
    internal var lazyCreateItemsTask: (PageListView<*, *>.() -> Unit)? = null

    fun <T> initSliderItems(dataList: List<T>, creator: PeekBannerV2ItemCreator<T>) {
        val items = dataList.toList()
        itemCount = items.size
        lazyCreateItemsTask = if (items.isEmpty()) {
            null
        } else {
            {
                buildLoopItemPairs(items).forEach { pair ->
                    creator(pair.current, pair.next)
                }
            }
        }
    }
}

class PeekBannerV2Event : ComposeEvent() {
    fun pageIndexDidChanged(handler: EventHandlerFn) {
        register(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, handler)
    }
}

typealias PeekBannerV2ItemCreator<T> =
    PageListView<*, *>.(currentItem: T, nextItem: T) -> Unit

fun ViewContainer<*, *>.PeekBannerV2(init: PeekBannerV2View.() -> Unit) {
    addChild(PeekBannerV2View(), init)
}

