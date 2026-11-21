package com.meemo.nlu

import com.meemo.core.model.InputSource
import com.meemo.core.model.MeemoIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VolumeCommandParsingTest {
    private val parser = CommandParser()

    @Test
    fun `parses English volume percentage`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = 50, delta = null),
            parser.parse("set volume to 50 percent", InputSource.TEXT).intent
        )
    }

    @Test
    fun `parses Bengali numerals in a Bengali volume command`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = 50, delta = null),
            parser.parse("ভলিউম ৫০ সেট করো", InputSource.TEXT).intent
        )
    }

    @Test
    fun `parses Bengali volume increase and Banglish decrease`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = null, delta = 1),
            parser.parse("ভলিউম বাড়াও", InputSource.TEXT).intent
        )
        assertEquals(
            MeemoIntent.SetVolume(percent = null, delta = -1),
            parser.parse("volume komao", InputSource.TEXT).intent
        )
    }

    @Test
    fun `clamps volume percentages and rejects oversized numeric tokens`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = 100, delta = null),
            parser.parse("volume 150", InputSource.TEXT).intent
        )
        assertTrue(parser.parse("volume 2026", InputSource.TEXT).intent is MeemoIntent.Unknown)
    }
}
