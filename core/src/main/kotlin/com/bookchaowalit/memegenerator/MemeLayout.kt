package com.bookchaowalit.memegenerator

/** Where a caption sits on the template image. */
enum class CaptionSlot { TOP, BOTTOM }

data class MemeTemplate(val id: String, val name: String, val widthPx: Int, val heightPx: Int, val slots: Set<CaptionSlot>) {
    init {
        require(widthPx > 0 && heightPx > 0) { "template size must be positive" }
        require(slots.isNotEmpty()) { "template needs at least one caption slot" }
    }
}

/** A caption laid out into lines with the font size that makes it fit. */
data class CaptionLayout(val slot: CaptionSlot, val lines: List<String>, val fontSizePx: Int)

object MemeLayout {
    /** Approximate glyph width of the bold "Impact"-style font relative to font size. */
    const val CHAR_WIDTH_RATIO = 0.6
    const val LINE_HEIGHT_RATIO = 1.1
    const val MIN_FONT_PX = 12

    /** Classic meme style: upper-case, whitespace collapsed. */
    fun normalize(text: String): String {
        val words = mutableListOf<String>()
        val current = StringBuilder()
        for (c in text) {
            when {
                // Any Unicode space (NBSP, U+2028, ideographic space, ...) separates words.
                c.isWhitespace() -> if (current.isNotEmpty()) { words += current.toString(); current.clear() }
                // Zero-width / BOM characters would otherwise produce invisible captions.
                c in INVISIBLE -> Unit
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) words += current.toString()
        return words.joinToString(" ").uppercase()
    }

    private val INVISIBLE = setOf('\u200B', '\u2060', '\uFEFF', '\u00AD')

    /**
     * Splits [text] into user-perceived characters: a base code point plus any
     * combining marks (Thai vowels/tone marks), variation selectors, emoji skin
     * tones and ZWJ-joined emoji; regional-indicator pairs (flags) stay together.
     */
    fun graphemes(text: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < text.length) {
            val start = i
            var cp = text.codePointAt(i)
            i += Character.charCount(cp)
            if (isRegionalIndicator(cp) && i < text.length && isRegionalIndicator(text.codePointAt(i))) {
                i += Character.charCount(text.codePointAt(i))
            }
            while (i < text.length) {
                val next = text.codePointAt(i)
                if (next == ZWJ) {
                    i += 1
                    if (i < text.length) { cp = text.codePointAt(i); i += Character.charCount(cp) }
                } else if (isExtender(next)) {
                    i += Character.charCount(next)
                } else {
                    break
                }
            }
            out += text.substring(start, i)
        }
        return out
    }

    private const val ZWJ = 0x200D

    private fun isRegionalIndicator(cp: Int) = cp in 0x1F1E6..0x1F1FF

    private fun isExtender(cp: Int): Boolean {
        val type = Character.getType(cp)
        return type == Character.NON_SPACING_MARK.toInt() ||
            type == Character.ENCLOSING_MARK.toInt() ||
            type == Character.COMBINING_SPACING_MARK.toInt() ||
            cp in 0xFE00..0xFE0F || cp in 0x1F3FB..0x1F3FF || cp in 0xE0020..0xE007F
    }

    /**
     * Greedy word wrap to at most [maxChars] user-perceived characters per
     * line (see [graphemes]: an emoji or a Thai syllable with its marks counts
     * once). Words longer than a line are hard-split between graphemes so
     * nothing overflows and no emoji or combining mark is cut off.
     */
    fun wrap(text: String, maxChars: Int): List<String> {
        require(maxChars > 0) { "maxChars must be positive" }
        val lines = mutableListOf<String>()
        var current = mutableListOf<String>()
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            var w = graphemes(word)
            while (w.size > maxChars) {
                if (current.isNotEmpty()) { lines += current.joinToString(""); current = mutableListOf() }
                lines += w.take(maxChars).joinToString("")
                w = w.drop(maxChars)
            }
            if (w.isEmpty()) continue
            if (current.isEmpty()) current.addAll(w)
            else if (current.size + 1 + w.size <= maxChars) { current.add(" "); current.addAll(w) }
            else { lines += current.joinToString(""); current = w.toMutableList() }
        }
        if (current.isNotEmpty()) lines += current.joinToString("")
        return lines
    }

    /**
     * Largest font size (<= [maxFontPx]) at which [text] fits in a box of
     * [boxWidthPx] x [boxHeightPx]. Falls back to [MIN_FONT_PX] when even
     * that overflows (the UI can then warn the user).
     */
    fun fit(text: String, slot: CaptionSlot, boxWidthPx: Int, boxHeightPx: Int, maxFontPx: Int = 72): CaptionLayout {
        require(boxWidthPx > 0 && boxHeightPx > 0) { "box must be positive" }
        require(maxFontPx >= MIN_FONT_PX) { "maxFontPx must be at least $MIN_FONT_PX" }
        val normalized = normalize(text)
        if (normalized.isEmpty()) return CaptionLayout(slot, emptyList(), maxFontPx)
        for (size in maxFontPx downTo MIN_FONT_PX) {
            val maxChars = (boxWidthPx / (size * CHAR_WIDTH_RATIO)).toInt()
            if (maxChars < 1) continue
            val lines = wrap(normalized, maxChars)
            if (lines.size * size * LINE_HEIGHT_RATIO <= boxHeightPx) return CaptionLayout(slot, lines, size)
        }
        val maxChars = maxOf(1, (boxWidthPx / (MIN_FONT_PX * CHAR_WIDTH_RATIO)).toInt())
        return CaptionLayout(slot, wrap(normalized, maxChars), MIN_FONT_PX)
    }

    /** Lays out captions for every slot of [template]; each slot gets the top/bottom quarter. */
    fun layout(template: MemeTemplate, captions: Map<CaptionSlot, String>): List<CaptionLayout> {
        val unknown = captions.keys - template.slots
        require(unknown.isEmpty()) { "template ${template.id} has no slot(s) $unknown" }
        // At least 1px so tiny templates degrade to the minimum font instead of throwing.
        val boxW = maxOf(1, (template.widthPx * 0.9).toInt())
        val boxH = maxOf(1, template.heightPx / 4)
        return template.slots.sortedBy { it.ordinal }.map { slot -> fit(captions[slot].orEmpty(), slot, boxW, boxH) }
    }

    /** File-system and share-safe name, e.g. "Distracted Boyfriend" -> "distracted-boyfriend.png". */
    fun exportFileName(templateName: String, extension: String = "png"): String {
        val slug = templateName.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "meme" }
        return "${slug.take(60).trimEnd('-')}.$extension"
    }
}
