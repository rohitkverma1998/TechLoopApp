package com.book.teachloop

import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterLabelTest {
    @Test
    fun chapterLabel_includesChapterNumberAndTitle() {
        val label = chapterLabel(1, text("Revision", "पुनरावृत्ति"))

        assertEquals("Chapter 1 : Revision", label.english)
        assertEquals("अध्याय 1 : पुनरावृत्ति", label.hindi)
    }

    @Test
    fun chapterLabel_usesEnglishWhenHindiTitleMissing() {
        val label = chapterLabel(3, text("Large Numbers", ""))

        assertEquals("Chapter 3 : Large Numbers", label.english)
        assertEquals("अध्याय 3 : Large Numbers", label.hindi)
    }
}
