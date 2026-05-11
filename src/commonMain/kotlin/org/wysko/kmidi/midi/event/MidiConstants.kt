/*
 * Copyright © 2026 Jacob Wysko
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

package org.wysko.kmidi.midi.event

internal object MidiConstants {
    object ChannelVoiceMessages {
        const val NOTE_OFF_EVENT = 0b10000000.toByte()
        const val NOTE_ON_EVENT = 0b10010000.toByte()
        const val POLYPHONIC_KEY_PRESSURE = 0b10100000.toByte()
        const val CONTROL_CHANGE = 0b10110000.toByte()
        const val PROGRAM_CHANGE = 0b11000000.toByte()
        const val CHANNEL_PRESSURE = 0b11010000.toByte()
        const val PITCH_WHEEL_CHANGE = 0b11100000.toByte()
    }

    object SpecialMessages {
        const val MIDI_TIME_CODE_QTR_FRAME = 0xF1.toByte()
        const val SONG_POSITION_POINTER = 0xF2.toByte()
        const val SONG_SELECT = 0xF3.toByte()
        const val UNDEFINED_RESERVED_F4 = 0xF4.toByte()
        const val UNDEFINED_RESERVED_F5 = 0xF5.toByte()
        const val TUNE_REQUEST = 0xF6.toByte()
        const val END_OF_SYSEX = 0xF7.toByte()
        const val TIMING_CLOCK = 0xF8.toByte()
        const val UNDEFINED_RESERVED_F9 = 0xF9.toByte()
        const val START = 0xFA.toByte()
        const val CONTINUE = 0xFB.toByte()
        const val STOP = 0xFC.toByte()
        const val UNDEFINED_RESERVED_FD = 0xFD.toByte()
        const val ACTIVE_SENSING = 0xFE.toByte()
        const val SYSTEM_RESET = 0xFF.toByte()
    }

    object MetaEvents {
        const val SEQUENCE_NUMBER = 0x00.toByte()
        const val TEXT_EVENT = 0x01.toByte()
        const val COPYRIGHT_NOTICE = 0x02.toByte()
        const val SEQUENCE_TRACK_NAME = 0x03.toByte()
        const val INSTRUMENT_NAME = 0x04.toByte()
        const val LYRIC = 0x05.toByte()
        const val MARKER = 0x06.toByte()
        const val CUE_POINT = 0x07.toByte()
        const val MIDI_CHANNEL_PREFIX = 0x20.toByte()
        const val END_OF_TRACK = 0x2F.toByte()
        const val TEMPO = 0x51.toByte()
        const val SMPTE_OFFSET = 0x54.toByte()
        const val TIME_SIGNATURE = 0x58.toByte()
        const val KEY_SIGNATURE = 0x59.toByte()
        const val SEQUENCER_SPECIFIC_EVENT = 0x7F.toByte()
    }

    object Controllers {
        const val DATA_ENTRY_MSB = 0x06.toByte()
        const val DATA_ENTRY_LSB = 0x26.toByte()
        const val RPN_LSB = 0x64.toByte()
        const val RPN_MSB = 0x65.toByte()
    }
}
