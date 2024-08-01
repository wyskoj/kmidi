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

@file:JvmName("StandardMidiFileReaderJvm")

package org.wysko.kmidi.midi.reader

import org.wysko.kmidi.midi.StandardMidiFile
import java.io.File
import java.io.InputStream

/**
 * Convenience function to read a [File] as a [StandardMidiFile].
 *
 * @param file The file to read.
 * @return The read [StandardMidiFile].
 */
public fun StandardMidiFileReader.readFile(file: File): StandardMidiFile = readByteArray(file.readBytes())

/**
 * Convenience function to read an [InputStream] as a [StandardMidiFile].
 *
 * @param inputStream The input stream to read.
 * @return The read [StandardMidiFile].
 */
public fun StandardMidiFileReader.readInputStream(inputStream: InputStream): StandardMidiFile =
    readByteArray(inputStream.readBytes())
