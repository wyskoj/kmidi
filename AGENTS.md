# AGENTS.md - kmidi Development Guide

## Project Overview
**kmidi** is a published Kotlin multiplatform library for parsing, building, and analyzing Standard MIDI files (SMF). It's a pragmatic implementation that prioritizes correct SMF handling over file robustness.

- **Language**: Kotlin 1.9.24 (multiplatform: JVM primary)
- **Key Dependency**: Apache License 2.0
- **Architecture**: No external runtime dependencies; pure Kotlin
- **Distribution**: Maven Central (via Sonatype Nexus Publishing)

## Core Architecture

### Four-Pillar Component Model
The codebase is divided into clear functional areas:

1. **Reader** (`midi/reader/`): Parse MIDI bytes into `StandardMidiFile` objects
   - Entry point: `StandardMidiFileReader.readByteArray()` / `readStream()`
   - Features **configurable policies** for lenient vs strict parsing
   - Key classes: `StandardMidiFileHeaderReader`, `StandardMidiFileTracksReader`

2. **Builder** (`midi/builder/`): Create MIDI files programmatically
   - DSL-based: `smf { track { ... } }`
   - Extension functions provide time denominators: `Int.quarter`, `Int.eighth`, `Int.sixteenth`
   - Entry point: `smf()` function in `StandardMidiFileBuilder`

3. **Writer** (`midi/StandardMidiFileWriter.kt`): Serialize `StandardMidiFile` to bytes
   - Implements SMF specification 1.0 exactly
   - Handles bit packing and running status

4. **Analysis** (`midi/analysis/`): Extract insights from MIDI data
   - Example: `Polyphony` utility calculates note overlaps
   - Works on `Arc` (NoteOn/NoteOff pairs extracted from raw events)

### Core Data Model
- **StandardMidiFile**: Header + Tracks (immutable data class)
- **Track**: List of Events (immutable; properties: `name`, `notes`, `arcs`)
- **Event hierarchy**:
  - `Event` (base, has `tick`)
  - `MidiEvent` (channel-based: NoteOn/Off, CC, Program, etc.)
  - `MetaEvent` (SetTempo, TimeSignature, KeySignature, etc.)
  - `VirtualEvent` (synthetic: Composite pitch bend, Parameter changes)

### Division & Time Representation
- Two time encoding modes: **MetricalTime** (ticks/quarter note, common) and **TimecodeBasedTime** (SMPTE frame-based)
- All events tracked in absolute ticks (no delta-time storage in memory)
- `StandardMidiFile.tpq` shortcuts to header's ticksPerQuarterNote

## Key Patterns & Conventions

### Documentation is Mandatory
**Detekt enforces javadoc on all public APIs** (excludes test paths):
```kotlin
/** Brief description here. */
public fun myFunction(): Result
```
Private functions need javadoc too. This is non-negotiable; builds fail otherwise.

### Sealed Classes for Exhaustive Type Modeling
Use sealed classes for event types and division types—enables `when` exhaustiveness:
```kotlin
public sealed class Division { 
    data class MetricalTime(...) : Division()
    data class TimecodeBasedTime(...) : Division()
}
```
When adding new event or structural types, **always use sealed** not open classes.

### Data Classes + Init Blocks for Validation
Combine Kotlin data classes with `init` blocks for spec compliance:
```kotlin
public data class Header(...) {
    init {
        require(chunkType == HEADER_MAGIC)
        check(tracks.size == header.trackCount.toInt())
    }
}
```

### Configurable Policies for Pragmatism
`StandardMidiFileReader.Policies` defines lenient/strict modes. When adding file-reading logic:
- Lenient (default): Allow MIDI file variations/bugs
- Strict: Enforce SMF spec exactly
- Add policy flags to `Policies` data class, not magic branches

### Running Status Memory
The SMF spec defines "running status"—a byte optimization where channel messages can omit status bytes. Implementation must:
- Track `runningStatus` across events within a track
- Reset on meta-events and sysex (per spec)
- Allow override via `allowRunningStatusAcrossNonMidiEvents` policy

