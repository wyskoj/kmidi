package org.wysko.kmidi.stream

internal interface MidiInputStream {
    fun available(): Int
    fun position(): Int
    fun read(): Byte
    fun readWord(): Short
    fun readDWord(): Int
    fun readNBytes(n: Int): ByteArray
    fun readVlq(): Pair<Int, Int>
    fun skip(n: Int)
    fun read24BitInt(): Int
}