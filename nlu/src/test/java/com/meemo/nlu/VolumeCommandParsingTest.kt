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
    fun `parses turn up and turn down volume phrasing`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = null, delta = 1),
            parser.parse("turn up the volume", InputSource.TEXT).intent
        )
        assertEquals(
            MeemoIntent.SetVolume(percent = null, delta = -1),
            parser.parse("turn down volume", InputSource.TEXT).intent
        )
    }

    @Test
    fun `parses Bengali percentage words after the sound alias`() {
        assertEquals(
            MeemoIntent.SetVolume(percent = 50, delta = null),
            parser.parse("সাউন্ড ৫০ শতাংশে সেট করো", InputSource.TEXT).intent
        )
        assertEquals(
            MeemoIntent.SetVolume(percent = 50, delta = null),
            parser.parse("সাউন্ড ৫০ পার্সেন্টে সেট করো", InputSource.TEXT).intent
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
