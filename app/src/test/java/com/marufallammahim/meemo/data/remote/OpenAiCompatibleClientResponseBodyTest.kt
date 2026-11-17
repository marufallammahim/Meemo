package com.marufallammahim.meemo.data.remote

import okhttp3.MediaType
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenAiCompatibleClientResponseBodyTest {

    @Test
    fun `accepts a response exactly at the byte limit`() {
        val expected = "a".repeat(MAX_AI_RESPONSE_BYTES)
        val body = expected.toResponseBody("text/plain".toMediaType())

        assertEquals(expected, readBoundedResponseBody(body))
    }

    @Test
    fun `rejects oversized responses without a content length`() {
        val body = unknownLengthBody("x".repeat(MAX_AI_RESPONSE_BYTES + 1))

        assertNull(readBoundedResponseBody(body))
    }

    @Test
    fun `enforces the limit using UTF-8 bytes rather than character count`() {
        val text = "বাংলা".repeat(2_200)
        assert(text.length < MAX_AI_RESPONSE_BYTES)
        val body = text.toResponseBody("text/plain".toMediaType())

        assertNull(readBoundedResponseBody(body))
    }

    private fun unknownLengthBody(text: String): ResponseBody {
        val buffer = Buffer().writeUtf8(text)
        return object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength(): Long = -1L
            override fun source(): BufferedSource = buffer
        }
    }
}
