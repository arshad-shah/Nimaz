package com.arshadshah.nimaz.data.local.help

import kotlinx.serialization.json.Json

/**
 * The lenient reader for the small amount of JSON stored *inside* help rows.
 *
 * This file used to also declare the shape of `assets/help/help.json` — the bundled asset
 * `HelpContentSeeder` parsed. Both went at versionCode 385 (`docs/retirement.yaml`) when the
 * artifact started carrying the help content whole. What survives is the parser itself, which
 * [com.arshadshah.nimaz.data.repository.HelpRepositoryImpl] still needs for the `pathLabels`
 * column: a JSON string array persisted in the database rather than modelled as a table.
 */
val helpJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
