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
    fun normalize(text: String): String = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ").uppercase()

    /**
     * Greedy word wrap to at most [maxChars] per line. Words longer than a
     * line are hard-split so nothing overflows.
     */
    fun wrap(text: String, maxChars: Int): List<String> {
        require(maxChars > 0) { "maxChars must be positive" }
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in text.split(' ').filter { it.isNotEmpty() }) {
            var w = word
            while (w.length > maxChars) {
                if (current.isNotEmpty()) { lines += current.toString(); current = StringBuilder() }
                // Never cut an emoji / astral character (surrogate pair) in half.
                var cut = maxChars
                if (Character.isHighSurrogate(w[cut - 1])) cut = if (cut > 1) cut - 1 else 2
                lines += w.take(cut)
                w = w.drop(cut)
            }
            if (w.isEmpty()) continue
            if (current.isEmpty()) current.append(w)
            else if (current.length + 1 + w.length <= maxChars) current.append(' ').append(w)
            else { lines += current.toString(); current = StringBuilder(w) }
        }
        if (current.isNotEmpty()) lines += current.toString()
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
