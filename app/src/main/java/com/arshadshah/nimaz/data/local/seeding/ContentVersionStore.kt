package com.arshadshah.nimaz.data.local.seeding

import com.arshadshah.nimaz.data.local.dua.DuaContentSeeder
import com.arshadshah.nimaz.data.local.help.HelpContentSeeder
import com.arshadshah.nimaz.data.local.qaida.QaidaContentSeeder
import com.arshadshah.nimaz.data.local.quran.QuranLayoutSeeder
import com.arshadshah.nimaz.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keyed store of "which version of this bundled content has been seeded".
 *
 * Replaces the one-interface-per-seeder pattern (`IndopakContentVersionStore`,
 * `DuaContentVersionStore`, …) so shipping a new seeded asset needs no new type and no new
 * DataStore key. Abstracted so seeders stay unit-testable without Android.
 */
interface ContentVersionStore {
    suspend fun get(key: String): Int
    suspend fun set(key: String, version: Int)
}

@Singleton
class DataStoreContentVersionStore @Inject constructor(
    private val settings: SettingsRepository
) : ContentVersionStore {

    /**
     * The version last seeded under [key], falling back **once** to the pre-registry
     * preference that used to hold it.
     *
     * The fallback is the whole point: the keyed preference starts at 0 on every existing
     * install, so without it the first launch after this change would re-seed every bundled
     * asset — ~14k layout rows plus all the Dua, Help and Qaida content — for no reason, on
     * devices that already hold exactly that content. Writes always go to the new key, so the
     * fallback is read-only and stops mattering after the first seed.
     */
    override suspend fun get(key: String): Int {
        val stored = settings.getContentVersion(key).first()
        if (stored > 0) return stored
        return LEGACY_KEYS[key]?.invoke(settings) ?: 0
    }

    override suspend fun set(key: String, version: Int) =
        settings.setContentVersion(key, version)

    private companion object {
        /**
         * New content key → the preference that held its version before the keyed store.
         * Nothing is ever *added* here: a content type shipped after this change starts on the
         * keyed preference and has no legacy value to inherit.
         */
        val LEGACY_KEYS: Map<String, suspend (SettingsRepository) -> Int> = mapOf(
            QuranLayoutSeeder.contentKey("indopak16") to { s -> s.indopakContentVersion.first() },
            DuaContentSeeder.CONTENT_KEY to { s -> s.duaContentVersion.first() },
            HelpContentSeeder.CONTENT_KEY to { s -> s.helpContentVersion.first() },
            QaidaContentSeeder.CONTENT_KEY to { s -> s.qaidaContentVersion.first() }
        )
    }
}
