package com.team.eddie.uber_alles.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils

import com.team.eddie.uber_alles.utils.PreferencesUtility.LOGGED_IN_EMAIL_PREF
import com.team.eddie.uber_alles.utils.PreferencesUtility.USER_TYPE_PREF

object SaveSharedPreference {

    internal fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun cleanAll(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }

    fun setLoggedIn(context: Context, loggedUser: String) {
        val editor = getPreferences(context).edit()
        editor.putString(LOGGED_IN_EMAIL_PREF, loggedUser)
        editor.apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return !TextUtils.isEmpty(getPreferences(context).getString(LOGGED_IN_EMAIL_PREF, ""))
    }

    fun setUserType(context: Context, isDriver: Boolean) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(USER_TYPE_PREF, isDriver)
        editor.apply()
    }

    fun isDriver(context: Context): Boolean {
        return getPreferences(context).getBoolean(USER_TYPE_PREF, false)
    }


}
