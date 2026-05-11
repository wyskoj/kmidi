package org.wysko.kmidi.stream

import java.io.InputStream
import kotlin.experimental.and

internal class StreamedInputStream(
    private val inputStream: InputStream
) : MidiInputStream {
    private var position = 0

    override fun available() = inputStream.available()

    override fun position() = position

    override fun read(): Byte {
        position++
        return inputStream.read().toByte()
    }

    override fun readWord(): Short {
        val msb = read().toInt() and 0xFF
        val lsb = read().toInt() and 0xFF
        return ((msb shl 8) or lsb).toShort()
    }

    override fun readDWord(): Int = readNBytes(4).let {
        (it[0].toInt() and 0xFF shl 24) or
                (it[1].toInt() and 0xFF shl 16) or
                (it[2].toInt() and 0xFF shl 8) or
                (it[3].toInt() and 0xFF)
    }

    override fun readNBytes(n: Int): ByteArray {
        position += n
        return inputStream.readNBytes(n)
    }

    override fun readVlq(): Pair<Int, Int> {
        var value = 0
        var byte: Byte
        var bytesRead = 0
        do {
            byte = read()
            bytesRead++
            value = (value shl 7) or (byte and 0x7F).toInt()
        } while (byte and 0x80.toByte() != 0.toByte())
        return value to bytesRead
    }

    override fun skip(n: Int) {
        position += n
        inputStream.skipNBytes(n.toLong())
    }

    override fun read24BitInt(): Int {
        val bytes = readNBytes(3)
        val i = (bytes[0].toInt() and 0xFF) shl 16
        val j = (bytes[1].toInt() and 0xFF) shl 8
        val k = bytes[2].toInt() and 0xFF
        return i or j or k
    }
}
