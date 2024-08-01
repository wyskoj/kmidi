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

package org.wysko.kmidi.midi.builder

import org.wysko.kmidi.SmpteTimecode
import org.wysko.kmidi.midi.StandardMidiFile
import org.wysko.kmidi.midi.event.ChannelPressureEvent
import org.wysko.kmidi.midi.event.ControlChangeEvent
import org.wysko.kmidi.midi.event.Event
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.event.PitchWheelChangeEvent
import org.wysko.kmidi.midi.event.PolyphonicKeyPressureEvent
import org.wysko.kmidi.midi.event.ProgramEvent
import org.wysko.kmidi.midi.event.SysexEvent

private const val TEMPO_MAGIC = 60000000
private const val PITCH_BEND_CENTER = 8192.0

/**
 * A builder for a [StandardMidiFile.Track].
 */
@Suppress("TooManyFunctions")
public class TrackBuilder : Builder<StandardMidiFile.Track> {
    private val events = mutableListOf<Event>()
    private var currentChannel: Byte = 0
    private var currentTick = 0

    override fun build(): StandardMidiFile.Track =
        StandardMidiFile.Track((events + MetaEvent.EndOfTrack(events.maxOf { it.tick })))

    /**
     * Adds a single note event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is a
     * note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param note The MIDI note number.
     * @param velocity The velocity of the note.
     * @param duration The duration of the note, in ticks.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     */
    public fun note(
        note: Int,
        duration: Int,
        velocity: Int = 100,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        addNote(Note(note, duration, velocity))
    }

    /**
     * Adds a single note event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is a
     * note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param note A string representing the note (see [Note.get]).
     * @param velocity The velocity of the note.
     * @param duration The duration of the note, in ticks.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     */
    public fun note(
        note: String,
        duration: Int,
        velocity: Int = 100,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        addNote(Note(Note[note], duration, velocity))
    }

    /**
     * Adds a single note event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is a
     * note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param note The [Note] to add.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     */
    public fun note(
        note: Note,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        addNote(note)
    }

    /**
     * Adds a set of note events to the track, all of which occur at the same time (i.e., a chord).
     *
     * - If [deltaTime] is specified, the events will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the events will occur at the specified time.
     * - If neither are specified, the events will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param notes The set of notes to add.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     */
    public fun notes(
        notes: Set<Note>,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        notes.forEach { addNote(it) }
    }

    /**
     * Sets the current channel for the events in [init]. The channel will be reset to its previous value after [init]
     * completes.
     *
     * *Events added outside a `channel` block will default to channel 0.*
     *
     * @param channel The channel to set.
     * @param init The block to execute.
     */
    public fun channel(
        channel: Int,
        init: TrackBuilder.() -> Unit,
    ) {
        val previousChannel = currentChannel
        currentChannel = channel.toByte()
        init()
        currentChannel = previousChannel
    }

    /**
     * Sets the tempo of the sequence.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param beatsPerMinute The tempo, in beats per minute.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.SetTempo
     */
    public fun tempo(
        beatsPerMinute: Number,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.SetTempo(currentTick, (TEMPO_MAGIC / beatsPerMinute.toDouble()).toInt())
    }

    /**
     * Sets the program (instrument) for the channel.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param program The program number.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see ProgramEvent
     */
    public fun program(
        program: Int,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += ProgramEvent(currentTick, currentChannel, program.toByte())
    }

    /**
     * Sets the channel pressure (after-touch) for the channel.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param pressure The pressure value.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see ChannelPressureEvent
     */
    public fun channelPressure(
        pressure: Int,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += ChannelPressureEvent(currentTick, currentChannel, pressure.toByte())
    }

    /**
     * Sets the value of a controller for the channel.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param controller The controller number.
     * @param value The controller value.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see ControlChangeEvent
     */
    public fun controller(
        controller: Int,
        value: Int,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += ControlChangeEvent(currentTick, currentChannel, controller.toByte(), value.toByte())
    }

    /**
     * Sets the pitch bend for the channel.
     *
     * The pitch bend value should be in the range `-1.0..1.0`, where `-1.0` is the lowest pitch, `0.0` is the default
     * (center) pitch, and `1.0` is the highest pitch. The actual bend in semitones is dependent on the current pitch
     * bend sensitivity registered parameter number.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param pitch The pitch bend value.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see PitchWheelChangeEvent
     */
    public fun pitch(
        pitch: Double,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events +=
            PitchWheelChangeEvent(
                currentTick,
                currentChannel,
                ((pitch * PITCH_BEND_CENTER) + PITCH_BEND_CENTER).toInt().toShort(),
            )
    }

    /**
     * Sets the polyphonic key pressure (after-touch) for a note on the channel.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param note The key (note) number.
     * @param pressure The pressure value.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see PolyphonicKeyPressureEvent
     */
    public fun keyPressure(
        note: Int,
        pressure: Int,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += PolyphonicKeyPressureEvent(currentTick, currentChannel, note.toByte(), pressure.toByte())
    }

    /**
     * Adds a system-exclusive (sysex) event to the track.
     *
     * *The data should not include the start (`0xF0`) and end (`0xF7`) bytes.*
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * @param data The sysex data.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see SysexEvent
     */
    public fun sysex(
        data: ByteArray,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += SysexEvent(currentTick, data)
    }

    /**
     * Adds a sequence number meta-event to the track.
     *
     * *This event must occur at the beginning of the track.*
     *
     * @param number The sequence number.
     * @throws IllegalStateException If this invocation occurs at any nonzero delta-time.
     * @see MetaEvent.SequenceNumber
     */
    public fun sequenceNumber(number: Int) {
        check(currentTick == 0) { "Sequence number must occur at the beginning of the track" }
        events += MetaEvent.SequenceNumber(number.toShort())
    }

