package com.arshadshah.nimaz.data.sync

import com.arshadshah.nimaz.data.local.database.dao.KhatamDao
import com.arshadshah.nimaz.data.local.user.TafseerUserDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import com.arshadshah.nimaz.data.local.user.BookmarkDao
import io.mockk.mockk

/**
 * A [SyncDataImporter] wired from relaxed mocks.
 *
 * It takes 21 collaborators and each test cares about one or two, so the rest are relaxed
 * mocks and the granular `importXxxData` entry points are called directly — `import` would
 * drag in every table.
 */
internal fun importerWith(
    bookmarkDao: BookmarkDao = mockk(relaxed = true),
    khatamDao: KhatamDao = mockk(relaxed = true),
    tafseerUserDao: TafseerUserDao = mockk(relaxed = true),
    zakatDao: ZakatDao = mockk(relaxed = true),
) = SyncDataImporter(
    database = mockk(relaxed = true),
    quranDao = mockk(relaxed = true),
    prayerDao = mockk(relaxed = true),
    fastingDao = mockk(relaxed = true),
    tasbihDao = mockk(relaxed = true),
    sessionDao = mockk(relaxed = true),
    khatamDao = khatamDao,
    tafseerDao = mockk(relaxed = true),
    tafseerUserDao = tafseerUserDao,
    zakatDao = zakatDao,
    asmaUlHusnaDao = mockk(relaxed = true),
    asmaUnNabiDao = mockk(relaxed = true),
    prophetDao = mockk(relaxed = true),
    hadithDao = mockk(relaxed = true),
    bookmarkDao = bookmarkDao,
    progressDao = mockk(relaxed = true),
    readingProgressDao = mockk(relaxed = true),
    duaDao = mockk(relaxed = true),
    qaidaDao = mockk(relaxed = true),
    locationDao = mockk(relaxed = true),
    preferencesDataStore = mockk(relaxed = true)
)
