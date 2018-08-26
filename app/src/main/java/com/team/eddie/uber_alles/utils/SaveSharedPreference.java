package com.team.eddie.uber_alles.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import static com.team.eddie.uber_alles.utils.PreferencesUtility.LOGGED_IN_EMAIL_PREF;

public class SaveSharedPreference {

    static SharedPreferences getPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setLoggedIn(Context context, String loggedUser) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putString(LOGGED_IN_EMAIL_PREF, loggedUser);
        editor.apply();
    }

    public static boolean isLoggedIn(Context context) {
        return !TextUtils.isEmpty(getPreferences(context).getString(LOGGED_IN_EMAIL_PREF, ""));
    }
}
