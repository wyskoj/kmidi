/*
 * Copyright © 2023 Jacob Wysko
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

@file:Suppress("Unused")

package org.wysko.kmidi.util

/**
 * Shifts the bits of this byte to the left by [other] places.
 *
 * @receiver The byte to shift the bits of.
 * @param other The number of places to shift the bits.
 * @return The byte with the bits shifted to the left.
 */
internal infix fun Byte.shl(other: Int): Byte = (this.toInt() shl other).toByte()

/**
 * Shifts the bits of this byte to the right by [other] places.
 *
 * @receiver The byte to shift the bits of.
 * @param other The number of places to shift the bits.
 * @return The byte with the bits shifted to the right.
 */
internal infix fun Byte.shr(other: Int): Byte = (this.toInt() shr other).toByte()

/**
 * Shifts the bits of this short to the left by [other] places.
 *
 * @receiver The short to shift the bits of.
 * @param other The number of places to shift the bits.
 * @return The short with the bits shifted to the left.
 */
internal infix fun Short.shl(other: Int): Short = (this.toInt() shl other).toShort()

/**
 * Shifts the bits of this short to the right by [other] places.
 *
 * @receiver The short to shift the bits of.
 * @param other The number of places to shift the bits.
 * @return The short with the bits shifted to the right.
 */
internal infix fun Short.shr(other: Int): Short = (this.toInt() shr other).toShort()