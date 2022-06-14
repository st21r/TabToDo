package io.github.bjxytw.tabtodo.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import io.github.bjxytw.tabtodo.R
import android.content.Intent
import android.os.Build
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceManager
import android.widget.Toast
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.db.DBHelper

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference(SettingsActivity.PREF_KEY_NOTIFICATION).setOnPreferenceClickListener {
            val intent = Intent()
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra("android.provider.extra.APP_PACKAGE", context?.packageName)
            } else {
                intent.putExtra("app_package", context?.packageName)
                intent.putExtra("app_uid", context?.applicationInfo?.uid)
            }

            startActivity(intent)
            return@setOnPreferenceClickListener true
        }

        findPreference(SettingsActivity.PREF_KEY_NOTIFICATION_TIMING).setOnPreferenceChangeListener { _, value ->
            context?.let {
                val dbHelper = DBHelper(it.applicationContext)
                MainActivity.resetAllNotificationAlarms(it, dbHelper, value.toString())
            }
            return@setOnPreferenceChangeListener true
        }
    }
}

