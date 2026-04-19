package com.colamusic.core.database

import androidx.room.TypeConverter

class RoomConverters {
    @TypeConverter fun stringListToCsv(v: List<String>?): String? = v?.joinToString("\u001F")
    @TypeConverter fun csvToStringList(v: String?): List<String>? =
        v?.split("\u001F")?.filter { it.isNotEmpty() }
}
