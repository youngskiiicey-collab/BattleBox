package com.example.ui.auth

import android.content.Context

object AdminLoginPrefs {
    private const val PREFS_NAME = "battlebox_admin_login_prefs"
    private const val KEY_REMEMBER_ID = "remember_admin_id"
    private const val KEY_ADMIN_EMAIL = "saved_admin_email"
    private const val KEY_PREFERRED_PORTAL = "preferred_portal"

    fun isRememberEmailEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_REMEMBER_ID, true)

    fun getRememberedAdminEmail(context: Context): String =
        if (isRememberEmailEnabled(context)) {
            prefs(context).getString(KEY_ADMIN_EMAIL, "").orEmpty()
        } else {
            ""
        }

    fun saveAdminLoginPreference(context: Context, remember: Boolean, email: String) {
        prefs(context).edit().apply {
            putBoolean(KEY_REMEMBER_ID, remember)
            if (remember && email.isNotBlank()) {
                putString(KEY_ADMIN_EMAIL, email.trim())
            } else if (!remember) {
                remove(KEY_ADMIN_EMAIL)
            }
            apply()
        }
    }

    fun getPreferredPortal(context: Context): AuthPortalTab {
        val raw = prefs(context).getString(KEY_PREFERRED_PORTAL, AuthPortalTab.USER_TRAINER.name)
        return runCatching { AuthPortalTab.valueOf(raw ?: AuthPortalTab.USER_TRAINER.name) }
            .getOrDefault(AuthPortalTab.USER_TRAINER)
    }

    fun savePreferredPortal(context: Context, portal: AuthPortalTab) {
        prefs(context).edit().putString(KEY_PREFERRED_PORTAL, portal.name).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
