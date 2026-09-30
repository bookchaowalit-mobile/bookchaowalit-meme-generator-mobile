package com.bookchaowalit.memegenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MemeLayoutEdgeCaseTest {

    @Test
    fun hardSplitNeverCutsASurrogatePair() {
        val lines = MemeLayout.wrap("A😀😀😀", 2)
        assertEquals("A😀😀😀", lines.joinToString(""))
        assertTrue(lines.none { Character.isHighSurrogate(it.last()) || Character.isLowSurrogate(it.first()) }, "$lines")
        // A width of one char still makes progress on an emoji.
        assertEquals(listOf("😀", "😀"), MemeLayout.wrap("😀😀", 1))
    }

    @Test
    fun wrapTreatsRepeatedSpacesAsOne() {
        assertEquals(listOf("A B", "C"), MemeLayout.wrap("A   B  C", 3))
    }

    @Test
    fun normalizeHandlesEmptyAndUnicode() {
        assertEquals("", MemeLayout.normalize(" \n\t "))
        assertEquals("STRASSE ÉTÉ", MemeLayout.normalize("straße été"))
        assertEquals("สวัสดี ครับ", MemeLayout.normalize(" สวัสดี   ครับ "))
    }

    @Test
    fun fitValidatesArguments() {
        assertFailsWith<IllegalArgumentException> { MemeLayout.fit("x", CaptionSlot.TOP, 0, 10) }
        assertFailsWith<IllegalArgumentException> { MemeLayout.fit("x", CaptionSlot.TOP, 10, 0) }
        assertFailsWith<IllegalArgumentException> { MemeLayout.fit("x", CaptionSlot.TOP, 100, 100, maxFontPx = 4) }
        assertEquals(MemeLayout.MIN_FONT_PX, MemeLayout.fit("x", CaptionSlot.TOP, 100, 100, maxFontPx = MemeLayout.MIN_FONT_PX).fontSizePx)
    }

    @Test
    fun fitIsMonotonicInBoxSize() {
        val text = "when the build finally passes on the first try"
        val small = MemeLayout.fit(text, CaptionSlot.TOP, 300, 80)
        val large = MemeLayout.fit(text, CaptionSlot.TOP, 600, 160)
        assertTrue(large.fontSizePx >= small.fontSizePx)
    }

    @Test
    fun tinyTemplatesFallBackInsteadOfThrowing() {
        val tiny = MemeTemplate("t", "Tiny", 1, 3, setOf(CaptionSlot.TOP, CaptionSlot.BOTTOM))
        val layouts = MemeLayout.layout(tiny, mapOf(CaptionSlot.TOP to "hello"))
        assertEquals(MemeLayout.MIN_FONT_PX, layouts[0].fontSizePx)
        assertEquals("HELLO", layouts[0].lines.joinToString(""))
    }

    @Test
    fun templateValidation() {
        assertFailsWith<IllegalArgumentException> { MemeTemplate("x", "X", 0, 10, setOf(CaptionSlot.TOP)) }
        assertFailsWith<IllegalArgumentException> { MemeTemplate("x", "X", 10, 10, emptySet()) }
    }

    @Test
    fun exportFileNameIsBoundedAndNeverEndsInDash() {
        val name = MemeLayout.exportFileName("a".repeat(59) + " b" + "c".repeat(40))
        assertTrue(name.length <= 64, name)
        assertTrue(!name.removeSuffix(".png").endsWith("-"), name)
        assertEquals("meme.png", MemeLayout.exportFileName("มีม"))
    }
}
