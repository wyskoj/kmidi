object SmfExamples {
    val example1 = intArrayOf(
        0x4D, 0x54, 0x68, 0x64,
        0x00, 0x00, 0x00, 0x06,
        0x00, 0x00,
        0x00, 0x01,
        0x00, 0x60,
        0x4D, 0x54, 0x72, 0x6B,
        0x00, 0x00, 0x00, 0x3B,
        0x00, 0xFF, 0x58, 0x04, 0x04, 0x02, 0x18, 0x08,
        0x00, 0xFF, 0x51, 0x03, 0x07, 0xA1, 0x20,
        0x00, 0xC0, 0x05,
        0x00, 0xC1, 0x2E,
        0x00, 0xC2, 0x46,
        0x00, 0x92, 0x30, 0x60,
        0x00, 0x3C, 0x60,
        0x60, 0x91, 0x43, 0x40,
        0x60, 0x90, 0x4C, 0x20,
        0x81, 0x40, 0x82, 0x30, 0x40,
        0x00, 0x3C, 0x40,
        0x00, 0x81, 0x43, 0x40,
        0x00, 0x80, 0x4C, 0x40,
        0x00, 0xFF, 0x2F, 0x00,
    ).map { it.toByte() }.toByteArray()
}