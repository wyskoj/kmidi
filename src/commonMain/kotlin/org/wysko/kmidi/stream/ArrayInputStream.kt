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

package org.wysko.kmidi.stream

import org.wysko.kmidi.midi.reader.UnexpectedEndOfFileException
import org.wysko.kmidi.util.shl
import kotlin.experimental.and
import kotlin.experimental.or

@Suppress("MagicNumber")
internal class ArrayInputStream(
    private val bytes: ByteArray,
) : MidiInputStream {
    private var position = 0

    override fun available(): Int = bytes.size - position

    override fun position(): Int = position

    override fun read(): Byte = if (position < bytes.size) bytes[position++] else throw UnexpectedEndOfFileException()

    override fun readWord(): Short = (read().toShort() shl 8) or read()

    override fun readDWord(): Int = readNBytes(4).let {
        (it[0].toInt() and 0xFF shl 24) or
                (it[1].toInt() and 0xFF shl 16) or
                (it[2].toInt() and 0xFF shl 8) or
                (it[3].toInt() and 0xFF)
    }

    override fun readNBytes(n: Int): ByteArray {
        val array = ByteArray(n)
        repeat(n) {
            array[it] = read()
        }
        return array
    }

    override fun readVlq(): Pair<Int, Int> {
        var value = 0
        var byte: Byte
        val start = position
        do {
            byte = read()
            value = (value shl 7) or (byte and 0x7F).toInt()
        } while (byte and 0x80.toByte() != 0.toByte())
        return value to (position - start)
    }

    override fun skip(n: Int) {
        if (position + n > bytes.size) {
            throw IndexOutOfBoundsException("Cannot skip $n bytes: only ${bytes.size - position} bytes remaining")
        }
        position += n
    }

    override fun read24BitInt(): Int {
        val bytes = readNBytes(3)
        val i = (bytes[0].toInt() and 0xFF) shl 16
        val j = (bytes[1].toInt() and 0xFF) shl 8
        val k = bytes[2].toInt() and 0xFF
        return i or j or k
    }
}

private const val SHORT_MASK: Short = 0b00000000_11111111

private infix fun Short.or(read: Byte): Short = this or (read.toShort() and SHORT_MASK)
