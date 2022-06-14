package io.github.bjxytw.tabtodo.settings

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import io.github.bjxytw.tabtodo.R
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setSupportActionBar(settings_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.content_settings, SettingsFragment())
            .commit()


    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return true
    }

    companion object {
        const val PREF_KEY_NOTIFICATION = "notification"
        const val PREF_KEY_NOTIFICATION_TIMING = "notification_timing"
        const val PREF_KEY_INSERT_TOP = "insert_top"

        const val VALUE_TIMING_DEFAULT = "default"
        const val VALUE_TIMING_5_MINUTE_AGO = "5_minute_ago"
        const val VALUE_TIMING_15_MINUTE_AGO = "15_minute_ago"
        const val VALUE_TIMING_30_MINUTE_AGO = "30_minute_ago"
        const val VALUE_TIMING_1_HOUR_AGO = "1_hour_ago"
    }
}