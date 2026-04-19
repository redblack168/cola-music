package com.colamusic.core.model

enum class DownloadStatus(val displayName: String) {
    Queued("排队中"),
    Downloading("下载中"),
    Completed("已下载"),
    Failed("失败"),
    Paused("已暂停"),
}

/** Snapshot for UI. [progressPct] is 0..100; [byteSize] is the final file size for complete entries, estimated during progress. */
data class DownloadedSong(
    val song: Song,
    val status: DownloadStatus,
    val progressPct: Int,
    val byteSize: Long,
    val relativePath: String?,
    val errorMessage: String?,
    val updatedAtMs: Long,
    val completedAtMs: Long?,
)
