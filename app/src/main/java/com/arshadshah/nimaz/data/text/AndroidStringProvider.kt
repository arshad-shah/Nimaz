package com.arshadshah.nimaz.data.text

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.arshadshah.nimaz.core.text.StringProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves strings against the application `Context` — the one place that holds it. */
@Singleton
class AndroidStringProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : StringProvider {

    override fun get(@StringRes id: Int, vararg args: Any): String =
        if (args.isEmpty()) context.getString(id) else context.getString(id, *args)

    override fun quantity(@PluralsRes id: Int, count: Int, vararg args: Any): String =
        context.resources.getQuantityString(id, count, *args)
}
