package io.github.bjxytw.tabtodo.db

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.text.TextUtils
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.receiver.NotificationAlarmReceiver
import io.github.bjxytw.tabtodo.data.TabData
import io.github.bjxytw.tabtodo.data.TaskData
import java.util.*

class DBHelper(private val context: Context) {
    private val helper = SQLiteHelper(context)
    private val db = helper.writableDatabase

    fun close() { helper.close() }

    fun loadAll() : Pair<ArrayList<TabData>, ArrayList<ArrayList<TaskData>>> {
        val allTaskList = ArrayList<ArrayList<TaskData>>()
        val tabList = loadAllTabs()

        if (tabList.isEmpty()) {
            val initTabName = "New Tab"
            val values = ContentValues().apply {
                put(DBContract.TabEntry.NAME, initTabName)
                put(DBContract.TabEntry.SORT, TabData.Sort.Default.num)
                put(DBContract.TabEntry.POSITION, 0)
            }
            val id = db.insert(DBContract.TabEntry.TABLE_NAME, null, values).toInt()
            tabList.add(TabData(id, initTabName, TabData.Sort.Default, TabData.DataState.Default))
        }

        for (i in 0 until tabList.size) {
            allTaskList.add(ArrayList())
        }

        for ((tabPosition, tab) in tabList.withIndex()) {
            allTaskList[tabPosition] = loadTasksInTab(tab.dataId, tab.sort)
        }

        return tabList to allTaskList
    }

