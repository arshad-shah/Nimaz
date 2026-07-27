package com.arshadshah.nimaz.data.local.quran

import com.arshadshah.nimaz.domain.model.quran.catalogue.QuranEditions

/**
 * Where each catalogue edition's bytes actually come from — bundled asset paths, content
 * versions and audio CDN identifiers.
 *
 * This is the data-layer half of the registry. [QuranEditions] holds pure metadata and cannot
 * name an asset path or a CDN id without domain depending on the data layer, so those live
 * here and the two halves are held in step by `QuranEditionRegistryTest`, which fails the
 * build if either side gains an id the other lacks. See ADR-002.
 *
 * ## Why content ships as assets rather than in the prepopulated DB
 * `assets/database/nimaz_prepopulated.db` is a ~147 MB Git-LFS blob that Room copies with
 * `createFromAsset` **only on a fresh install** — it is never re-copied on upgrade.
 * Regenerating it to embed new content would bloat the LFS asset and still not reach existing
 * installs. Seeding from versioned JSON assets makes fresh installs and upgrades converge on
 * the same content for a few MB of compressible JSON.
 */
object QuranContentAssets {

    /**
     * A bundled JSON asset and the content version that gates re-seeding it.
     *
     * Bump [contentVersion] whenever the asset's bytes change so existing installs re-seed on
     * update; `0` means "never seeded" and the first shipped version is `1`.
     */
    data class AssetBinding(
        val assetPath: String,
        val contentVersion: Int
    )

    /**
     * The line-accurate mushaf layouts, keyed by [MushafLayoutEdition.id][com.arshadshah.nimaz.domain.model.quran.catalogue.MushafLayoutEdition.id].
     *
     * Flowed editions (Madani) are paginated by the `ayahs.page` column that ships in the
     * prepopulated DB, so they have no entry here — only editions with `hasLineLayout` do.
     */
    val mushafLayouts: Map<String, LayoutAssets> = mapOf(
        "indopak16" to LayoutAssets(
            layout = AssetBinding("quran/mushaf_layout_indopak16.json", contentVersion = 1),
            ayahText = AssetBinding("quran/ayahs_indopak.json", contentVersion = 1)
        )
    )

    /**
     * The two assets a line-accurate layout needs: the page/line segments, and the ayah text
     * its word positions index into.
     *
     * [ayahText] is `null` when the layout tokenises against an ayah text column the app
     * already ships (today only `text_uthmani`), in which case only the layout is seeded.
     */
    data class LayoutAssets(
        val layout: AssetBinding,
        val ayahText: AssetBinding?
    )

    /** Translations, keyed by translator id. */
    val translations: Map<String, AssetBinding> = mapOf(
        // Sahih International ships inside the prepopulated DB rather than as a seeded asset,
        // so it has no binding yet. Editions added after the registry landed get one here.
    )

    /**
     * Audio CDN binding per reciter: the alquran.cloud edition identifier and the highest
     * bitrate that service offers for them — some reciters are 64 kbps only.
     *
     * Identifiers from https://api.alquran.cloud/v1/edition?format=audio&type=versebyverse
     */
    data class ReciterAudio(
        val cdnId: String,
        val bitrate: Int
    )

    val reciterAudio: Map<String, ReciterAudio> = mapOf(
        "mishary" to ReciterAudio("ar.alafasy", 128),
        "sudais" to ReciterAudio("ar.abdurrahmaansudais", 64),
        "abdulbasit" to ReciterAudio("ar.abdulsamad", 64),
        "maher" to ReciterAudio("ar.mahermuaiqly", 128),
        "minshawi" to ReciterAudio("ar.minshawi", 128),
        "hussary" to ReciterAudio("ar.husary", 128),
        "ajamy" to ReciterAudio("ar.ahmedajamy", 128),
        "shuraim" to ReciterAudio("ar.saoodshuraym", 64),
        "shaatree" to ReciterAudio("ar.shaatree", 128),
        "hudhaify" to ReciterAudio("ar.hudhaify", 128),
        "ayyoub" to ReciterAudio("ar.muhammadayyoub", 128),
        "jibreel" to ReciterAudio("ar.muhammadjibreel", 128),
        "basfar" to ReciterAudio("ar.abdullahbasfar", 64)
    )

    /** The CDN binding for [reciterId], resolved through the catalogue's legacy aliases. */
    fun reciterAudioFor(reciterId: String?): ReciterAudio {
        val edition = QuranEditions.reciter(reciterId)
        return reciterAudio[edition.id]
            ?: reciterAudio.getValue(QuranEditions.defaultReciter.id)
    }
}
