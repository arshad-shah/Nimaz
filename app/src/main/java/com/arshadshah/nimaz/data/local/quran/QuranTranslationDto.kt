package com.arshadshah.nimaz.data.local.quran

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val quranTranslationJson: Json = Json {
    ignoreUnknownKeys = true // the asset also carries provenance fields the app doesn't read
    isLenient = true
}

/**
 * One bundled `assets/quran/translations/<id>.json`, as emitted by the importer in the
 * arshad-shah/nimaz-data repository (`upstream/scripts/download_translations.py`).
 *
 * This asset is on the retirement path: the translations now live in the fetched artifact
 * as `tr.*` collections. See docs/DATA_RETIREMENT.md, entry `quran-translation-seeder`.
 *
 * [texts] is a *positional* array, not a map: index `i` holds the translation of global ayah
 * id `i + 1`. That is safe because the app's ayah id space is the canonical 1..6236 mushaf
 * order, and the generator hard-fails if the upstream edition does not align with
 * `ayahs.json` verse for verse. It also keeps the asset roughly a third smaller than the
 * equivalent array of objects — which matters when 15 of them ship in the APK.
 *
 * [contentVersion] is compared against the version last seeded for this translation, so
 * correcting a translation's text later re-seeds it on upgrade.
 */
@Serializable
data class TranslationAssetDto(
    @SerialName("translationId") val translationId: String,
    @SerialName("contentVersion") val contentVersion: Int,
    @SerialName("source") val source: String = "",
    @SerialName("texts") val texts: List<String>
)
