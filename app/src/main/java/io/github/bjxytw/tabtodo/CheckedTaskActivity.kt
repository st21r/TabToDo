package io.github.bjxytw.tabtodo

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import io.github.bjxytw.tabtodo.adapter.CheckedTaskRecyclerAdapter
import io.github.bjxytw.tabtodo.data.TaskData
import io.github.bjxytw.tabtodo.db.DBHelper
import io.github.bjxytw.tabtodo.settings.SettingsActivity
import kotlinx.android.synthetic.main.activity_checked_task.*

class CheckedTaskActivity : AppCompatActivity(), CheckedTaskRecyclerAdapter.ItemUncheckListener {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerAdapter: CheckedTaskRecyclerAdapter
    private var tabId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checked_task)
        setSupportActionBar(checked_task_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        tabId = intent.getIntExtra(EXTRA_TAB_ID, 0)

        dbHelper = DBHelper(applicationContext)
        recyclerAdapter = CheckedTaskRecyclerAdapter(this, this, dbHelper.getCheckedTasksInTab(tabId))

        checked_task_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@CheckedTaskActivity)
            adapter = recyclerAdapter
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    override fun finish() {
        setResult(Activity.RESULT_OK)
        super.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.checked_task_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()

            R.id.item_delete_checked_task -> {
                AlertDialog.Builder(this)
                    .setMessage(getString(R.string.confirm_delete_all_checked_task))
                    .setPositiveButton(getString(R.string.delete)) { _, _ ->
                        recyclerAdapter.removeAll()
                        dbHelper.deleteCheckedTask(tabId)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
        }
        return true
    }

    override fun onTaskUnchecked(data: TaskData) {
        val toFirst = PreferenceManager.getDefaultSharedPreferences(this)
            .getBoolean(SettingsActivity.PREF_KEY_INSERT_TOP, true)
        dbHelper.unCheckTask(data.dataId, data.tabId, toFirst)
        MainActivity.setNotificationAlarm(this, data)
        Snackbar.make(findViewById<View>(android.R.id.content), "元に戻しました", Snackbar.LENGTH_SHORT).show()
    }

    override fun onTaskDeleted(data: TaskData) {
        dbHelper.deleteTask(data)
    }

    companion object {
        const val REQUEST_CODE = 2
        const val EXTRA_TAB_ID = "tab_id"
    }
}