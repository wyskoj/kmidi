/*
 * Copyright © 2023 Jacob Wysko
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

import org.wysko.kmidi.util.shl
import kotlin.experimental.and
import kotlin.experimental.or


/**
 * An input stream that reads from an array of bytes.
 *
 * @property bytes The array of bytes to read from.
 */
@Suppress("MagicNumber")
internal class ArrayInputStream(private val bytes: ByteArray) {
    /**
     * The current position within the stream.
     */
    var position = 0
        private set

    /**
     * Reads a single byte from the stream.
     *
     * @return The next byte in the stream.
     * @throws EOFException If there are no more bytes to read.
     */
    fun read(): Byte = if (position < bytes.size) bytes[position++] else throw EOFException

    /**
     * Reads a word (two bytes) from the stream.
     *
     * @return The next word in the stream.
     * @throws EOFException If there are no more bytes to read.
     */
    fun readWord(): Short = (read().toShort() shl 8) or read().toShort()

    /**
     * Reads a double word (four bytes) from the stream.
     *
     * @return The next double word in the stream.
     * @throws EOFException If there are no more bytes to read.
     */
    fun readDWord(): Int = readNBytes(4).let {
        return (it[0].toInt() and 0xFF shl 24) or
                (it[1].toInt() and 0xFF shl 16) or
                (it[2].toInt() and 0xFF shl 8) or
                (it[3].toInt() and 0xFF)
    }

    /**
     * Reads an arbitrary number of bytes from the stream.
     *
     * @param n The number of bytes to read.
     * @return The next [n] bytes in the stream.
     * @throws EOFException If there are no more bytes to read.
     */
    fun readNBytes(n: Int): ByteArray {
        val array = ByteArray(n)
        repeat(n) {
            array[it] = read()
        }
        return array
    }

    /**
     * Reads a variable-length quantity from the stream.
     *
     * @return A pair, where the first is the value, and the second is the number of bytes read.
     * @throws EOFException If there are no more bytes to read.
     */
    fun readVlq(): Pair<Int, Int> {
        var value = 0
        var byte: Byte
        val start = position
        do {
            byte = read()
            value = (value shl 7) or (byte and 0x7F).toInt()
        } while (byte and 0x80.toByte() != 0.toByte())
        return value to (position - start)
    }

    /**
     * Skips ahead an arbitrary number of bytes in the stream.
     *
     * @param n The number of bytes to skip.
     * @throws IndexOutOfBoundsException If there is not enough bytes to skip.
     */
    fun skip(n: Int) {
        if (position + n > bytes.size) {
            throw IndexOutOfBoundsException("Cannot skip $n bytes: only ${bytes.size - position} bytes remaining")
        }
        position += n
    }

    /**
     * Reads a 24-bit integer from the stream.
     *
     * @return The next 24-bit integer in the stream.
     * @throws EOFException If there are no more bytes to read.
     */
    fun read24BitInt(): Int {
        val bytes = readNBytes(3)
        val i = (bytes[0].toInt() and 0xFF) shl 16
        val j = (bytes[1].toInt() and 0xFF) shl 8
        val k = bytes[2].toInt() and 0xFF
        return i or j or k
    }
}
