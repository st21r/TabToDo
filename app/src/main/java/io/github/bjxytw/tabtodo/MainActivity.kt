package io.github.bjxytw.tabtodo

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_main.*
import android.support.design.widget.TabLayout
import android.support.v4.content.ContextCompat
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.ViewGroup
import io.github.bjxytw.tabtodo.adapter.TaskItemRecyclerAdapter
import io.github.bjxytw.tabtodo.adapter.TabPagerAdapter
import io.github.bjxytw.tabtodo.data.TabData
import io.github.bjxytw.tabtodo.data.TaskData
import io.github.bjxytw.tabtodo.db.DBHelper
import io.github.bjxytw.tabtodo.fragment.BottomNavigationDrawerFragment
import io.github.bjxytw.tabtodo.fragment.CreateDialogFragment
import io.github.bjxytw.tabtodo.fragment.DialogFragmentBase
import io.github.bjxytw.tabtodo.fragment.EditDialogFragment
import android.support.v7.app.AlertDialog
import android.os.Build
import android.os.Handler
import android.support.annotation.RequiresApi
import android.support.v4.content.LocalBroadcastManager
import io.github.bjxytw.tabtodo.receiver.NotificationAlarmReceiver
import java.util.*
import android.os.Looper
import android.preference.PreferenceManager
import io.github.bjxytw.tabtodo.settings.SettingsActivity