    /**
     * Adds a copyright notice meta-event to the track.
     *
     * *This event must occur at the beginning of the track.*
     *
     * @param copyrightNotice The copyright notice.
     * @throws IllegalStateException If this invocation occurs at any nonzero delta-time.
     * @see MetaEvent.CopyrightNotice
     */
    public fun copyrightNotice(copyrightNotice: String) {
        check(currentTick == 0) { "Copyright notice must occur at the beginning of the track" }
        events += MetaEvent.CopyrightNotice(copyrightNotice)
    }

    /**
     * Adds an instrument name meta-event to the track.
     *
     * *This event must occur at the beginning of the track.*
     *
     * @param instrumentName The instrument name.
     * @throws IllegalStateException If this invocation occurs at any nonzero delta-time.
     * @see MetaEvent.InstrumentName
     */
    public fun instrumentName(instrumentName: String) {
        check(currentTick == 0) { "Instrument name must occur at the beginning of the track" }
        events += MetaEvent.InstrumentName(instrumentName)
    }

    /**
     * Adds a SMPTE offset meta-event to the track.
     *
     * *This event must occur at the beginning of the track.*
     *
     * @param smpteOffset The offset as a [SmpteTimecode].
     * @throws IllegalStateException If this invocation occurs at any nonzero delta-time.
     * @see MetaEvent.SmpteOffset
     * @see SmpteTimecode
     */
    public fun smpteOffset(smpteOffset: SmpteTimecode) {
        check(currentTick == 0) { "SMPTE offset must occur at the beginning of the track" }
        events += MetaEvent.SmpteOffset(smpteOffset)
    }

    /**
     * Adds a lyric meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param lyric The lyric text.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.Lyric
     */
    public fun lyric(
        lyric: String,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.Lyric(currentTick, lyric)
    }

    /**
     * Adds a marker meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param marker The marker text.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.Marker
     */
    public fun marker(
        marker: String,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.Marker(currentTick, marker)
    }

    /**
     * Adds a cue point meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param cuePoint The cue point text.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.CuePoint
     */
    public fun cuePoint(
        cuePoint: String,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.CuePoint(currentTick, cuePoint)
    }

    /**
     * Adds a channel prefix meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param channel The channel.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.ChannelPrefix
     */
    public fun channelPrefix(
        channel: Int,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.ChannelPrefix(currentTick, channel.toByte())
    }

    /**
     * Adds a text meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param text The text of the event.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.Text
     */
    public fun text(
        text: String,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.Text(currentTick, text)
    }

    /**
     * Adds a time signature meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param numerator The time signature numerator, as notated.
     * @param denominator The time signature denominator as a negative power of two.
     * @param metronome The number of MIDI clocks per metronome click.
     * @param thirtySeconds Optionally, the number of 32nd notes per MIDI quarter note (default is 8).
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.TimeSignature
     */
    @Suppress("LongParameterList")
    public fun timeSignature(
        numerator: Int,
        denominator: Int,
        metronome: Int,
        thirtySeconds: Int? = 8,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events +=
            MetaEvent.TimeSignature(
                currentTick,
                numerator.toByte(),
                denominator.toByte(),
                metronome.toByte(),
                (thirtySeconds ?: 8).toByte(),
            )
    }

    /**
     * Adds a key signature meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param key The [MetaEvent.KeySignature.Key].
     * @param scale The [MetaEvent.KeySignature.Scale].
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.KeySignature
     */
    public fun keySignature(
        key: MetaEvent.KeySignature.Key,
        scale: MetaEvent.KeySignature.Scale,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.KeySignature(currentTick, key, scale)
    }

    /**
     * Adds a sequencer-specific meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param data The sequencer specific data.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.SequencerSpecific
     */
    public fun sequencerSpecific(
        data: ByteArray,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.SequencerSpecific(currentTick, data)
    }

    /**
     * Adds a custom meta-event to the track.
     *
     * - If [deltaTime] is specified, the event will occur [deltaTime] ticks after the previous event (or, if the last
     * event is a note, after its `NoteOff` event).
     * - If [absoluteTime] is specified, the event will occur at the specified time.
     * - If neither are specified, the event will occur immediately after the previous event (or, if the last event is
     * a note, after its `NoteOff` event).
     *
     * *No more than one of [deltaTime] and [absoluteTime] can be specified.*
     *
     * @param type The type of the meta-event.
     * @param data The data of the meta-event.
     * @param deltaTime Optionally, the number of ticks to wait before the event occurs.
     * @param absoluteTime Optionally, the time at which the event should occur.
     * @throws IllegalArgumentException If both [deltaTime] and [absoluteTime] are specified.
     * @see MetaEvent.Unknown
     */
    public fun customMeta(
        type: Int,
        data: ByteArray,
        deltaTime: Int? = null,
        absoluteTime: Int? = null,
    ) {
        adjustTime(deltaTime, absoluteTime)
        events += MetaEvent.Unknown(currentTick, type.toByte(), data)
    }

    private fun addNote(note: Note) {
        events += NoteEvent.NoteOn(currentTick, currentChannel, note.note.toByte(), note.velocity.toByte())
        currentTick += note.duration
        events += NoteEvent.NoteOff(currentTick, currentChannel, note.note.toByte(), 0)
    }

    private fun adjustTime(
        deltaTime: Int?,
        absoluteTime: Int?,
    ) {
        require(!(deltaTime != null && absoluteTime != null)) {
            "Only one of deltaTime and absoluteTime can be specified"
        }
        if ((deltaTime == null).xor(absoluteTime == null)) {
            when {
                deltaTime != null -> currentTick += deltaTime
                absoluteTime != null -> currentTick = absoluteTime
            }
        }
    }
}
