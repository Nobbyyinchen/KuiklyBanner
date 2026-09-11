/*
 * MIT License
 * Copyright (c) 2026 KuiklyBanner contributors
 */
package io.github.nobbyyinchen.kuikly.banner

import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.global.GlobalFunctionRef
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexPositionType
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.View

internal data class VerticalBannerItemStyle(
    val zIndex: Int,
    val scale: Float,
    val translateY: Float,
    val opacity: Float,
)

internal fun calculateVerticalBannerItemStyle(
    itemIndex: Int,
    currentIndex: Int,
    itemCount: Int,
    visibleStackCount: Int,
    itemHeight: Float,
    stackSpacing: Float,
    scaleStep: Float,
    opacityStep: Float,
): VerticalBannerItemStyle {
    if (itemCount <= 0) {
        return VerticalBannerItemStyle(0, 0f, 0f, 0f)
    }

    val relativeIndex = normalizeBannerIndex(itemIndex - currentIndex, itemCount)
    val visibleCount = visibleStackCount.coerceIn(1, itemCount)
    if (relativeIndex < visibleCount) {
        val normalizedSpacing = stackSpacing.coerceAtLeast(0f)
        val translateY = if (relativeIndex == 0 || normalizedSpacing == 0f) {
            0f
        } else {
            -normalizedSpacing * relativeIndex
        }
        return VerticalBannerItemStyle(
            zIndex = visibleCount - relativeIndex,
            scale = (1f - scaleStep.coerceAtLeast(0f) * relativeIndex).coerceAtLeast(0f),
            translateY = translateY,
            opacity = (1f - opacityStep.coerceAtLeast(0f) * relativeIndex).coerceIn(0f, 1f),
        )
    }

    val hiddenScale = (1f - scaleStep.coerceAtLeast(0f) * visibleCount).coerceAtLeast(0f)
    val hiddenTranslateY = if (relativeIndex > itemCount / 2) {
        itemHeight.coerceAtLeast(0f)
    } else {
        -(itemHeight.coerceAtLeast(0f) + stackSpacing.coerceAtLeast(0f) * (visibleCount - 1))
    }
    return VerticalBannerItemStyle(
        zIndex = 0,
        scale = hiddenScale,
        translateY = hiddenTranslateY,
        opacity = 0f,
    )
}

/**
 * A vertically swiped, looping banner whose upcoming items are displayed as a stack.
 */
class VerticalBannerView : ComposeView<VerticalBannerAttr, VerticalBannerEvent>() {

    var currentPageIndex: Int by observable(0)
        private set

