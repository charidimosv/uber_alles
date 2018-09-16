package com.team.eddie.uber_alles.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.TextUtils
import com.google.gson.Gson
import com.team.eddie.uber_alles.utils.PreferencesUtility.ACTIVE_REQUEST_PREF
import com.team.eddie.uber_alles.utils.PreferencesUtility.LOGGED_IN_EMAIL_PREF
import com.team.eddie.uber_alles.utils.PreferencesUtility.RECEIVER_NAME
import com.team.eddie.uber_alles.utils.PreferencesUtility.SENDER_NAME
import com.team.eddie.uber_alles.utils.PreferencesUtility.USER_INFO
import com.team.eddie.uber_alles.utils.PreferencesUtility.USER_TYPE_PREF


object SaveSharedPreference {

    internal fun getPreferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun cleanAll(context: Context) {
        getPreferences(context).edit().clear().apply()
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

    fun setActiveRequest(context: Context, boolValue: Boolean) {
        setBooleanTypeValue(context, ACTIVE_REQUEST_PREF, boolValue)
    }

    fun getActiveRequest(context: Context): Boolean {
        return getBooleanTypeValue(context, ACTIVE_REQUEST_PREF, false)
    }

    fun setBooleanTypeValue(context: Context, name: String, boolValue: Boolean) {
        getPreferences(context).edit().putBoolean(name, boolValue).apply()
    }

    fun getBooleanTypeValue(context: Context, name: String, defaultValue: Boolean): Boolean {
        return getPreferences(context).getBoolean(name, defaultValue)
    }

    fun setChatSender(context: Context, name: String) {
        getPreferences(context).edit().putString(SENDER_NAME, name).apply()
    }

    fun getChatSender(context: Context): String {
        return getPreferences(context).getString(SENDER_NAME, "")
    }

    fun setChatReceiver(context: Context, name: String) {
        getPreferences(context).edit().putString(RECEIVER_NAME, name).apply()
    }

    fun getChatReceiver(context: Context): String {
        return getPreferences(context).getString(RECEIVER_NAME, "")
    }

    fun setUserInfo(context: Context, info: com.team.eddie.uber_alles.utils.firebase.UserInfo) {
        val jsonInfo = Gson().toJson(info)
        getPreferences(context).edit().putString(USER_INFO, jsonInfo).apply()
    }

    fun getUserInfo(context: Context): com.team.eddie.uber_alles.utils.firebase.UserInfo? {
        val jsonInfo = getPreferences(context).getString(USER_INFO, "")
        return Gson().fromJson(jsonInfo, com.team.eddie.uber_alles.utils.firebase.UserInfo::class.java)
    }
}
