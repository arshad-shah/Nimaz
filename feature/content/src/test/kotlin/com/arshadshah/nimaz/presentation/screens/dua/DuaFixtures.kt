package com.arshadshah.nimaz.presentation.screens.dua

import com.arshadshah.nimaz.domain.model.Dua
import com.arshadshah.nimaz.domain.model.DuaBookmark
import com.arshadshah.nimaz.domain.model.DuaCategory
import com.arshadshah.nimaz.domain.model.DuaOccasion

/**
 * Content the dua screens render.
 *
 * Every optional field the screens branch on — transliteration, benefits, reference, repeat
 * count, occasion, description — is a named parameter, so a test that is about one of them
 * names only that one.
 */
internal fun category(
    id: String = "morning",
    nameEnglish: String = "Morning Adhkar",
    nameArabic: String = "أذكار الصباح",
    description: String? = "Supplications for the morning",
    iconName: String? = "🌅",
    duaCount: Int = 12,
    displayOrder: Int = 1,
) = DuaCategory(
    id = id,
    nameArabic = nameArabic,
    nameEnglish = nameEnglish,
    description = description,
    iconName = iconName,
    displayOrder = displayOrder,
    duaCount = duaCount,
)

internal fun dua(
    id: String = "dua_1",
    categoryId: String = "morning",
    titleEnglish: String = "Upon waking",
    textArabic: String = "الحمد لله الذي أحيانا",
    textTransliteration: String? = "Alhamdulillahilladhi ahyana",
    textEnglish: String = "Praise be to Allah who gave us life",
    reference: String? = "Sahih al-Bukhari 6312",
    occasion: DuaOccasion? = DuaOccasion.WAKING_UP,
    benefits: String? = null,
    repeatCount: Int? = null,
    displayOrder: Int = 1,
) = Dua(
    id = id,
    categoryId = categoryId,
    titleArabic = "دعاء",
    titleEnglish = titleEnglish,
    textArabic = textArabic,
    textTransliteration = textTransliteration,
    textEnglish = textEnglish,
    reference = reference,
    occasion = occasion,
    benefits = benefits,
    repeatCount = repeatCount,
    audioUrl = null,
    displayOrder = displayOrder,
)

internal fun favourite(id: Long, duaId: String = "dua_$id") = DuaBookmark(
    id = id,
    duaId = duaId,
    categoryId = "morning",
    note = null,
    isFavorite = true,
    createdAt = 0L,
    updatedAt = 0L,
)
