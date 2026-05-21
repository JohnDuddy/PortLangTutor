package com.duddy.portugues.data.preferences

import android.content.Context
import com.duddy.portugues.data.model.PhraseDifficulty

object OnboardingPreferences {
    private const val PREFS_NAME = "duddy_portugues_onboarding"
    private const val KEY_COMPLETED = "has_completed_onboarding"
    private const val KEY_PLACEMENT_LEVEL = "placement_level"
    private const val KEY_DAILY_MINUTES = "daily_minutes"
    private const val KEY_HOME_TOOLTIP_SEEN = "has_seen_home_tooltip"
    private const val KEY_TRIAL_SESSION_USED = "has_used_trial_session"

    fun isCompleted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_COMPLETED, false)

    fun complete(
        context: Context,
        placementLevel: PhraseDifficulty,
        dailyMinutes: Int,
    ) {
        prefs(context).edit()
            .putBoolean(KEY_COMPLETED, true)
            .putString(KEY_PLACEMENT_LEVEL, placementLevel.name)
            .putInt(KEY_DAILY_MINUTES, dailyMinutes)
            .apply()
    }

    fun placementLevel(context: Context): PhraseDifficulty =
        PhraseDifficulty.parse(prefs(context).getString(KEY_PLACEMENT_LEVEL, null))

    fun dailyMinutes(context: Context): Int =
        prefs(context).getInt(KEY_DAILY_MINUTES, 10)

    fun hasSeenHomeTooltip(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HOME_TOOLTIP_SEEN, false)

    fun markHomeTooltipSeen(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_HOME_TOOLTIP_SEEN, true)
            .apply()
    }

    fun hasUsedTrialSession(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TRIAL_SESSION_USED, false)

    fun markTrialSessionUsed(context: Context) {
        prefs(context).edit()
            .putBoolean(KEY_TRIAL_SESSION_USED, true)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
