package com.bookchaowalit.memegenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemeLayoutTest {
    @Test
    fun normalizesToUpperCase() {
        assertEquals("ONE DOES NOT SIMPLY", MemeLayout.normalize("  one  does\nnot simply "))
    }

    @Test
    fun wrapsGreedilyAndHardSplitsLongWords() {
        assertEquals(listOf("ONE DOES", "NOT", "SIMPLY"), MemeLayout.wrap("ONE DOES NOT SIMPLY", 8))
        assertEquals(listOf("AB", "ABCDE", "FGHIJ", "K"), MemeLayout.wrap("AB ABCDEFGHIJK", 5))
        assertTrue(MemeLayout.wrap("", 5).isEmpty())
        assertFailsWith<IllegalArgumentException> { MemeLayout.wrap("A", 0) }
    }

    @Test
    fun shortTextGetsLargestFont() {
        val l = MemeLayout.fit("hi", CaptionSlot.TOP, 900, 200)
        assertEquals(72, l.fontSizePx)
        assertEquals(listOf("HI"), l.lines)
    }

    @Test
    fun longTextShrinksUntilItFits() {
        val text = "when the build passes on the first try and you do not know why"
        val l = MemeLayout.fit(text, CaptionSlot.BOTTOM, 400, 120)
        assertTrue(l.fontSizePx < 72)
        assertTrue(l.lines.size * l.fontSizePx * MemeLayout.LINE_HEIGHT_RATIO <= 120)
        val maxChars = (400 / (l.fontSizePx * MemeLayout.CHAR_WIDTH_RATIO)).toInt()
        assertTrue(l.lines.all { it.length <= maxChars })
        assertEquals(MemeLayout.normalize(text), l.lines.joinToString(" "))
    }

    @Test
    fun impossibleFitFallsBackToMinimumFont() {
        val l = MemeLayout.fit("word ".repeat(200), CaptionSlot.TOP, 100, 20)
        assertEquals(MemeLayout.MIN_FONT_PX, l.fontSizePx)
    }

    @Test
    fun layoutUsesTemplateSlots() {
        val t = MemeTemplate("drake", "Drake", 1000, 800, setOf(CaptionSlot.BOTTOM, CaptionSlot.TOP))
        val layouts = MemeLayout.layout(t, mapOf(CaptionSlot.TOP to "top text"))
        assertEquals(listOf(CaptionSlot.TOP, CaptionSlot.BOTTOM), layouts.map { it.slot })
        assertTrue(layouts[1].lines.isEmpty())
        val single = MemeTemplate("s", "S", 100, 100, setOf(CaptionSlot.TOP))
        assertFailsWith<IllegalArgumentException> { MemeLayout.layout(single, mapOf(CaptionSlot.BOTTOM to "x")) }
    }

    @Test
    fun exportFileNameIsSafe() {
        assertEquals("distracted-boyfriend.png", MemeLayout.exportFileName("Distracted Boyfriend"))
        assertEquals("meme.jpg", MemeLayout.exportFileName("!!!", "jpg"))
        assertEquals("a-b.png", MemeLayout.exportFileName("../a/b"))
    }
}
