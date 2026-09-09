package com.quarkdown.bibliographer.token

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextFormatTest {
    @Test
    fun `default format is plain`() {
        assertTrue(TextFormat().isPlain)
        assertTrue(TextFormat.Plain.isPlain)
    }

    @Test
    fun `any attribute makes the format non-plain`() {
        assertFalse(TextFormat(italic = true).isPlain)
        assertFalse(TextFormat(bold = true).isPlain)
        assertFalse(TextFormat(smallCaps = true).isPlain)
    }
}
