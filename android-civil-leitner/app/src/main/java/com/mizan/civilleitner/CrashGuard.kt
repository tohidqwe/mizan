package com.mizan.civilleitner

import android.content.Context
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

object CrashGuard {
    private const val FILE = "last_crash.txt"

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                context.openFileOutput(FILE, Context.MODE_PRIVATE).bufferedWriter().use { out ->
                    out.appendLine("timestamp=${Instant.now()}")
                    out.appendLine("thread=${thread.name}")
                    out.append(sw.toString())
                }
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun hasPreviousCrash(context: Context): Boolean = context.getFileStreamPath(FILE).exists()

    fun clear(context: Context) {
        runCatching { context.deleteFile(FILE) }
    }
}
