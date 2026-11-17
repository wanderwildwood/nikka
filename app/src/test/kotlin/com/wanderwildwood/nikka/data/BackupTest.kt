package com.wanderwildwood.nikka.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayInputStream

class BackupTest {

    @Test
    fun `a file within the limit is read whole`() {
        val text = "{\"format\":\"x\"}"
        assertEquals(text, Backup.readText(ByteArrayInputStream(text.toByteArray())))
    }

    @Test
    fun `a file past the limit is refused`() {
        val big = ByteArray(Backup.MAX_BYTES + 1) { 'a'.code.toByte() }
        assertThrows(IllegalArgumentException::class.java) { Backup.readText(ByteArrayInputStream(big)) }
    }
}
