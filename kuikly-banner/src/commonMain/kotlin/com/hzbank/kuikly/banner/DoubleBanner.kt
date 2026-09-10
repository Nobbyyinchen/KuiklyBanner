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
import com.tencent.kuikly.core.views.View

private enum class DoubleBannerDriver {
    NONE,
    MAIN,
    SUB,
}

/**
 * Two looping PageLists whose drag progress stays synchronized.
 */
class DoubleBannerView : ComposeView<DoubleBannerViewAttr, DoubleBannerViewEvent>() {

    var pageListRef: ViewRef<PageListView<*, *>>? = null
        private set

    var subPageListRef: ViewRef<PageListView<*, *>>? = null
        private set

    var currentPageIndex: Int = 0
        private set

    private var autoPlayTaskCallbackId: GlobalFunctionRef = ""
    private var settleTaskCallbackId: GlobalFunctionRef = ""
    private var isDragging = false
    private var isAutoPlaying = false
    private var isAnimating = false
    private var isViewLoaded = false
    private var manualDriver = DoubleBannerDriver.NONE

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionColumn()
                    if (ctx.attr.containerWidth > 0f) {
                        width(ctx.attr.containerWidth)
                    }
                    overflow(true)
                }

                PageList {
                    ref { ctx.pageListRef = it }
                    attr {
                        if (ctx.attr.mainPageItemWidth > 0f) {
                            pageItemWidth(ctx.attr.mainPageItemWidth)
                        }
                        if (ctx.attr.mainHeight > 0f) {
                            pageItemHeight(ctx.attr.mainHeight)
                        }
                        if (ctx.attr.containerWidth > 0f) {
                            width(ctx.attr.containerWidth)
                        }
                        if (ctx.attr.mainHeight > 0f) {
                            height(ctx.attr.mainHeight)
                        }
                        scrollEnable(ctx.attr.scrollEnable)
                        val physicalIndex = toPhysicalBannerIndex(ctx.attr.defaultPageIndex, ctx.attr.itemCount)
                        defaultPageIndex(physicalIndex)
                        firstContentLoadMaxIndex(physicalIndex + 2)
                        pageDirection(true)
                        showScrollerIndicator(false)
                        overflow(false)
                        keepItemAlive(true)
                    }

                    ctx.attr.mainLazyCreateItemsTask?.let { apply(it) }

                    event {
                        scroll { ctx.syncFromMain(it as ScrollParams) }
                        pageIndexDidChanged {
                            ctx.schedulePageSettled(it as JSONObject, DoubleBannerDriver.MAIN)
                        }
                        dragBegin { ctx.onDragBegin(DoubleBannerDriver.MAIN) }
                        dragEnd { ctx.onDragEnd() }
                    }
                }

                PageList {
                    ref { ctx.subPageListRef = it }
                    attr {
                        if (ctx.attr.subPageItemWidth > 0f) {
                            pageItemWidth(ctx.attr.subPageItemWidth)
                        }
                        if (ctx.attr.subHeight > 0f) {
                            pageItemHeight(ctx.attr.subHeight)
                        }
                        if (ctx.attr.containerWidth > 0f) {
                            width(ctx.attr.containerWidth)
                        }
                        if (ctx.attr.subHeight > 0f) {
                            height(ctx.attr.subHeight)
                        }
                        scrollEnable(ctx.attr.subScrollEnable)
                        val physicalIndex = toPhysicalBannerIndex(ctx.attr.defaultPageIndex, ctx.attr.itemCount)
                        defaultPageIndex(physicalIndex)
                        firstContentLoadMaxIndex(physicalIndex + 2)
                        pageDirection(true)
                        showScrollerIndicator(false)
                        overflow(true)
                        if (ctx.attr.overlapHeight > 0f) {
                            marginTop(-ctx.attr.overlapHeight)
                        }
                        keepItemAlive(true)
                    }

                    ctx.attr.subLazyCreateItemsTask?.let { apply(it) }

                    event {
                        scroll { ctx.syncFromSub(it as ScrollParams) }
                        pageIndexDidChanged {
                            ctx.schedulePageSettled(it as JSONObject, DoubleBannerDriver.SUB)
                        }
                        dragBegin { ctx.onDragBegin(DoubleBannerDriver.SUB) }
                        dragEnd { ctx.onDragEnd() }
                    }
                }
            }
        }
    }

    override fun createAttr() = DoubleBannerViewAttr()

    override fun createEvent() = DoubleBannerViewEvent()

    override fun viewDidLoad() {
        super.viewDidLoad()
        isViewLoaded = true
        currentPageIndex = normalizeBannerIndex(attr.defaultPageIndex, attr.itemCount).coerceAtLeast(0)
        startLoopPlayIfNeeded()
    }

    override fun didRemoveFromParentView() {
        isViewLoaded = false
        stopLoopPlayIfNeeded()
        cancelSettleTask()
        pageListRef = null
        subPageListRef = null
        super.didRemoveFromParentView()
    }

    fun startLoopPlayIfNeeded() {
        if (attr.itemCount > 1 && attr.loopPlayIntervalTimeMs > 0 && !isAutoPlaying && isViewLoaded) {
            scheduleNextPage()
        }
    }

    fun stopLoopPlayIfNeeded() {
        if (autoPlayTaskCallbackId.isNotEmpty()) {
            GlobalFunctions.destroyGlobalFunction(pagerId, autoPlayTaskCallbackId)
            autoPlayTaskCallbackId = ""
        }
        isAutoPlaying = false
    }

    internal fun restartLoopPlayIfNeeded() {
        stopLoopPlayIfNeeded()
        startLoopPlayIfNeeded()
    }

    fun scrollToPage(index: Int, animation: Boolean = false) {
        val mainView = pageListRef?.view ?: return
        val subView = subPageListRef?.view ?: return
        if (mainView.renderView == null || subView.renderView == null || isDragging) return

        manualDriver = DoubleBannerDriver.NONE
        isAnimating = animation
        val physicalIndex = toPhysicalBannerIndex(index, attr.itemCount)
        mainView.scrollToPageIndex(physicalIndex, animation, null)
        subView.scrollToPageIndex(physicalIndex, animation, null)
    }

    private fun scheduleNextPage() {
        isAutoPlaying = true
        autoPlayTaskCallbackId = setTimeout(pagerId, attr.loopPlayIntervalTimeMs) {
            autoPlayTaskCallbackId = ""
            if (attr.loopPlayIntervalTimeMs > 0 && isViewLoaded) {
                scrollToPage(currentPageIndex + 1, true)
                scheduleNextPage()
            } else {
                isAutoPlaying = false
            }
        }
    }

    private fun onDragBegin(driver: DoubleBannerDriver) {
        manualDriver = driver
        isDragging = true
        isAnimating = false
        cancelSettleTask()
        stopLoopPlayIfNeeded()
    }

    private fun onDragEnd() {
        isDragging = false
        startLoopPlayIfNeeded()
    }

    private fun syncFromMain(params: ScrollParams) {
        if (isAnimating || manualDriver != DoubleBannerDriver.MAIN) return
        val mainWidth = attr.mainPageItemWidth
        val subWidth = attr.subPageItemWidth
        if (mainWidth <= 0f || subWidth <= 0f) return

        val pageProgress = params.offsetX / mainWidth
        subPageListRef?.view?.setContentOffset(pageProgress * subWidth, 0f, false)
    }

    private fun syncFromSub(params: ScrollParams) {
        if (isAnimating || manualDriver != DoubleBannerDriver.SUB) return
        val mainWidth = attr.mainPageItemWidth
        val subWidth = attr.subPageItemWidth
        if (mainWidth <= 0f || subWidth <= 0f) return

        val pageProgress = params.offsetX / subWidth
        pageListRef?.view?.setContentOffset(pageProgress * mainWidth, 0f, false)
    }

    private fun schedulePageSettled(data: JSONObject, source: DoubleBannerDriver) {
        if (!isPreferredEventSource(source)) return

        isAnimating = false
        cancelSettleTask()
        if (attr.settleDelayTimeMs <= 0) {
            handlePageSettled(data, source)
            return
        }

        settleTaskCallbackId = setTimeout(pagerId, attr.settleDelayTimeMs) {
            settleTaskCallbackId = ""
            if (!isDragging && isViewLoaded) {
                handlePageSettled(data, source)
            }
        }
    }

    private fun isPreferredEventSource(source: DoubleBannerDriver): Boolean {
        return when (manualDriver) {
            DoubleBannerDriver.MAIN -> source == DoubleBannerDriver.MAIN
            DoubleBannerDriver.SUB -> source == DoubleBannerDriver.SUB
            DoubleBannerDriver.NONE -> source == DoubleBannerDriver.MAIN
        }
    }

    private fun handlePageSettled(data: JSONObject, source: DoubleBannerDriver) {
        val itemCount = attr.itemCount
        val physicalIndex = data.optInt("index")
        val logicalIndex = toLogicalBannerIndex(physicalIndex, itemCount)
        if (logicalIndex < 0) return

        val canonicalPhysicalIndex = toPhysicalBannerIndex(logicalIndex, itemCount)
        if (physicalIndex != canonicalPhysicalIndex) {
            pageListRef?.view?.scrollToPageIndex(canonicalPhysicalIndex, false, null)
            subPageListRef?.view?.scrollToPageIndex(canonicalPhysicalIndex, false, null)
        } else if (source == DoubleBannerDriver.MAIN) {
            subPageListRef?.view?.scrollToPageIndex(physicalIndex, false, null)
        } else {
            pageListRef?.view?.scrollToPageIndex(physicalIndex, false, null)
        }

        if (logicalIndex == currentPageIndex) return
        currentPageIndex = logicalIndex
        data.put("index", logicalIndex)
        event.onFireEvent(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, data)
    }

    private fun cancelSettleTask() {
        if (settleTaskCallbackId.isNotEmpty()) {
            GlobalFunctions.destroyGlobalFunction(pagerId, settleTaskCallbackId)
            settleTaskCallbackId = ""
        }
    }
}

