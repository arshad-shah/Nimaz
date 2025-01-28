package com.arshadshah.nimaz.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(ViewModelComponent::class)
object ViewModelModule {

    @Provides
    fun provideViewModelScope(
        coroutineScope: CoroutineScope
    ): CoroutineScope = coroutineScope
}
