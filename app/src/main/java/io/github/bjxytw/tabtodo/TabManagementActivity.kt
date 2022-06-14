package io.github.bjxytw.tabtodo

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.text.InputFilter
import android.view.MenuItem
import android.widget.EditText
import io.github.bjxytw.tabtodo.adapter.TabItemRecyclerAdapter
import io.github.bjxytw.tabtodo.db.DBHelper
import kotlinx.android.synthetic.main.activity_tab_management.*

class TabManagementActivity : AppCompatActivity(), TabItemRecyclerAdapter.ItemClickListener {

    private lateinit var dbHelper: DBHelper
    private lateinit var recyclerAdapter: TabItemRecyclerAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tab_management)
        setSupportActionBar(tab_management_toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        dbHelper = DBHelper(applicationContext)

        recyclerAdapter = TabItemRecyclerAdapter(this, dbHelper.loadAllTabs(), this)

        val recyclerView = tab_recycler_view.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@TabManagementActivity)
            adapter = recyclerAdapter
        }

        itemTouchHelper = ItemTouchHelper(itemTouchCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        add_tab_button.setOnClickListener {
            showDialog(false, null, null)
        }
    }

    override fun onDestroy() {
        dbHelper.close()
        super.onDestroy()
    }

    override fun finish() {
        setResult(if (recyclerAdapter.edited) Activity.RESULT_OK else Activity.RESULT_CANCELED)
        super.finish()
    }

    override fun onPause() {
        if (recyclerAdapter.edited) {
            val (tabs, deletedTabs) = recyclerAdapter.getAllItems()
            dbHelper.saveAllTabs(tabs, deletedTabs)
        }
        super.onPause()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    override fun onTextClick(position: Int, title: String) {
        showDialog(true, position, title)
    }

    private fun showDialog(isEditMode: Boolean, position: Int?, oldName: String?) {
        if (dialog != null && dialog!!.isShowing) return

        val margin = MainActivity.dpToPx(this, 24.0f)
        val marginBottom = MainActivity.dpToPx(this, 4.0f)
        val dialogBuilder = AlertDialog.Builder(this)
        val title = if (isEditMode) "タブの編集" else "タブの追加"

        val editText = EditText(this).apply {
            setPadding(margin, margin, margin, marginBottom)
            setBackgroundColor(Color.TRANSPARENT)
            setSingleLine()
            if (isEditMode && oldName != null) {
                setText(oldName)
                setOnClickListener { selectAll() }
            }
            filters = arrayOf(InputFilter.LengthFilter(14))
        }

        dialogBuilder.apply {
            setTitle(title)
            setView(editText)
            setPositiveButton("完了")
            { _: DialogInterface, _: Int ->
                val text = editText.text.toString()
                if (text.isNotEmpty()) {
                    if (isEditMode && position != null)
                        recyclerAdapter.changeName(text, position)
                    else {
                        recyclerAdapter.addTab(text)
                    }
                }
            }
            setNegativeButton(getString(R.string.cancel), null)
        }

        dialog = dialogBuilder.create().apply { show() }
    }

    override fun onDragStart(viewHolder: TabItemRecyclerAdapter.ViewHolder) {
        itemTouchHelper.startDrag(viewHolder)
    }

    private val itemTouchCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {

        override fun onMove(recyclerView: RecyclerView,
                            viewHolder: RecyclerView.ViewHolder,
                            target: RecyclerView.ViewHolder) : Boolean {
            val adapter = recyclerView.adapter as TabItemRecyclerAdapter
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
        }
    }

    companion object {
        const val REQUEST_CODE = 1
    }
}