class DoubleBannerViewAttr : ComposeAttr() {
    var containerWidth: Float by observable(0f)
    var mainHeight: Float by observable(0f)
    var subHeight: Float by observable(0f)
    var mainPageItemWidth: Float by observable(0f)
    var subPageItemWidth: Float by observable(0f)
    var overlapHeight: Float by observable(0f)
    var defaultPageIndex: Int by observable(0)
    var scrollEnable: Boolean by observable(true)
    var subScrollEnable: Boolean by observable(true)
    var settleDelayTimeMs: Int by observable(250)

    /** Autoplay interval in milliseconds. A value less than or equal to zero disables autoplay. */
    var loopPlayIntervalTimeMs: Int = 3_000
        set(value) {
            val normalizedValue = value.coerceAtLeast(0)
            if (normalizedValue != field) {
                field = normalizedValue
                (view() as? DoubleBannerView)?.restartLoopPlayIfNeeded()
            }
        }

    internal var itemCount: Int = 0
    internal var mainLazyCreateItemsTask: (PageListView<*, *>.() -> Unit)? = null
    internal var subLazyCreateItemsTask: (PageListView<*, *>.() -> Unit)? = null

    fun <T> initSliderItems(
        dataList: List<T>,
        mainCreator: DoubleBannerItemCreator<T>,
        subCreator: DoubleBannerItemCreator<T>,
    ) {
        val items = dataList.toList()
        itemCount = items.size
        if (items.isEmpty()) {
            mainLazyCreateItemsTask = null
            subLazyCreateItemsTask = null
            return
        }

        val pairs = buildLoopItemPairs(items)
        mainLazyCreateItemsTask = {
            pairs.forEach { pair -> mainCreator(pair.current, pair.next) }
        }
        subLazyCreateItemsTask = {
            pairs.forEach { pair -> subCreator(pair.current, pair.next) }
        }
    }
}

class DoubleBannerViewEvent : ComposeEvent() {
    fun pageIndexDidChanged(handler: EventHandlerFn) {
        register(PageListEvent.PageListEventConst.PAGE_INDEX_DID_CHANGED, handler)
    }
}

typealias DoubleBannerItemCreator<T> =
    PageListView<*, *>.(currentItem: T, nextItem: T) -> Unit

fun ViewContainer<*, *>.DoubleBanner(init: DoubleBannerView.() -> Unit) {
    addChild(DoubleBannerView(), init)
}

