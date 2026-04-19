package com.colamusic.core.diagnostics

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory ring buffer for diagnostic events. UI in feature:settings reads
 * this for the hidden diagnostics screen. Cleared on process restart.
 */
@Singleton
class EventLog @Inject constructor() {
    private val buffer = ArrayDeque<Event>()
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    @Synchronized
    fun record(level: Level, tag: String, message: String) {
        val evt = Event(System.currentTimeMillis(), level, tag, message)
        buffer.addFirst(evt)
        while (buffer.size > MAX) buffer.removeLast()
        _events.update { buffer.toList() }
    }

    enum class Level { INFO, WARN, ERROR }
    data class Event(val tsMs: Long, val level: Level, val tag: String, val message: String)

    companion object { private const val MAX = 500 }
}

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticsModule
