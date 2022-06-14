package io.github.bjxytw.tabtodo.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.data.TaskData
import io.github.bjxytw.tabtodo.db.DBHelper
import java.util.*

class NotificationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(INTENT_TASK_ID, 0)
        val dbHelper = DBHelper(context.applicationContext)

        dbHelper.getTaskFromId(id)?.let {
            setNotification(context, it, dbHelper.getTabPositionFromId(it.tabId))
        }
        dbHelper.close()
    }

    private fun setNotification(context: Context, taskData: TaskData, tabPosition: Int) {

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.INTENT_TAB_DEFAULT_POSITION, tabPosition)
        }


        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val checkIntent = Intent(context, CheckReceiver::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(CheckReceiver.INTENT_CHECK_TASK_ID, taskData.dataId)
        }

        val checkPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(context, 0, checkIntent, PendingIntent.FLAG_CANCEL_CURRENT)

        val builder = NotificationCompat.Builder(context, MainActivity.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check_circle_24dp)
            .setContentTitle(taskData.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_check_circle_24dp, "完了", checkPendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) { notify(taskData.dataId, builder.build()) }
    }

    class CheckReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getIntExtra(INTENT_CHECK_TASK_ID, -1)
            with(NotificationManagerCompat.from(context)) { cancel(id) }

            val dbHelper = DBHelper(context.applicationContext)
            dbHelper.getTaskFromId(id)?.let {

                dbHelper.checkTask(id, it.tabId)
                if (it.schedule.repeat != TaskData.Repeat.None) {
                    repeatTask(context, dbHelper, it)
                }
            }
            dbHelper.close()
            MainActivity.UpdateReceiver.sendBroadcast(context.applicationContext)
        }

        private fun repeatTask(context: Context, dbHelper: DBHelper, taskData: TaskData) {
            dbHelper.deleteTask(taskData)

            val repeatCalender = Calendar.getInstance().apply {
                clear()
                timeInMillis = taskData.schedule.timeInMillis
            }

            do {
                when (taskData.schedule.repeat) {
                    TaskData.Repeat.None -> {}
                    TaskData.Repeat.EveryDay -> repeatCalender.add(Calendar.DAY_OF_MONTH, 1)
                    TaskData.Repeat.EveryWeek -> repeatCalender.add(Calendar.WEEK_OF_MONTH, 1)
                    TaskData.Repeat.EveryOtherWeek -> repeatCalender.add(Calendar.WEEK_OF_MONTH, 2)
                    TaskData.Repeat.EveryMonth -> repeatCalender.add(Calendar.MONTH, 1)
                    TaskData.Repeat.EveryOtherMonth -> repeatCalender.add(Calendar.MONTH, 2)
                    TaskData.Repeat.EveryYear -> repeatCalender.add(Calendar.YEAR, 1)
                }
            } while (repeatCalender.timeInMillis <= System.currentTimeMillis())

            val newData = dbHelper.insertTask(taskData.title, TaskData.Schedule(repeatCalender.timeInMillis,
                taskData.schedule.calenderState, taskData.schedule.repeat), taskData.tabId,
                PreferenceManager.getDefaultSharedPreferences(context).getBoolean("add_to_top", true))

            MainActivity.setNotificationAlarm(context, newData)
        }

        companion object {
            const val INTENT_CHECK_TASK_ID = "check_task_id"
        }
    }

    companion object {
        const val INTENT_TASK_ID = "task_id"
    }
}