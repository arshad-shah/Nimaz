package com.arshadshah.nimaz.data.local.user

import android.content.Context
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs [LegacyUserDataImport] once, at a moment when it is safe to write.
 *
 * It used to run from the user database's `onOpen` callback, which is the obvious place and
 * the wrong one: Room installs its invalidation tracker *after* the callback returns, so an
 * INSERT there fires triggers against `room_table_modification_log` before that table exists.
 * On a device that threw on every single launch — twenty instrumented tests, including the
 * ones that only check the app starts.
 *
 * So it runs from `AppInitializer` instead, by which time Room has finished opening the
 * database. Idempotent, so running it on a launch where it has already happened costs two
 * counts; and it never throws — a person whose data cannot be copied should still get an app,
 * with the original rows still sitting untouched in the old file for a later attempt.
 *
 * What it does *not* do any more is hand Room's own connection to the copy. That looked like
 * the frugal thing — the database is already open, why open another? — and it brought the same
 * crash back through a different door, because the copy's `ATTACH` makes the framework swap
 * the connection out from under Room and take the tracker's temporary tables with it. The
 * copy owns its connection now; see [LegacyUserDataImport].
 */
@Singleton
class UserDataMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userDatabase: NimazUserDatabase,
) {

    suspend fun migrateIfNeeded(): Int {
        val legacy = File(context.getDatabasePath(NimazDatabase.DATABASE_NAME).absolutePath)
        if (!legacy.exists()) return 0
        return try {
            // Touched only to make Room create and migrate the schema — the copy needs the
            // tables to exist before it can write to them. Nothing is read or written here.
            userDatabase.openHelper.writableDatabase
            val user = context.getDatabasePath(NimazUserDatabase.DATABASE_NAME)
            LegacyUserDataImport.run(user.absolutePath, legacy.absolutePath)
        } catch (e: Exception) {
            // Reported, not rethrown, and nothing is left half-written: the copy is one
            // transaction. The legacy rows are untouched either way.
            CrashReporter.recordException(e)
            0
        }
    }
}
