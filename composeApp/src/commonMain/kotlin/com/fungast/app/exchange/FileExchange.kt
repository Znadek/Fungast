package com.fungast.app.exchange

/** Moves files in and out of the app through the platform's share sheet and file picker. */
interface FileExchange {
    /** Opens the share sheet (WhatsApp, email, Drive, "Save to device"…) with [file] attached. */
    suspend fun share(file: SharedFile)

    /** Lets the user pick a file and returns its text, or null if cancelled or unreadable. */
    suspend fun pickTextFile(): String?
}
