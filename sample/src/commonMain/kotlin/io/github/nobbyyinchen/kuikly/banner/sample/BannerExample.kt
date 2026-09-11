package io.github.nobbyyinchen.kuikly.banner.sample

import io.github.nobbyyinchen.kuikly.banner.DoubleBanner
import io.github.nobbyyinchen.kuikly.banner.PeekBannerV2
import io.github.nobbyyinchen.kuikly.banner.VerticalBanner
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

data class BannerExampleItem(
    val title: String,
    val color: Color,
    val secondaryColor: Color,
)

private val exampleItems = listOf(
    BannerExampleItem("One", Color(0xFF5B8FF9), Color(0xFFD6E4FF)),
    BannerExampleItem("Two", Color(0xFF61DDAA), Color(0xFFD9F7BE)),
    BannerExampleItem("Three", Color(0xFF65789B), Color(0xFFE6F7FF)),
)

/** A source-only showcase that can be embedded in any Kuikly Pager. */
fun ViewContainer<*, *>.BannerExample(onIndexChanged: (Int) -> Unit = {}) {
    View {
        attr {
            width(375f)
            flexDirectionColumn()
        }

        PeekBannerV2 {
            attr {
                width = 375f
                height = 120f
                pageItemWidth = 319f
                pageItemHeight = 120f
                loopPlayIntervalTimeMs = 3_000
                initSliderItems(exampleItems) { item, _ ->
                    View {
                        attr {
                            width(303f)
                            height(120f)
                            marginRight(16f)
                            borderRadius(12f)
                            backgroundColor(item.color)
                            allCenter()
                        }
                        Text {
                            attr {
                                text(item.title)
                                fontSize(20f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            }
            event {
                pageIndexDidChanged { params ->
                    onIndexChanged((params as JSONObject).optInt("index"))
                }
            }
        }

        DoubleBanner {
            attr {
                marginTop(24f)
                containerWidth = 375f
                mainHeight = 160f
                subHeight = 64f
                mainPageItemWidth = 319f
                subPageItemWidth = 375f
                overlapHeight = 16f
                loopPlayIntervalTimeMs = 3_000
                initSliderItems(
                    dataList = exampleItems,
                    mainCreator = { item, _ ->
                        View {
                            attr {
                                width(303f)
                                height(160f)
                                marginRight(16f)
                                borderRadius(12f)
                                backgroundColor(item.color)
                            }
                        }
                    },
                    subCreator = { item, _ ->
                        View {
                            attr {
                                width(375f)
                                height(64f)
                                backgroundColor(item.secondaryColor)
                                allCenter()
                            }
                            Text {
                                attr {
                                    text(item.title)
                                    fontSize(16f)
                                    color(Color.BLACK)
                                }
                            }
                        }
                    },
                )
            }
            event {
                pageIndexDidChanged { params ->
                    onIndexChanged((params as JSONObject).optInt("index"))
                }
            }
        }

        VerticalBanner {
            attr {
                marginTop(24f)
                width = 375f
                height = 168f
                itemHeight = 120f
                visibleStackCount = 3
                stackSpacing = 24f
                loopPlayIntervalTimeMs = 3_000
                initSliderItems(exampleItems) { item, _ ->
                    View {
                        attr {
                            width(343f)
                            height(120f)
                            borderRadius(12f)
                            backgroundColor(item.color)
                            allCenter()
                        }
                        Text {
                            attr {
                                text(item.title)
                                fontSize(20f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            }
            event {
                pageIndexDidChanged { params ->
                    onIndexChanged((params as JSONObject).optInt("index"))
                }
            }
        }
    }
}
