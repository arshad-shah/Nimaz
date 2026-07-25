package com.arshadshah.nimaz.core.util

import android.content.Context
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.domain.model.WorshipReminderType

/**
 * Localized copy for the extended worship reminders — shared by the notification (title/body) and
 * the Home "Next Worship" card (name eyebrow + Arabic name), so the two never drift. Mirrors
 * [NotificationContentHelper]. All strings come from resources (localized ×6). See spec §8.
 *
 * [subKey] disambiguates variants: `monday`/`thursday` for the Mon/Thu fast, `arafah`/`ashura`
 * for that reminder.
 */
object WorshipReminderContent {

    /** Short display name shown as the card eyebrow and used in generic copy. */
    fun name(context: Context, type: WorshipReminderType): String =
        context.getString(nameRes(type))

    /** Arabic name for the card. */
    fun arabic(context: Context, type: WorshipReminderType): String =
        context.getString(arabicRes(type))

    fun title(context: Context, type: WorshipReminderType, subKey: String? = null): String =
        when (type) {
            WorshipReminderType.MONDAY_THURSDAY_FAST -> context.getString(
                if (subKey == "thursday") R.string.worship_mon_thu_title_thursday
                else R.string.worship_mon_thu_title_monday
            )

            WorshipReminderType.ARAFAH_ASHURA_FAST -> context.getString(
                if (subKey == "ashura") R.string.worship_ashura_title
                else R.string.worship_arafah_title
            )

            else -> context.getString(titleRes(type))
        }

    fun body(context: Context, type: WorshipReminderType, subKey: String? = null): String =
        when (type) {
            WorshipReminderType.ARAFAH_ASHURA_FAST -> context.getString(
                if (subKey == "ashura") R.string.worship_ashura_body
                else R.string.worship_arafah_body
            )

            else -> context.getString(bodyRes(type))
        }

    private fun nameRes(type: WorshipReminderType): Int = when (type) {
        WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_name
        WorshipReminderType.WITR -> R.string.worship_witr_name
        WorshipReminderType.SUHOOR -> R.string.worship_suhoor_name
        WorshipReminderType.IFTAR -> R.string.worship_iftar_name
        WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_name
        WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_name
        WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_name
        WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_name
        WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_name
        WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_name
        WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_ashura_name
    }

    private fun arabicRes(type: WorshipReminderType): Int = when (type) {
        WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_arabic
        WorshipReminderType.WITR -> R.string.worship_witr_arabic
        WorshipReminderType.SUHOOR -> R.string.worship_suhoor_arabic
        WorshipReminderType.IFTAR -> R.string.worship_iftar_arabic
        WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_arabic
        WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_arabic
        WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_arabic
        WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_arabic
        WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_arabic
        WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_arabic
        WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_ashura_arabic
    }

    private fun titleRes(type: WorshipReminderType): Int = when (type) {
        WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_title
        WorshipReminderType.WITR -> R.string.worship_witr_title
        WorshipReminderType.SUHOOR -> R.string.worship_suhoor_title
        WorshipReminderType.IFTAR -> R.string.worship_iftar_title
        WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_title
        WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_title
        WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_title
        WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_title
        WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_title
        // handled by title(): Mon/Thu + Arafah/Ashura vary by subKey
        WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_title_monday
        WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_title
    }

    private fun bodyRes(type: WorshipReminderType): Int = when (type) {
        WorshipReminderType.TAHAJJUD -> R.string.worship_tahajjud_body
        WorshipReminderType.WITR -> R.string.worship_witr_body
        WorshipReminderType.SUHOOR -> R.string.worship_suhoor_body
        WorshipReminderType.IFTAR -> R.string.worship_iftar_body
        WorshipReminderType.TARAWEEH -> R.string.worship_taraweeh_body
        WorshipReminderType.LAYLATUL_QADR -> R.string.worship_laylatul_qadr_body
        WorshipReminderType.ADHKAR_MORNING -> R.string.worship_adhkar_morning_body
        WorshipReminderType.ADHKAR_EVENING -> R.string.worship_adhkar_evening_body
        WorshipReminderType.MONDAY_THURSDAY_FAST -> R.string.worship_mon_thu_body
        WorshipReminderType.WHITE_DAYS_FAST -> R.string.worship_white_days_body
        WorshipReminderType.ARAFAH_ASHURA_FAST -> R.string.worship_arafah_body
    }
}
