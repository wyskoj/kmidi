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

import org.wysko.kmidi.midi.UnexpectedEndOfFileException
import org.wysko.kmidi.util.shl
import kotlin.experimental.and
import kotlin.experimental.or

@Suppress("MagicNumber")
internal class ArrayInputStream(
    private val bytes: ByteArray,
) {
    var position = 0
        private set

    fun read(): Byte = if (position < bytes.size) bytes[position++] else throw UnexpectedEndOfFileException()

    fun readWord(): Short = (read().toShort() shl 8) or read()

    fun readDWord(): Int =
        readNBytes(4).let {
            return (it[0].toInt() and 0xFF shl 24) or
                (it[1].toInt() and 0xFF shl 16) or
                (it[2].toInt() and 0xFF shl 8) or
                (it[3].toInt() and 0xFF)
        }

    fun readNBytes(n: Int): ByteArray {
        val array = ByteArray(n)
        repeat(n) {
            array[it] = read()
        }
        return array
    }

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

    fun skip(n: Int) {
        if (position + n > bytes.size) {
            throw IndexOutOfBoundsException("Cannot skip $n bytes: only ${bytes.size - position} bytes remaining")
        }
        position += n
    }

    fun read24BitInt(): Int {
        val bytes = readNBytes(3)
        val i = (bytes[0].toInt() and 0xFF) shl 16
        val j = (bytes[1].toInt() and 0xFF) shl 8
        val k = bytes[2].toInt() and 0xFF
        return i or j or k
    }
}

private const val SHORT_MASK: Short = 0b00000000_11111111

private infix fun Short.or(read: Byte): Short = this or (read.toShort() and SHORT_MASK)
