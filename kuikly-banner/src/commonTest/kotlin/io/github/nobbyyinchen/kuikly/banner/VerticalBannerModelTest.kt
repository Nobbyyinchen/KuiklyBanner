package io.github.nobbyyinchen.kuikly.banner

import kotlin.test.Test
import kotlin.test.assertEquals

class VerticalBannerModelTest {

    @Test
    fun currentAndUpcomingItemsFormAStack() {
        val current = style(itemIndex = 1, currentIndex = 1)
        val firstUpcoming = style(itemIndex = 2, currentIndex = 1)
        val secondUpcoming = style(itemIndex = 3, currentIndex = 1)

        assertEquals(3, current.zIndex)
        assertEquals(1f, current.scale)
        assertEquals(0f, current.translateY)
        assertEquals(1f, current.opacity)

        assertEquals(2, firstUpcoming.zIndex)
        assertEquals(0.9f, firstUpcoming.scale)
        assertEquals(-24f, firstUpcoming.translateY)
        assertEquals(0.8f, firstUpcoming.opacity)

        assertEquals(1, secondUpcoming.zIndex)
        assertEquals(0.8f, secondUpcoming.scale)
        assertEquals(-48f, secondUpcoming.translateY)
        assertEquals(0.6f, secondUpcoming.opacity)
    }

    @Test
    fun indexesWrapAroundTheEndOfTheList() {
        val firstItemAfterLast = style(itemIndex = 0, currentIndex = 4)
        assertEquals(2, firstItemAfterLast.zIndex)
        assertEquals(-24f, firstItemAfterLast.translateY)
    }

    @Test
    fun nonStackItemsAreHidden() {
        val hidden = style(itemIndex = 4, currentIndex = 0)
        assertEquals(0, hidden.zIndex)
        assertEquals(0f, hidden.opacity)
        assertEquals(88f, hidden.translateY)
    }

    @Test
    fun styleInputsAreSafelyClamped() {
        val style = calculateVerticalBannerItemStyle(
            itemIndex = 1,
            currentIndex = 0,
            itemCount = 2,
            visibleStackCount = 10,
            itemHeight = -1f,
            stackSpacing = -1f,
            scaleStep = 2f,
            opacityStep = 2f,
        )

        assertEquals(1, style.zIndex)
        assertEquals(0f, style.scale)
        assertEquals(0f, style.translateY)
        assertEquals(0f, style.opacity)
    }

    private fun style(itemIndex: Int, currentIndex: Int): VerticalBannerItemStyle {
        return calculateVerticalBannerItemStyle(
            itemIndex = itemIndex,
            currentIndex = currentIndex,
            itemCount = 5,
            visibleStackCount = 3,
            itemHeight = 88f,
            stackSpacing = 24f,
            scaleStep = 0.1f,
            opacityStep = 0.2f,
        )
    }
}