class MainActivity : AppCompatActivity(),
    TaskItemRecyclerAdapter.ItemClickListener,
    CreateDialogFragment.OnAddItemListener {

    private lateinit var dbHelper: DBHelper

    private lateinit var tabList: ArrayList<TabData>
    private lateinit var adapterList: ArrayList<TaskItemRecyclerAdapter>
    private lateinit var itemTouchCallbackList: ArrayList<ItemTouchCallBack>

    private lateinit var receiver: UpdateReceiver

    private var selectedTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottom_bar)

        add_button.setOnClickListener {
            CreateDialogFragment().apply {
                arguments = setupDialogFragmentBundle()
                show(supportFragmentManager, CreateDialogFragment.TAG)
                setOnAddItemListener(this@MainActivity)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel(applicationContext)

        dbHelper = DBHelper(applicationContext)
        setupTabs(dbHelper, intent.getIntExtra(INTENT_TAB_DEFAULT_POSITION, 0))

        receiver = UpdateReceiver.register(applicationContext, object : UpdateReceiver.Callback {
            override fun onUpdate() {
                setupTabs(dbHelper, selectedTabPosition)
            }
        })

    }

    override fun onDestroy() {
        receiver.unregister()
        dbHelper.close()
        super.onDestroy()
    }

    private fun setupTabs(helper: DBHelper, defaultTabPosition: Int) {
        val paddingBottomSize = dpToPx(this, RECYCLER_BOTTOM_MARGIN)

        val adapterList = ArrayList<TaskItemRecyclerAdapter>()
        val tabPagerList = ArrayList<Pair<RecyclerView, String>>()
        val itemTouchCallbackList = ArrayList<ItemTouchCallBack>()

        val (tabList, taskList) = helper.loadAll()

        for ((index, tab) in tabList.withIndex()) {
            val recyclerAdapter = TaskItemRecyclerAdapter(this, this, taskList[index], tab.sort)

            val recyclerView = RecyclerView(this).apply {
                setHasFixedSize(true)
                setPadding(0, 0, 0, paddingBottomSize)
                clipToPadding = false
                layoutManager = LinearLayoutManager(this@MainActivity)
                adapter = recyclerAdapter
            }

            val itemTouchCallBack = ItemTouchCallBack().apply {
                if (tab.sort == TabData.Sort.Date) dragEnabled = false
            }

            ItemTouchHelper(itemTouchCallBack).attachToRecyclerView(recyclerView)
            itemTouchCallbackList.add(itemTouchCallBack)

            adapterList.add(recyclerAdapter)

            tabPagerList.add(Pair(recyclerView, tab.name))
        }

        view_pager.adapter = TabPagerAdapter(tabPagerList)

        tab_layout.apply {
            setupWithViewPager(view_pager)

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    this@MainActivity.selectedTabPosition = tab.position
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })

            val tabViews = getChildAt(0) as ViewGroup
            for (i in 0 until tabViews.childCount) {
                tabViews.getChildAt(i).setOnLongClickListener {
                    startTabManagementActivity(this@MainActivity)
                    return@setOnLongClickListener false
                }
            }
        }

        this.adapterList = adapterList
        this.tabList = tabList
        this.itemTouchCallbackList = itemTouchCallbackList

        if (defaultTabPosition > 0 && defaultTabPosition < tabList.size) selectTab(defaultTabPosition)
    }

    private fun setupDialogFragmentBundle(): Bundle {
        val tabNameList = ArrayList<String>()
        for (tab in tabList) tabNameList.add(tab.name)

        return Bundle().apply {
            putStringArrayList(DialogFragmentBase.BUNDLE_TAB_NAME_LIST, tabNameList)
            putInt(DialogFragmentBase.BUNDLE_TAB_SELECTED_POSITION, selectedTabPosition)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            TabManagementActivity.REQUEST_CODE -> setupTabs(dbHelper, 0)
            CheckedTaskActivity.REQUEST_CODE -> setupTabs(dbHelper, selectedTabPosition)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.bottom_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item?.isEnabled = false
        Handler().postDelayed({ item?.isEnabled = true }, 500L)

        when (item?.itemId) {
            R.id.item_sort -> {
                val items = arrayOf(
                    TabData.Sort.Default.text,
                    TabData.Sort.Date.text)

                val defaultItem = tabList[selectedTabPosition].sort.num
                var selectedItem = defaultItem

                AlertDialog.Builder(this)
                    .setTitle("並び替え")
                    .setSingleChoiceItems(items, defaultItem) { _, checkedItem ->
                        selectedItem = checkedItem
                    }
                    .setPositiveButton("完了") { _, _ ->
                        if (selectedItem != defaultItem) {
                            tabList[selectedTabPosition].also { tab ->
                                tab.sort = TabData.Sort.fromInt(selectedItem)

                                adapterList[selectedTabPosition].changeSortMode(
                                    tab.sort, dbHelper.loadTasksInTab(tab.dataId, tab.sort))

                                dbHelper.updateTabSort(tab)

                                itemTouchCallbackList[selectedTabPosition].dragEnabled = when (tab.sort) {
                                    TabData.Sort.Default -> true
                                    TabData.Sort.Date -> false
                                }
                            }
                        }
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()
            }

            R.id.item_checked_task -> startCheckedTaskActivity(this, tabList[selectedTabPosition].dataId)

            android.R.id.home -> BottomNavigationDrawerFragment.show(supportFragmentManager)
        }
        return true
    }

    override fun onAddItem(title: String, schedule: TaskData.Schedule, tabPosition: Int) {
        addTask(title, schedule, tabPosition)
    }

    override fun onTaskTextClicked(position: Int, data: TaskData) {

        val bundle = setupDialogFragmentBundle()
        bundle.putString(DialogFragmentBase.BUNDLE_TITLE, data.title)
        bundle.putInt(DialogFragmentBase.BUNDLE_CALENDER_STATE, data.schedule.calenderState)
        bundle.putLong(DialogFragmentBase.BUNDLE_CALENDER_TIME, data.schedule.timeInMillis)
        bundle.putInt(DialogFragmentBase.BUNDLE_CALENDER_REPEAT, data.schedule.repeat.num)

        EditDialogFragment().apply {
            arguments = bundle
            show(supportFragmentManager, EditDialogFragment.TAG)
            setOnChangeItemListener(object : EditDialogFragment.OnChangeItemListener{
                override fun onChangeItem(title: String, schedule: TaskData.Schedule, newPosition: Int) {
                    cancelNotificationAlarm(this@MainActivity, data)

                    if (selectedTabPosition == newPosition) {
                        adapterList[selectedTabPosition].updateTask(position, title, schedule)
                        dbHelper.updateTask(data)
                        setNotificationAlarm(this@MainActivity, data)
                    } else {
                        dbHelper.deleteTask(data)
                        adapterList[selectedTabPosition].removeTask(position)
                        updateTasksPosition(selectedTabPosition)
                        addTask(title, schedule, newPosition)
                    }
                }

                override fun onDeleteItem() {
                    cancelNotificationAlarm(this@MainActivity, data)
                    dbHelper.deleteTask(data)
                    adapterList[selectedTabPosition].removeTask(position)
                }
            })
        }
    }

    override fun onTaskChecked(position: Int, data: TaskData) {
        cancelNotificationAlarm(this, data)

        if (data.schedule.repeat != TaskData.Repeat.None) repeatTask(data)
        else dbHelper.checkTask(data.dataId, data.tabId)

        updateTasksPosition(selectedTabPosition)
    }

    private fun repeatTask(data: TaskData) {
        dbHelper.deleteTask(data)

        val repeatCalender = Calendar.getInstance().apply {
            clear()
            timeInMillis = data.schedule.timeInMillis
        }

        do {
            when (data.schedule.repeat) {
                TaskData.Repeat.None -> {}
                TaskData.Repeat.EveryDay -> repeatCalender.add(Calendar.DAY_OF_MONTH, 1)
                TaskData.Repeat.EveryWeek -> repeatCalender.add(Calendar.WEEK_OF_MONTH, 1)
                TaskData.Repeat.EveryOtherWeek -> repeatCalender.add(Calendar.WEEK_OF_MONTH, 2)
                TaskData.Repeat.EveryMonth -> repeatCalender.add(Calendar.MONTH, 1)
                TaskData.Repeat.EveryOtherMonth -> repeatCalender.add(Calendar.MONTH, 2)
                TaskData.Repeat.EveryYear -> repeatCalender.add(Calendar.YEAR, 1)
            }
        } while (repeatCalender.timeInMillis <= System.currentTimeMillis())

        Handler(Looper.getMainLooper()).postDelayed({
            addTask(data.title, TaskData.Schedule(repeatCalender.timeInMillis,
                data.schedule.calenderState, data.schedule.repeat), selectedTabPosition)
        }, 500)
    }

    private fun addTask(title: String, schedule: TaskData.Schedule, tabPosition: Int) {
        val toFirst = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(SettingsActivity.PREF_KEY_INSERT_TOP, true)

        val data = dbHelper.insertTask(title, schedule, tabList[tabPosition].dataId, toFirst)

        adapterList[tabPosition].addTask(data, toFirst)
        setNotificationAlarm(this, data)

        selectTab(tabPosition)
    }

    private fun updateTasksPosition(tabPosition: Int) {
        if (tabList[tabPosition].sort == TabData.Sort.Default) {
            dbHelper.updateMultipleTasksPosition(
                adapterList[tabPosition].getAllTasksId(),
                tabList[tabPosition].dataId)
        }
    }

    private fun selectTab(position: Int) {
        tab_layout.setScrollPosition(position, 0.0f, true)
        view_pager.currentItem = position
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH)
            .apply { description = getString(R.string.notification_channel_description) }

        notificationManager.createNotificationChannel(channel)
    }

    inner class ItemTouchCallBack : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        var dragEnabled = true

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder) : Boolean {
            val adapter = recyclerView.adapter as TaskItemRecyclerAdapter
            adapter.moveItem(viewHolder.adapterPosition, target.adapterPosition)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE)
                viewHolder?.itemView?.setBackgroundColor(
                    ContextCompat.getColor(applicationContext, R.color.colorPrimary))

            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            super.clearView(recyclerView, viewHolder)
            viewHolder.itemView.setBackgroundColor(0)
            updateTasksPosition(selectedTabPosition)
        }

        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val dragFlags = if (dragEnabled) ItemTouchHelper.UP or ItemTouchHelper.DOWN else 0
            return makeMovementFlags(dragFlags, 0)
        }
    }

    class UpdateReceiver private constructor
        (context: Context, private val callback: Callback) : BroadcastReceiver() {
        private val manager: LocalBroadcastManager = LocalBroadcastManager.getInstance(context.applicationContext)

        init {
            manager.registerReceiver(this, IntentFilter().apply { addAction(ACTION_UPDATED) })
        }

        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != ACTION_UPDATED) return
            callback.onUpdate()
        }

        fun unregister() {
            manager.unregisterReceiver(this)
        }

        interface Callback { fun onUpdate() }

        companion object {
            private const val ACTION_UPDATED = "action_task_updated"

            fun register(context: Context, callback: Callback): UpdateReceiver {
                return UpdateReceiver(context, callback)
            }

            fun sendBroadcast(context: Context) {
                val manager = LocalBroadcastManager.getInstance(context.applicationContext)
                manager.sendBroadcast(Intent(ACTION_UPDATED))
            }
        }
    }

    companion object {
        const val INTENT_TAB_DEFAULT_POSITION = "tab_default_position"
        const val NOTIFICATION_CHANNEL_ID = "schedule_notification"

        private const val RECYCLER_BOTTOM_MARGIN = 28.0f

        private const val TIME_MILLIS_OF_5_MINUTE = 300000L  // 1000*60*5
        private const val TIME_MILLIS_OF_15_MINUTE = 900000L  // 1000*60*15
        private const val TIME_MILLIS_OF_30_MINUTE = 1800000L  // 1000*60*30
        private const val TIME_MILLIS_OF_HOUR = 3600000L  // 1000*60*60
        const val TIME_MILLIS_OF_DAY = 86400000L  // 1000*60*60*24

        fun startTabManagementActivity(activity: Activity?) {
            activity?.startActivityForResult(
                Intent(activity, TabManagementActivity::class.java), TabManagementActivity.REQUEST_CODE)
        }

        fun startCheckedTaskActivity(activity: Activity?, tabId: Int) {
            val intent = Intent(activity, CheckedTaskActivity::class.java).apply {
                putExtra(CheckedTaskActivity.EXTRA_TAB_ID, tabId)
            }
            activity?.startActivityForResult(intent, CheckedTaskActivity.REQUEST_CODE)
        }

        fun setNotificationAlarm(context: Context, data: TaskData) {
            val notificationTimeAgoValue = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SettingsActivity.PREF_KEY_NOTIFICATION_TIMING, SettingsActivity.VALUE_TIMING_DEFAULT)

            notificationTimeAgoValue?.let {
                setNotificationAlarm(context, data, it)
            }
        }

        private fun setNotificationAlarm(context: Context, data: TaskData, notificationTimeAgoValue: String) {
            if (data.schedule.calenderState != TaskData.Schedule.TIME_CONFIGURED) return

            val notificationTimeAgo = when (notificationTimeAgoValue) {
                SettingsActivity.VALUE_TIMING_DEFAULT -> 0
                SettingsActivity.VALUE_TIMING_5_MINUTE_AGO -> TIME_MILLIS_OF_5_MINUTE
                SettingsActivity.VALUE_TIMING_15_MINUTE_AGO -> TIME_MILLIS_OF_15_MINUTE
                SettingsActivity.VALUE_TIMING_30_MINUTE_AGO -> TIME_MILLIS_OF_30_MINUTE
                SettingsActivity.VALUE_TIMING_1_HOUR_AGO -> TIME_MILLIS_OF_HOUR
                else -> 0
            }

            val notificationTime = data.schedule.timeInMillis - notificationTimeAgo

            if (notificationTime <= System.currentTimeMillis()) return

            val alarmIntent = Intent(context, NotificationAlarmReceiver::class.java).apply {
                putExtra(NotificationAlarmReceiver.INTENT_TASK_ID, data.dataId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, data.dataId, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(notificationTime, null), pendingIntent)
        }

        fun cancelNotificationAlarm(context: Context, data: TaskData) {
            if (data.schedule.calenderState != TaskData.Schedule.TIME_CONFIGURED) return

            val alarmIntent = Intent(context, NotificationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, data.dataId, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(pendingIntent)
        }

        fun resetAllNotificationAlarms(context: Context, dbHelper: DBHelper) {
            for (task in dbHelper.getAllScheduledTasks()) setNotificationAlarm(context, task)
        }

        fun resetAllNotificationAlarms(context: Context, dbHelper: DBHelper, notificationTimeAgoValue: String) {
            for (task in dbHelper.getAllScheduledTasks()) setNotificationAlarm(context, task, notificationTimeAgoValue)
        }

        fun dpToPx(context: Context, dp: Float): Int {
            return (dp * context.resources.displayMetrics.density + 0.5f).toInt()
        }
    }
}
