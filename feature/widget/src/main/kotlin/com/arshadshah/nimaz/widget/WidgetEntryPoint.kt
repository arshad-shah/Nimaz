package com.arshadshah.nimaz.widget

import com.arshadshah.nimaz.domain.repository.PrayerRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * How a Glance widget reaches the object graph. A widget is not a `@AndroidEntryPoint`, so it
 * cannot be injected; it asks for what it needs here.
 *
 * This exposed `PrayerDao` until `widget/` became `:feature:widget` in PR 13 of #551, which put a
 * `:core:database` type in a feature module's API. It now exposes the domain repository — the same
 * seam every ViewModel uses — so the widget depends on the contract rather than the schema.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun prayerRepository(): PrayerRepository
}
