# Getting started

**kmidi** is a pragmatic Kotlin library for parsing and analyzing MIDI files. It is designed to be simple,
efficient, and easy to use. It is built on top of
the [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) framework.

## Features

- **Parsing** – kmidi can parse MIDI files and extract information such as tracks, events, and metadata.
- **Analysis** – kmidi can analyze MIDI files and provide information such as tempo, time signature, and key signature.

## Installation

To use kmidi in your project, add the following dependency to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("org.wysko:kmidi:0.0.4")
}
```

> The latest version of kmidi can be found on [Maven Central](https://search.maven.org/artifact/org.wysko/kmidi).

## Basic usage

Here's a simple example of how to parse a MIDI file and return the names of each track using kmidi:

```kotlin
val smf = StandardMidiFileReader.readFile(File("path/to/file.mid"))
val names = smf.tracks.map { it.name }
```