    fun loadTasksInTab(tabId: Int, sort: TabData.Sort) : ArrayList<TaskData> {
        val taskList = ArrayList<TaskData>()

        val order = when (sort) {
            TabData.Sort.Default -> DBContract.TaskEntry.POSITION
            TabData.Sort.Date -> DBContract.TaskEntry.SCHEDULE_TIME
        }

        val cursor = db.rawQuery("SELECT * FROM ${DBContract.TaskEntry.TABLE_NAME}" +
                " WHERE ${DBContract.TaskEntry.TAB_ID}=$tabId" +
                " AND ${DBContract.TaskEntry.CHECKED}=$DATA_NOT_CHECKED" +
                " ORDER BY $order", null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(BaseColumns._ID))
                val title = it.getString(it.getColumnIndex(DBContract.TaskEntry.TITLE))
                val taskTabId = it.getInt(it.getColumnIndex(DBContract.TaskEntry.TAB_ID))
                val schedule = TaskData.Schedule(
                    it.getLong(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_TIME)) ,
                    it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_STATE)),
                    TaskData.Repeat.fromInt(it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_REPEAT))))

                val taskData = TaskData(id, title, schedule, tabId)

                if (tabId == taskTabId) {
                    taskList.add(taskData)
                }
            }
        }

        return taskList
    }

    fun loadAllTabs() : ArrayList<TabData> {
        val tabList = ArrayList<TabData>()
        val cursor = db.rawQuery("SELECT * FROM ${DBContract.TabEntry.TABLE_NAME}" +
                " ORDER BY ${DBContract.TabEntry.POSITION}", null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(BaseColumns._ID))
                val name = it.getString(it.getColumnIndex(DBContract.TabEntry.NAME))
                val sort = it.getInt(it.getColumnIndex(DBContract.TabEntry.SORT))
                tabList.add(TabData(id, name, TabData.Sort.fromInt(sort), TabData.DataState.Default))
            }
        }

        return tabList
    }

    fun saveAllTabs(tabList : ArrayList<TabData>, deletedTabIdList : ArrayList<Int>) {
        for ((index, data) in tabList.withIndex()) {

            val values = ContentValues().apply { put(DBContract.TabEntry.POSITION, index) }

            when (data.dataState) {
                TabData.DataState.Created -> {
                    values.apply {
                        put(DBContract.TabEntry.NAME, data.name)
                        put(DBContract.TabEntry.SORT, data.sort.num)
                    }
                    db.insert(DBContract.TabEntry.TABLE_NAME, null, values)
                }

                TabData.DataState.Edited -> {
                    values.put(DBContract.TabEntry.NAME, data.name)
                    db.update(DBContract.TabEntry.TABLE_NAME, values,
                        "${BaseColumns._ID}=${data.dataId}", null)
                }

                TabData.DataState.Default -> {
                    db.update(DBContract.TabEntry.TABLE_NAME, values,
                        "${BaseColumns._ID}=${data.dataId}", null)
                }
            }
        }

        for (tabId in deletedTabIdList) {

            for (task in loadTasksInTab(tabId, TabData.Sort.Default)) {
                MainActivity.cancelNotificationAlarm(context, task)
            }

            db.delete(DBContract.TabEntry.TABLE_NAME, "${BaseColumns._ID}=$tabId", null)
            db.delete(DBContract.TaskEntry.TABLE_NAME, "${DBContract.TaskEntry.TAB_ID}=$tabId", null)
        }
    }


    fun updateTabSort(data: TabData) {
        val values = ContentValues().apply {
            put(DBContract.TabEntry.SORT, data.sort.num)
        }

        db.update(DBContract.TabEntry.TABLE_NAME, values,
            "${BaseColumns._ID}=${data.dataId}", null)
    }

    fun insertTask(title: String, schedule: TaskData.Schedule, tabId: Int, toFirst: Boolean) : TaskData {
        if (toFirst) {
            db.execSQL("UPDATE ${DBContract.TaskEntry.TABLE_NAME}" +
                        " SET ${DBContract.TaskEntry.POSITION}=${DBContract.TaskEntry.POSITION}+1" +
                        " WHERE ${DBContract.TaskEntry.TAB_ID}=$tabId")
        }

        val insertPosition = if (toFirst) 0 else {
            DatabaseUtils.queryNumEntries(db, DBContract.TaskEntry.TABLE_NAME,
                "${DBContract.TaskEntry.TAB_ID}=$tabId")
        }

        val values = ContentValues().apply {
            put(DBContract.TaskEntry.TITLE, title)
            put(DBContract.TaskEntry.CHECKED, DATA_NOT_CHECKED)
            put(DBContract.TaskEntry.SCHEDULE_STATE, schedule.calenderState)
            put(DBContract.TaskEntry.SCHEDULE_TIME, schedule.timeInMillis)
            put(DBContract.TaskEntry.SCHEDULE_REPEAT, schedule.repeat.num)
            put(DBContract.TaskEntry.COLOR, 0)
            put(DBContract.TaskEntry.TAB_ID, tabId)
            put(DBContract.TaskEntry.POSITION, insertPosition)
        }

        val id = db.insert(DBContract.TaskEntry.TABLE_NAME, null, values).toInt()
        return TaskData(id, title, schedule, tabId)
    }

    fun updateTask(data: TaskData) {
        val values = ContentValues().apply {
            put(DBContract.TaskEntry.TITLE, data.title)
            put(DBContract.TaskEntry.SCHEDULE_STATE, data.schedule.calenderState)
            put(DBContract.TaskEntry.SCHEDULE_TIME, data.schedule.timeInMillis)
            put(DBContract.TaskEntry.SCHEDULE_REPEAT, data.schedule.repeat.num)
        }

        db.update(DBContract.TaskEntry.TABLE_NAME, values,
            "${BaseColumns._ID}=${data.dataId}" +
                    " AND ${DBContract.TaskEntry.TAB_ID}=${data.tabId}", null)
    }

    fun checkTask(id: Int, tabId: Int) {
        val values = ContentValues().apply {
            put(DBContract.TaskEntry.CHECKED, DATA_CHECKED)
            put(DBContract.TaskEntry.POSITION, 0)
        }

        db.update(DBContract.TaskEntry.TABLE_NAME, values,
            "${BaseColumns._ID}=$id" +
                    " AND ${DBContract.TaskEntry.TAB_ID}=$tabId", null)
    }

    fun unCheckTask(id: Int, tabId: Int, toFirst: Boolean) {
        if (toFirst) {
            db.execSQL("UPDATE ${DBContract.TaskEntry.TABLE_NAME}" +
                    " SET ${DBContract.TaskEntry.POSITION}=${DBContract.TaskEntry.POSITION}+1" +
                    " WHERE ${DBContract.TaskEntry.TAB_ID}=$tabId")
        }

        val insertPosition = if (toFirst) 0 else {
            DatabaseUtils.queryNumEntries(db, DBContract.TaskEntry.TABLE_NAME,
                "${DBContract.TaskEntry.TAB_ID}=$tabId")
        }
        
        val values = ContentValues().apply {
            put(DBContract.TaskEntry.CHECKED, DATA_NOT_CHECKED)
            put(DBContract.TaskEntry.POSITION, insertPosition)
        }

        db.update(DBContract.TaskEntry.TABLE_NAME, values,
            "${BaseColumns._ID}=$id" +
                    " AND ${DBContract.TaskEntry.TAB_ID}=$tabId", null)
    }

    fun updateMultipleTasksPosition(idList: ArrayList<Int>, tabId: Int) {
        for ((index, id) in idList.withIndex()) {
            db.update(DBContract.TaskEntry.TABLE_NAME, ContentValues().apply { put(DBContract.TaskEntry.POSITION, index) },
                "${BaseColumns._ID}=$id" +
                        " AND ${DBContract.TaskEntry.TAB_ID}=$tabId", null)
        }
    }

    fun deleteTask(data: TaskData) {
        db.delete(DBContract.TaskEntry.TABLE_NAME,
            "${BaseColumns._ID}=${data.dataId}" +
                    " AND ${DBContract.TaskEntry.TAB_ID}=${data.tabId}", null)
    }

    fun deleteCheckedTask(tabId: Int) {
        db.delete(DBContract.TaskEntry.TABLE_NAME,
            "${DBContract.TaskEntry.CHECKED}=$DATA_CHECKED" +
                    " AND ${DBContract.TaskEntry.TAB_ID}=$tabId", null)
    }

    fun getAllScheduledTasks() : ArrayList<TaskData> {
        val taskList = ArrayList<TaskData>()

        val sql = "SELECT * FROM ${DBContract.TaskEntry.TABLE_NAME}" +
                " WHERE ${DBContract.TaskEntry.CHECKED}=$DATA_NOT_CHECKED" +
                " AND ${DBContract.TaskEntry.SCHEDULE_STATE}=${TaskData.Schedule.TIME_CONFIGURED}"

        val cursor = db.rawQuery(sql, null)
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(BaseColumns._ID))
                val title = it.getString(it.getColumnIndex(DBContract.TaskEntry.TITLE))
                val tabId = it.getInt(it.getColumnIndex(DBContract.TaskEntry.TAB_ID))
                val schedule = TaskData.Schedule(
                    it.getLong(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_TIME)),
                    it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_STATE)),
                    TaskData.Repeat.fromInt(it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_REPEAT))))

                val taskData = TaskData(id, title, schedule, tabId)

                taskList.add(taskData)
            }
        }

        return taskList
    }

    fun getCheckedTasksInTab(tabId: Int) : ArrayList<TaskData> {
        val taskList = ArrayList<TaskData>()

        val cursor = db.rawQuery("SELECT * FROM ${DBContract.TaskEntry.TABLE_NAME}" +
                " WHERE ${DBContract.TaskEntry.CHECKED}=$DATA_CHECKED" +
                " AND ${DBContract.TaskEntry.TAB_ID}=$tabId" +
                " ORDER BY ${BaseColumns._ID} DESC", null)

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getInt(it.getColumnIndex(BaseColumns._ID))
                val title = it.getString(it.getColumnIndex(DBContract.TaskEntry.TITLE))
                val taskTabId = it.getInt(it.getColumnIndex(DBContract.TaskEntry.TAB_ID))
                val schedule = TaskData.Schedule(
                    it.getLong(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_TIME)),
                    it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_STATE)),
                    TaskData.Repeat.fromInt(it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_REPEAT))))

                val taskData = TaskData(id, title, schedule, taskTabId)

                taskList.add(taskData)
            }
        }

        return taskList
    }

    fun getTaskFromId(id: Int) : TaskData? {
        var taskData: TaskData? = null
        val cursor = db.rawQuery("SELECT * FROM ${DBContract.TaskEntry.TABLE_NAME}" +
                " WHERE ${BaseColumns._ID}=$id", null)
        cursor.use {
            it.moveToNext()
            val title = it.getString(it.getColumnIndex(DBContract.TaskEntry.TITLE))
            val tabId = it.getInt(it.getColumnIndex(DBContract.TaskEntry.TAB_ID))
            val schedule = TaskData.Schedule(
                it.getLong(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_TIME)),
                it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_STATE)),
                TaskData.Repeat.fromInt(it.getInt(it.getColumnIndex(DBContract.TaskEntry.SCHEDULE_REPEAT))))
            taskData = TaskData(id, title, schedule, tabId)
        }

        return taskData
    }

    fun getTabPositionFromId(tabId: Int) : Int {
        val cursor = db.rawQuery("SELECT * FROM ${DBContract.TabEntry.TABLE_NAME}" +
                " WHERE ${BaseColumns._ID}=$tabId", null)
        var tabPosition = 0

        cursor.use {
            it.moveToNext()
            tabPosition = it.getInt(it.getColumnIndex(DBContract.TabEntry.POSITION))
        }

        return tabPosition
    }

    class SQLiteHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(SQL_CREATE_TASK_ENTRIES)
            db.execSQL(SQL_CREATE_TAB_ENTRIES)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(SQL_DELETE_TASK_ENTRIES)
            db.execSQL(SQL_DELETE_TAB_ENTRIES)
            onCreate(db)
        }

        override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            onUpgrade(db, oldVersion, newVersion)
        }

        companion object {
            private const val DATABASE_VERSION = 1
            private const val DATABASE_NAME = "TaskData.db"

            private const val SQL_CREATE_TASK_ENTRIES =
                "CREATE TABLE ${DBContract.TaskEntry.TABLE_NAME} (" +
                        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                        "${DBContract.TaskEntry.TITLE} TEXT," +
                        "${DBContract.TaskEntry.CHECKED} INTEGER," +
                        "${DBContract.TaskEntry.SCHEDULE_STATE} INTEGER," +
                        "${DBContract.TaskEntry.SCHEDULE_TIME} INTEGER," +
                        "${DBContract.TaskEntry.SCHEDULE_REPEAT} INTEGER," +
                        "${DBContract.TaskEntry.COLOR} INTEGER," +
                        "${DBContract.TaskEntry.TAB_ID} INTEGER," +
                        "${DBContract.TaskEntry.POSITION} INTEGER)"

            private const val SQL_CREATE_TAB_ENTRIES =
                "CREATE TABLE ${DBContract.TabEntry.TABLE_NAME} (" +
                        "${BaseColumns._ID} INTEGER PRIMARY KEY," +
                        "${DBContract.TabEntry.NAME} TEXT," +
                        "${DBContract.TabEntry.SORT} INTEGER," +
                        "${DBContract.TabEntry.POSITION} INTEGER)"

            private const val SQL_DELETE_TASK_ENTRIES = "DROP TABLE IF EXISTS ${DBContract.TaskEntry.TABLE_NAME}"
            private const val SQL_DELETE_TAB_ENTRIES = "DROP TABLE IF EXISTS ${DBContract.TabEntry.TABLE_NAME}"

        }
    }

    companion object {
        private const val DATA_NOT_CHECKED = 0
        private const val DATA_CHECKED = 1
    }
}