    private var autoPlayTaskCallbackId: GlobalFunctionRef = ""
    private var isViewLoaded = false
    private var gestureStartY = 0f

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    if (ctx.attr.width > 0f) {
                        width(ctx.attr.width)
                    }
                    if (ctx.attr.height > 0f) {
                        height(ctx.attr.height)
                    }
                    alignItems(FlexAlign.CENTER)
                    justifyContent(FlexJustifyContent.FLEX_END)
                    overflow(false)
                }

                repeat(ctx.attr.itemCount) { index ->
                    val style = calculateVerticalBannerItemStyle(
                        itemIndex = index,
                        currentIndex = ctx.currentPageIndex,
                        itemCount = ctx.attr.itemCount,
                        visibleStackCount = ctx.attr.visibleStackCount,
                        itemHeight = ctx.attr.itemHeight,
                        stackSpacing = ctx.attr.stackSpacing,
                        scaleStep = ctx.attr.scaleStep,
                        opacityStep = ctx.attr.opacityStep,
                    )
                    View {
                        attr {
                            positionType(FlexPositionType.ABSOLUTE)
                            left(0f)
                            right(0f)
                            bottom(0f)
                            if (ctx.attr.itemHeight > 0f) {
                                height(ctx.attr.itemHeight)
                            }
                            alignItems(FlexAlign.CENTER)
                            zIndex(style.zIndex)
                            transform(
                                scale = Scale(style.scale, style.scale),
                                translate = Translate(0f, 0f, 0f, style.translateY),
                            )
                            opacity(style.opacity)
                            animate(
                                Animation.easeInOut(ctx.attr.animationDurationSeconds.coerceAtLeast(0f)),
                                ctx.currentPageIndex,
                            )
                        }
                        ctx.attr.itemCreatorTask?.invoke(this, index)
                    }
                }

                if (ctx.attr.itemCount > 0) {
                    View {
                        attr {
                            positionType(FlexPositionType.ABSOLUTE)
                            absolutePositionAllZero()
                            zIndex(ctx.attr.visibleStackCount.coerceAtLeast(1) + 1)
                            backgroundColor(Color.TRANSPARENT)
                        }
                        event {
                            click { ctx.fireItemClickEvent() }
                            pan { params ->
                                when (params.state) {
                                    "start" -> {
                                        ctx.gestureStartY = params.pageY
                                        ctx.stopLoopPlayIfNeeded()
                                    }

                                    "end" -> {
                                        ctx.handleGestureEnd(params.pageY - ctx.gestureStartY)
                                        ctx.startLoopPlayIfNeeded()
                                    }

                                    "cancel" -> ctx.startLoopPlayIfNeeded()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun createAttr() = VerticalBannerAttr()

    override fun createEvent() = VerticalBannerEvent()

    override fun viewDidLoad() {
        super.viewDidLoad()
        isViewLoaded = true
        currentPageIndex = normalizeBannerIndex(attr.defaultPageIndex, attr.itemCount).coerceAtLeast(0)
        startLoopPlayIfNeeded()
    }

    override fun didRemoveFromParentView() {
        isViewLoaded = false
        stopLoopPlayIfNeeded()
        super.didRemoveFromParentView()
    }

    fun scrollToPage(index: Int) {
        updateCurrentPage(index)
    }

    fun showNextPage() {
        updateCurrentPage(currentPageIndex + 1)
    }

    fun showPreviousPage() {
        updateCurrentPage(currentPageIndex - 1)
    }

    fun startLoopPlayIfNeeded() {
        stopLoopPlayIfNeeded()
        if (!isViewLoaded || !attr.autoPlay || attr.itemCount <= 1 || attr.loopPlayIntervalTimeMs <= 0) {
            return
        }
        autoPlayTaskCallbackId = setTimeout(pagerId, attr.loopPlayIntervalTimeMs) {
            autoPlayTaskCallbackId = ""
            if (isViewLoaded) {
                showNextPage()
                startLoopPlayIfNeeded()
            }
        }
    }

    fun stopLoopPlayIfNeeded() {
        if (autoPlayTaskCallbackId.isNotEmpty()) {
            GlobalFunctions.destroyGlobalFunction(pagerId, autoPlayTaskCallbackId)
            autoPlayTaskCallbackId = ""
        }
    }

    internal fun restartLoopPlayIfNeeded() {
        startLoopPlayIfNeeded()
    }

    private fun handleGestureEnd(deltaY: Float) {
        val threshold = attr.swipeThreshold.coerceAtLeast(0f)
        when {
            deltaY > threshold -> showNextPage()
            deltaY < -threshold -> showPreviousPage()
        }
    }

    private fun updateCurrentPage(index: Int) {
        val normalizedIndex = normalizeBannerIndex(index, attr.itemCount)
        if (normalizedIndex < 0 || normalizedIndex == currentPageIndex) return

        currentPageIndex = normalizedIndex
        val data = JSONObject()
        data.put("index", normalizedIndex)
        event.onFireEvent(VerticalBannerEvent.PAGE_INDEX_DID_CHANGED, data)
    }

    private fun fireItemClickEvent() {
        if (attr.itemCount <= 0) return
        val data = JSONObject()
        data.put("index", currentPageIndex)
        event.onFireEvent(VerticalBannerEvent.ITEM_CLICK, data)
    }
}

class VerticalBannerAttr : ComposeAttr() {
    var width: Float by observable(0f)
    var height: Float by observable(0f)
    var itemHeight: Float by observable(0f)
    var defaultPageIndex: Int by observable(0)
    var visibleStackCount: Int by observable(3)
    var stackSpacing: Float by observable(24f)
    var scaleStep: Float by observable(0.1f)
    var opacityStep: Float by observable(0.2f)
    var swipeThreshold: Float by observable(50f)
    var animationDurationSeconds: Float by observable(0.5f)

    var autoPlay: Boolean = true
        set(value) {
            if (value != field) {
                field = value
                (view() as? VerticalBannerView)?.restartLoopPlayIfNeeded()
            }
        }

    var loopPlayIntervalTimeMs: Int = 3_000
        set(value) {
            val normalizedValue = value.coerceAtLeast(0)
            if (normalizedValue != field) {
                field = normalizedValue
                (view() as? VerticalBannerView)?.restartLoopPlayIfNeeded()
            }
        }

    internal var itemCount: Int = 0
    internal var itemCreatorTask: (ViewContainer<*, *>.(Int) -> Unit)? = null

    fun <T> initSliderItems(dataList: List<T>, creator: VerticalBannerItemCreator<T>) {
        val items = dataList.toList()
        itemCount = items.size
        itemCreatorTask = if (items.isEmpty()) {
            null
        } else {
            { index -> creator(items[index], index) }
        }
    }
}

class VerticalBannerEvent : ComposeEvent() {
    fun pageIndexDidChanged(handler: EventHandlerFn) {
        register(PAGE_INDEX_DID_CHANGED, handler)
    }

    fun itemClick(handler: EventHandlerFn) {
        register(ITEM_CLICK, handler)
    }

    internal companion object {
        const val PAGE_INDEX_DID_CHANGED = "pageIndexDidChanged"
        const val ITEM_CLICK = "itemClick"
    }
}

typealias VerticalBannerItemCreator<T> =
    ViewContainer<*, *>.(item: T, index: Int) -> Unit

fun ViewContainer<*, *>.VerticalBanner(init: VerticalBannerView.() -> Unit) {
    addChild(VerticalBannerView(), init)
}
