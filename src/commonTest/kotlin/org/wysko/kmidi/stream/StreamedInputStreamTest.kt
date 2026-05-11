package org.wysko.kmidi.stream

import junit.framework.TestCase
import org.junit.Assert
import kotlin.test.Test
import java.io.ByteArrayInputStream
import java.io.EOFException

class StreamedInputStreamTest {
	@Test
	fun `Test read`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
		TestCase.assertEquals(1, inputStream.read())
		TestCase.assertEquals(2, inputStream.read())
		TestCase.assertEquals(3, inputStream.read())
	}

	@Test
	fun `Test read word`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(69, 42)))
		TestCase.assertEquals(17706, inputStream.readWord())
	}

	@Test
	fun `Test read dword`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))
		TestCase.assertEquals(16909060, inputStream.readDWord())
	}

	@Test
	fun `Test read n bytes`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4, 5)))
		Assert.assertArrayEquals(byteArrayOf(0, 1, 2), inputStream.readNBytes(3))
		Assert.assertArrayEquals(byteArrayOf(3, 4, 5), inputStream.readNBytes(3))
	}

	@Test
	fun `Test skip`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4)))
		inputStream.skip(3)
		TestCase.assertEquals(3, inputStream.read())
		TestCase.assertEquals(4, inputStream.read())
	}

	@Test
	fun `Test 24 bit int`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(4, 20, 69)))
		TestCase.assertEquals(267333, inputStream.read24BitInt())
	}

	@Test
	fun `Test read empty stream`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf()))
		TestCase.assertEquals((-1).toByte(), inputStream.read())
	}

	@Test(expected = EOFException::class)
	fun `Test skip too many bytes`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0, 1, 2)))
		inputStream.skip(4)
	}

	@Test
	fun `Test read n bytes with short input`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0)))
		Assert.assertArrayEquals(byteArrayOf(0), inputStream.readNBytes(2))
	}

	@Test
	fun `Test read vlq`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0x81.toByte(), 0x80.toByte(), 0x00)))
		TestCase.assertEquals(0x4000 to 3, inputStream.readVlq())
	}

	@Test
	fun `Test position`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4)))
		TestCase.assertEquals(0, inputStream.position())
		inputStream.read()
		TestCase.assertEquals(1, inputStream.position())
		inputStream.skip(2)
		TestCase.assertEquals(3, inputStream.position())
	}

	@Test
	fun `Test available`() {
		val inputStream = StreamedInputStream(ByteArrayInputStream(byteArrayOf(0, 1, 2, 3, 4)))
		TestCase.assertEquals(5, inputStream.available())
		inputStream.read()
		TestCase.assertEquals(4, inputStream.available())
		inputStream.skip(2)
		TestCase.assertEquals(2, inputStream.available())
	}
}