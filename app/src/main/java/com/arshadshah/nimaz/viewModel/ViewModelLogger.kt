package com.arshadshah.nimaz.viewModel

import android.util.Log

object ViewModelLogger {
    private const val MAX_LOG_LENGTH = 4000

    fun d(tag: String, message: String, showThread: Boolean = true) {
        val threadInfo = if (showThread) "[Thread: ${Thread.currentThread().name}] " else ""
        val fullMessage = "$threadInfo$message"

        if (fullMessage.length > MAX_LOG_LENGTH) {
            val chunkCount = fullMessage.length / MAX_LOG_LENGTH
            for (i in 0..chunkCount) {
                val max = (MAX_LOG_LENGTH * (i + 1))
                if (max >= fullMessage.length) {
                    Log.d(
                        tag,
                        "CHUNK ${i + 1} of ${chunkCount + 1}: ${fullMessage.substring(MAX_LOG_LENGTH * i)}"
                    )
                } else {
                    Log.d(
                        tag,
                        "CHUNK ${i + 1} of ${chunkCount + 1}: ${
                            fullMessage.substring(
                                MAX_LOG_LENGTH * i,
                                max
                            )
                        }"
                    )
                }
            }
        } else {
            Log.d(tag, fullMessage)
        }
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun v(tag: String, message: String) {
        Log.v(tag, message)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

}