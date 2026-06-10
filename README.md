# kmidi

A pragmatic, Kotlin Multiplatform library for parsing, building, and analyzing Standard MIDI files (SMF).

[![Maven Central](https://img.shields.io/maven-central/v/org.wysko/kmidi?color=blue)](https://central.sonatype.com/artifact/org.wysko/kmidi)
[![Apache License 2.0](https://img.shields.io/badge/license-Apache%202.0-blue)](https://opensource.org/license/apache-2-0/)

## Features

- **Parse MIDI files** – Read Standard MIDI files with configurable parsing modes (lenient vs strict)
- **Build MIDI files** – Create MIDI files programmatically using a Kotlin DSL
- **Zero dependencies** – Pure Kotlin, no external runtime dependencies
- **Multiplatform ready** – Built on Kotlin Multiplatform (JVM primary)
- **Analyze MIDI data** – Extract insights like polyphony, arcs (note on/off pairs), and timing analysis
- **Pragmatic spec compliance** – Handles real-world MIDI files with variations and quirks

## Installation

### Gradle (Kotlin DSL)
```kotlin
dependencies {
    implementation("org.wysko:kmidi:0.2.1")
}
```

### Gradle (Groovy)
```groovy
dependencies {
    implementation 'org.wysko:kmidi:0.2.1'
}
```

### Maven
```xml
<dependency>
    <groupId>org.wysko</groupId>
    <artifactId>kmidi-jvm</artifactId>
    <version>0.2.1</version>
</dependency>
```

## Quick Start

### Reading MIDI Files

```kotlin
import org.wysko.kmidi.midi.reader.StandardMidiFileReader
import java.io.File

// Read from byte array
val bytes = File("song.mid").readBytes()
val midiFile = StandardMidiFileReader().readByteArray(bytes)

// Access tracks and events
midiFile.tracks.forEach { track ->
    println("Track: ${track.name}")
    println("Notes: ${track.notes.size}")
    track.notes.forEach { noteEvent ->
        println("  Note: ${noteEvent.note}, Tick: ${noteEvent.tick}, Channel: ${noteEvent.channel}")
    }
}

// Or read from a stream (memory-efficient for large files)
File("song.mid").inputStream().use { stream ->
    val midiFile = StandardMidiFileReader().readStream(stream)
}
```

### Building MIDI Files

Create MIDI files programmatically with the DSL:

```kotlin
import org.wysko.kmidi.midi.StandardMidiFileWriter
import org.wysko.kmidi.midi.builder.smf
import org.wysko.kmidi.midi.event.MetaEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.kmidi.midi.StandardMidiFile.Header.Format

val midiFile = smf {
    format = Format.Format0
    division = tpq(480)  // 480 ticks per quarter note
    
    track {
        add(MetaEvent.SetTempo(0, 500000))  // 120 BPM
        add(MetaEvent.TimeSignature(0, 4, 2, 24, 8))
        
        // Add notes using time denomination helpers
        add(NoteEvent.NoteOn(0.quarter, channel = 0, note = 60, velocity = 100))
        add(NoteEvent.NoteOff(1.quarter, channel = 0, note = 60, velocity = 0))
        
        add(NoteEvent.NoteOn(1.quarter, channel = 0, note = 64, velocity = 100))
        add(NoteEvent.NoteOff(2.quarter, channel = 0, note = 64, velocity = 0))
    }
}

// Write to file
val bytes = StandardMidiFileWriter().writeByteArray(midiFile)
File("output.mid").writeBytes(bytes)
```

#### Time Denomination Helpers

The builder includes convenient extension functions for note durations:

- `Int.whole` – whole note (4 quarter notes)
- `Int.half` – half note (2 quarter notes)
- `Int.quarter` – quarter note
- `Int.eighth` – eighth note
- `Int.sixteenth` – sixteenth note
- `Int.thirtySecond` – thirty-second note
- `Int.quarterTriplet`, `Int.eighthTriplet`, `Int.sixteenthTriplet`, `Int.thirtySecondTriplet`

```kotlin
// Example: Add a quarter note at tick 480 (1 quarter note from start)
add(NoteEvent.NoteOn(1.quarter, channel = 0, note = 60, velocity = 100))

// Add an eighth note 3 quarter notes in
add(NoteEvent.NoteOn(3.quarter + 1.eighth, channel = 0, note = 64, velocity = 100))
```

### Analyzing MIDI Data

Extract insights from parsed MIDI files:

```kotlin
val midiFile = StandardMidiFileReader().readByteArray(bytes)

// Get note arcs (paired NoteOn/NoteOff events)
midiFile.tracks[0].arcs.forEach { arc ->
    println("Note ${arc.note} plays from tick ${arc.start} to ${arc.end}")
    println("  Duration: ${arc.end - arc.start} ticks")
    println("  Velocity: ${arc.velocity}")
}

// Analyze polyphony (simultaneous notes)
import org.wysko.kmidi.midi.analysis.Polyphony
val polyphonyData = Polyphony.analyze(midiFile)
println("Max simultaneous notes: ${polyphonyData.maxPolyphony}")
```

## Advanced: Lenient vs Strict Parsing

By default, kmidi uses **lenient** parsing to handle real-world MIDI files that may not strictly conform to the SMF specification:

```kotlin
// Lenient parsing (default) – permissive, allows file quirks
val lenientReader = StandardMidiFileReader(Policies.lenient)
val midiFile = lenientReader.readByteArray(bytes)

// Strict parsing – enforces SMF specification exactly
val strictReader = StandardMidiFileReader(Policies.strict)
val midiFile = strictReader.readByteArray(bytes)
```

### Lenient Policy Features

- Allows running status across non-MIDI events
- Tolerates track count discrepancies
- Coerces velocity values to valid range
- Ignores invalid channel prefixes
- Handles incomplete meta-events gracefully
- Recovers from unexpected end-of-file conditions

### Strict Policy Features

- Enforces SMF specification 1.0 exactly
- Rejects any non-conformant data
- Throws exceptions on spec violations

You can also customize policies for specific use cases:

```kotlin
val customPolicies = StandardMidiFileReader.Policies(
    allowRunningStatusAcrossNonMidiEvents = true,
    allowTrackCountDiscrepancy = false,
    coerceVelocityToRange = true,
    ignoreBadChannelPrefixes = true,
    ignoreBadKeySignatures = false,
    ignoreIncompleteMetaEvents = true,
    unexpectedEndOfFilePolicy = AllowDirty
)
val reader = StandardMidiFileReader(customPolicies)
```

## Data Model

### StandardMidiFile

The top-level container for a complete MIDI file:

```kotlin
data class StandardMidiFile(
    val header: Header,
    val tracks: List<Track>,
    val tpq: Short  // ticks per quarter note (shortcut to header.division)
)
```

### Track

A sequence of MIDI events:

```kotlin
data class Track(
    val events: List<Event>
) {
    val name: String?  // First SequenceTrackName meta-event, if present
    val notes: List<NoteEvent>  // All note on/off events
    val arcs: List<Arc>  // Paired note on/off events
}
```

### Event Types

Events represent MIDI messages and metadata:

- **NoteEvent** – `NoteOn`, `NoteOff` (note messages)
- **ControlChangeEvent** – CC messages (e.g., volume, pan)
- **ProgramEvent** – instrument changes
- **PitchWheelChangeEvent** – pitch bend
- **ChaussePressureEvent** – channel pressure
- **PolyphonicKeyPressureEvent** – key pressure
- **SysexEvent** – system exclusive messages
- **MetaEvent** – metadata (tempo, time signature, key signature, text, etc.)

### Arc

Represents a paired NoteOn/NoteOff event:

```kotlin
data class Arc(
    val noteOn: NoteOn,
    val noteOff: NoteOff
) {
    val start: Int  // tick where note starts
    val end: Int    // tick where note ends
    val note: Byte  // MIDI note number (0-127)
    val channel: Byte  // MIDI channel (0-15)
    val velocity: Byte  // note on velocity
}
```

## Building & Contributing

### Requirements

- Kotlin 1.9.24+
- Gradle 8.0+
- JDK 8+

### Build Commands

```bash
# Run all tests
./gradlew jvmTest

# Run code quality checks
./gradlew detekt

# Generate Dokka documentation
./gradlew dokka

# Publish to local Maven repository (for testing)
./gradlew publishToMavenLocal
```

### Project Structure

```
src/
├── commonMain/     # Multiplatform Kotlin code
│   └── kotlin/org/wysko/kmidi/
│       ├── midi/
│       │   ├── reader/           # MIDI file parsing
│       │   ├── builder/          # MIDI file construction DSL
│       │   ├── event/            # Event type definitions
│       │   ├── analysis/         # Analysis utilities
│       │   └── StandardMidiFile*.kt
│       └── stream/               # I/O abstractions
├── commonTest/     # Cross-platform tests
└── jvmTest/        # JVM-specific tests
    └── resources/test_midi/  # Test MIDI files
```

## Specifications

This library implements the **Standard MIDI File Specification 1.0** as defined by the International MIDI Association (IMA). It handles MIDI running status optimization, multiple track formats (0, 1, and 2), and both metrical and timecode-based time divisions.

## License

Licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE.md) file for details.

## Reference

- Standard MIDI File Specification 1.0
- [MIDI Wikipedia Page](https://en.wikipedia.org/wiki/MIDI)
- [kmidi on Maven Central](https://central.sonatype.com/artifact/org.wysko/kmidi)

## Changelog

See [GitHub Releases](https://github.com/wyskoj/kmidi/releases) for version history.

---

**Need help?** [Open an issue on GitHub](https://github.com/wyskoj/kmidi/issues/new).

