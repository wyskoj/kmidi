/*
 * Copyright © 2024 Jacob Wysko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wysko.kmidi

import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertArrayEquals
import org.wysko.kmidi.midi.reader.UnexpectedEndOfFileException
import kotlin.test.Test

class ArrayInputStreamTest {
    @Test
    fun `Test read`() {
        val inputStream = ArrayInputStream(byteArrayOf(1, 2, 3))
        assertEquals(1, inputStream.read())
        assertEquals(2, inputStream.read())
        assertEquals(3, inputStream.read())
    }

    @Test
    fun `Test read word`() {
        val inputStream = ArrayInputStream(byteArrayOf(69, 42))
        assertEquals(17706, inputStream.readWord())
    }

    @Test
    fun `Test read dword`() {
        val inputStream = ArrayInputStream(byteArrayOf(1, 2, 3, 4))
        assertEquals(16909060, inputStream.readDWord())
    }

    @Test
    fun `Test read n bytes`() {
        val inputStream = ArrayInputStream(byteArrayOf(0, 1, 2, 3, 4, 5))
        assertArrayEquals(byteArrayOf(0, 1, 2), inputStream.readNBytes(3))
        assertArrayEquals(byteArrayOf(3, 4, 5), inputStream.readNBytes(3))
    }

    @Test
    fun `Test skip`() {
        val inputStream = ArrayInputStream(byteArrayOf(0, 1, 2, 3, 4))
        inputStream.skip(3)
        assertEquals(3, inputStream.read())
        assertEquals(4, inputStream.read())
    }

    @Test
    fun `Test 24 bit int`() {
        val inputStream = ArrayInputStream(byteArrayOf(4, 20, 69))
        assertEquals(267333, inputStream.read24BitInt())
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read empty stream`() {
        val inputStream = ArrayInputStream(byteArrayOf())
        inputStream.read()
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `Test skip too many bytes`() {
        val inputStream = ArrayInputStream(byteArrayOf(0, 1, 2))
        inputStream.skip(4)
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read word with unexpected end of file`() {
        val inputStream = ArrayInputStream(byteArrayOf(0))
        inputStream.readWord()
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read dword with unexpected end of file`() {
        val inputStream = ArrayInputStream(byteArrayOf(0))
        inputStream.readDWord()
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read n bytes with unexpected end of file`() {
        val inputStream = ArrayInputStream(byteArrayOf(0))
        inputStream.readNBytes(2)
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read 24 bit int with unexpected end of file`() {
        val inputStream = ArrayInputStream(byteArrayOf(0))
        inputStream.read24BitInt()
    }

    @Test(expected = UnexpectedEndOfFileException::class)
    fun `Test read VLQ with unexpected end of file`() {
        val inputStream =
            ArrayInputStream(byteArrayOf(0x81.toByte(), 0x80.toByte()))
        inputStream.readVlq()
    }

    @Test
    fun `Test read vlq`() {
        val inputStream =
            ArrayInputStream(byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x00))
        assertEquals(0x4000 to 3, inputStream.readVlq())
    }

    @Test
    fun `Test position`() {
        val inputStream = ArrayInputStream(byteArrayOf(0, 1, 2, 3, 4))
        assertEquals(0, inputStream.position)
        inputStream.read()
        assertEquals(1, inputStream.position)
        inputStream.skip(2)
        assertEquals(3, inputStream.position)
    }
}
