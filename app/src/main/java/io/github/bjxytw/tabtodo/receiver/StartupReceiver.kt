package io.github.bjxytw.tabtodo.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.db.DBHelper

class StartupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val dbHelper = DBHelper(context)
        MainActivity.resetAllNotificationAlarms(context, dbHelper)
        dbHelper.close()
    }

}