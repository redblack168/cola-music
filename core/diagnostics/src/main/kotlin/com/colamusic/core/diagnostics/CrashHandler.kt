package com.colamusic.core.diagnostics

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uncaught-exception handler that persists every crash to
 * [context.filesDir]/crashes/crash_YYYYMMDD_HHmmss.txt.
 *
 * Register once in Application.onCreate():
 *     Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this, Thread.getDefaultUncaughtExceptionHandler()))
 *
 * The previous handler is always chained so Android's own logcat crash line and
 * process teardown still happen.
 */
class CrashHandler(
    context: Context,
    private val fallback: Thread.UncaughtExceptionHandler?,
    private val versionName: String,
    private val versionCode: Int,
) : Thread.UncaughtExceptionHandler {

    private val appContext = context.applicationContext
    private val dir: File = File(appContext.filesDir, "crashes").apply { mkdirs() }

    override fun uncaughtException(t: Thread, e: Throwable) {
        runCatching { writeCrashFile(t, e) }
        fallback?.uncaughtException(t, e)
    }

    private fun writeCrashFile(thread: Thread, e: Throwable) {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val file = File(dir, "crash_$stamp.txt")
        file.bufferedWriter().use { w ->
            w.write("Cola Music ${versionName} (${versionCode})\n")
            w.write("Time: ${Date()}\n")
            w.write("Thread: ${thread.name}\n")
            w.write("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
            w.write("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            w.write("\n======== stack trace ========\n")
            val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
            w.write(sw.toString())
            w.write("\n======== event log (ring buffer) ========\n")
            // Best-effort: dump our own log tags via logcat pipe.
            runCatching {
                val proc = ProcessBuilder("logcat", "-d", "-t", "400", "cola:V", "AndroidRuntime:E", "*:S")
                    .redirectErrorStream(true)
                    .start()
                proc.inputStream.bufferedReader().use { r ->
                    val buf = CharArray(8 * 1024)
                    while (true) {
                        val n = r.read(buf); if (n <= 0) break
                        w.write(buf, 0, n)
                    }
                }
                proc.waitFor()
            }
        }
    }

    companion object {
        fun listCrashes(context: Context): List<File> =
            File(context.filesDir, "crashes").listFiles()
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()

        fun latestCrashText(context: Context): String? =
            listCrashes(context).firstOrNull()?.let { runCatching { it.readText() }.getOrNull() }

        fun logcatSnapshot(): String = runCatching {
            val proc = ProcessBuilder("logcat", "-d", "-t", "400")
                .redirectErrorStream(true)
                .start()
            val out = proc.inputStream.bufferedReader().readText()
            proc.waitFor()
            out
        }.getOrElse { "logcat unavailable: ${it.message}" }
    }
}