### VirtualEvents for Computed Data
`VirtualEvent` subclasses represent synthetic data not in the file:
- `VirtualCompositePitchBendEvent`: Merges multiple CC/pitch events
- `VirtualParameterNumberChangeEvent`: Reconstructs NRPN/RPN from CC messages
- **Never include these in file output**; they're analysis-only

## Testing & Build Commands

### Gradle Build (Kotlin Multiplatform)
```bash
# Full build with tests
./gradlew jvmTest

# Just compile (no tests)
./gradlew compileKotlinJvm

# Code quality checks
./gradlew detekt

# Publish to local Maven (for testing before upload)
./gradlew publishToMavenLocal

# Generate Dokka documentation
./gradlew dokka
```

### Test Organization
- **commonTest**: Kotlin tests running on all platforms (use `kotlin.test.*`)
- **jvmTest**: JVM-only tests (can use platform-specific libs)
- Tests are NOT documented (detekt excludes `*Test` dirs)
- Common patterns: `SmfExamples.example1` byte arrays for regression

### Test Files
MIDI test files stored in `src/jvmTest/resources/test_midi/`:
- Used for round-trip testing (read → verify structure)
- Example: `11-radiohead_1997-lucky.mid`

## Event Type Reference
When implementing new features, understand these event base classes:

| Class | Purpose | Examples |
|-------|---------|----------|
| `NoteEvent` | Note on/off pairs | `NoteOn(tick, channel, note, velocity)` |
| `MetaEvent` | Track metadata | `SetTempo`, `TimeSignature`, `KeySignature` |
| `MidiEvent` | Channel messages | `ControlChangeEvent`, `ProgramEvent` |
| `SysexEvent` | System exclusive | Manufacturer-specific raw bytes |
| `VirtualEvent` | Computed synthesis | Used only in analysis, never written |

## Common Implementation Tasks

### Adding New Event Type
1. Extend `MidiEvent` or `MetaEvent` (depending on type)
2. Add to sealed hierarchy in `Event.kt`
3. Update reader in `StandardMidiFileTracksReader` to parse it
4. Update writer in `StandardMidiFileWriter` to serialize it
5. Add detekt-compliant javadoc
6. Add tests covering both read and write round-trips

### Reading MIDI Files Programmatically
```kotlin
val reader = StandardMidiFileReader(Policies.lenient)
val smf = reader.readByteArray(myBytes)
smf.tracks[0].notes  // Get all NoteOn/Off events
smf.tracks[0].arcs   // Get paired start/end notes
```

### Building MIDI Files Programmatically
```kotlin
val midiFile = smf {
    format = Format0
    division = tpq(480)  // 480 ticks per quarter note
    track {
        add(MetaEvent.SetTempo(0, 500000))  // Tempo in microseconds
        add(NoteEvent.NoteOn(0, channel=0, note=60, velocity=100))
        add(NoteEvent.NoteOff(480, channel=0, note=60, velocity=100))
    }
}
StandardMidiFileWriter().writeByteArray(midiFile)
```

### Stream vs Byte Array Reading
- Use `readByteArray()` when you have full file in memory (faster, supports seeking)
- Use `readStream()` for large files or network streams (sequential, memory-efficient)
- Both return identical `StandardMidiFile` objects

## Style & Quality Gates

- **Code format**: IntelliJ Kotlin conventions
- **Max warnings allowed**: None (detekt blocks the build)
- **Magic numbers**: Ignored for -1, 0, 1, 2, 8 (configured in detekt.yml)
- **Suppress suppressions sparingly**: Use `@Suppress("MagicNumber")` only when unavoidable
- **Imports**: Wildcard imports are allowed (detekt disabled)

## Publishing & Deployment
When releasing:
1. Update version in `build.gradle.kts` (semantic versioning)
2. Ensure `detekt` passes
3. All tests pass on JVM
4. Dokka generates without errors
5. `./gradlew publish` uploads to Sonatype (requires GPG/OSSRH credentials via env vars)
- Credentials: `OSSRH_USERNAME`, `OSSRH_PASSWORD`, `OSSRH_GPG_SECRET_KEY_ID`, `OSSRH_GPG_SECRET_KEY`, `OSSRH_GPG_SECRET_KEY_PASSWORD`

