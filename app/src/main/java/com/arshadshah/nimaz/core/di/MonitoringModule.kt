package com.arshadshah.nimaz.core.di

import com.arshadshah.nimaz.data.device.AndroidCounterFeedback
import com.arshadshah.nimaz.domain.repository.CounterFeedback
import com.arshadshah.nimaz.core.monitoring.FirebaseTelemetry
import com.arshadshah.nimaz.core.monitoring.Telemetry
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the monitoring and device-feedback seams. `@Binds` for interface→impl per the DI convention in
 * `docs/ARCHITECTURE.md` §5.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class MonitoringModule {

    @Binds
    @Singleton
    abstract fun bindTelemetry(impl: FirebaseTelemetry): Telemetry

    @Binds
    @Singleton
    abstract fun bindCounterFeedback(impl: AndroidCounterFeedback): CounterFeedback
}
