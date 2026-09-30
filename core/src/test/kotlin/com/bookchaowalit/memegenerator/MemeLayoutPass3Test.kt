package com.bookchaowalit.memegenerator

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemeLayoutPass3Test {

    @Test
    fun thaiCombiningMarksDoNotCountTowardsLineWidthAndAreNeverOrphaned() {
        // "สวัสดี" is 6 UTF-16 units but 4 visible cells (ั and ี are combining marks).
        assertEquals(listOf("สวัสดี"), MemeLayout.wrap("สวัสดี", 4))
        val lines = MemeLayout.wrap("สวัสดี", 2)
        assertEquals(listOf("สวั", "สดี"), lines)
        assertTrue(lines.none { Character.getType(it.codePointAt(0)) == Character.NON_SPACING_MARK.toInt() })
    }

    @Test
    fun emojiCountAsOneCellAndZwjSequencesStayWhole() {
        val family = "👨‍👩‍👧"
        assertEquals(listOf(family + family), MemeLayout.wrap(family + family, 2))
        assertEquals(listOf(family, family), MemeLayout.wrap(family + family, 1))
        val thumbs = "👍🏽"
        assertEquals(listOf(thumbs, thumbs), MemeLayout.wrap(thumbs + thumbs, 1))
        val flag = "🇹🇭"
        assertEquals(listOf(flag, flag), MemeLayout.wrap(flag + flag, 1))
        assertEquals(listOf("A😀", "😀😀"), MemeLayout.wrap("A😀😀😀", 2))
    }

    @Test
    fun normalizeCollapsesUnicodeSpacesAndDropsInvisibleCharacters() {
        assertEquals("ONE DOES NOT", MemeLayout.normalize("one does　not"))
        assertEquals("", MemeLayout.normalize("​⁠﻿"))
        assertEquals("HI", MemeLayout.normalize("﻿hi​"))
        assertTrue(MemeLayout.fit("​", CaptionSlot.TOP, 300, 100).lines.isEmpty())
    }
}
