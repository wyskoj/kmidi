package org.wysko.kmidi.midi.builder

internal interface Builder<T> {
    fun build(): T
}
