package io.github.bjxytw.tabtodo.adapter

import android.content.Context
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.data.TabData
import io.github.bjxytw.tabtodo.data.TaskData
import java.util.*
import kotlin.collections.ArrayList

class TaskItemRecyclerAdapter(private val context: Context,
                              private val listener: ItemClickListener,
                              private var itemList : ArrayList<TaskData>,
                              private var sortMode: TabData.Sort) :
    RecyclerView.Adapter<TaskItemRecyclerAdapter.ViewHolder>() {

    private var recyclerView : RecyclerView? = null

    fun addTask(data: TaskData, toFirst: Boolean) {
        val index = when (sortMode) {
            TabData.Sort.Default -> {
                if (toFirst) 0 else itemList.size
            }

            TabData.Sort.Date -> {
                var addIndex = 0
                for ((index, item) in itemList.withIndex()) {
                    if (data.schedule.timeInMillis >= item.schedule.timeInMillis) {
                        addIndex = index + 1
                    }
                }
                addIndex
            }
        }

        itemList.add(index, data)
        notifyItemInserted(index)
        recyclerView?.scrollToPosition(index)
    }

    fun updateTask(position: Int, title: String, schedule: TaskData.Schedule) {
        itemList[position].title = title
        itemList[position].schedule = schedule

        if (sortMode == TabData.Sort.Date) {
            itemList.sortBy { item -> item.schedule.timeInMillis }
            notifyDataSetChanged()
        } else {
            notifyItemChanged(position)
        }
    }

    fun removeTask(position: Int) {
        itemList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun moveItem(from: Int, to: Int) {
        if (from < to) for (i in from until to) {
            Collections.swap(itemList, i, i + 1)
        } else for (i in from downTo to + 1) {
            Collections.swap(itemList, i, i - 1)
        }

        notifyItemMoved(from, to)
    }

    fun getAllTasksId() : ArrayList<Int> {
        val idList = ArrayList<Int>()
        for (item in itemList) idList.add(item.dataId)
        return idList
    }

    fun changeSortMode(sortMode: TabData.Sort, newItemList: ArrayList<TaskData>) {
        this.sortMode = sortMode
        itemList = newItemList
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_item, parent, false)
        val textLayout: LinearLayout = view.findViewById(R.id.task_list_text_layout)
        val titleView: TextView = view.findViewById(R.id.task_list_title)
        val checkButton: ImageButton = view.findViewById(R.id.task_check_button)

        recyclerView?.let { recycler ->
            textLayout.setOnClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnClickListener

                listener.onTaskTextClicked(position, itemList[position])
                notifyItemChanged(position)
            }

            checkButton.setOnClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnClickListener

                val checkedTask = itemList[position]

                checkButton.setImageResource(R.drawable.ic_check_circle_checked_24dp)

                titleView.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText))
                    paint.flags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }

                removeTask(position)

                listener.onTaskChecked(position, checkedTask)
            }
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val checkButton = holder.checkButton
        val textLayout = holder.textLayout
        val titleView = holder.titleTextView
        val scheduleView = holder.scheduleTextView

        titleView.apply {
            text = itemList[position].title
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryText))
            paint.flags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        checkButton.setImageResource(R.drawable.ic_check_circle_24dp)
        setScheduleText(context, itemList[position].schedule, textLayout, scheduleView)

    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    interface ItemClickListener {
        fun onTaskTextClicked(position: Int, data: TaskData)
        fun onTaskChecked(position: Int, data: TaskData)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textLayout: LinearLayout = itemView.findViewById(R.id.task_list_text_layout)
        val titleTextView: TextView = itemView.findViewById(R.id.task_list_title)
        val scheduleTextView: TextView = itemView.findViewById(R.id.task_list_sub_title)
        val checkButton : ImageButton = itemView.findViewById(R.id.task_check_button)
    }

    companion object {
        private const val TEXT_LAYOUT_PADDING_VERTICAL = 16.0f
        private const val TEXT_LAYOUT_PADDING_TOP_SHORT = 10.0f
        private const val TEXT_LAYOUT_PADDING_BOTTOM_SHORT = 8.0f
        private const val TEXT_LAYOUT_PADDING_HORIZONTAL = 12.0f

        fun setScheduleText(context: Context,
            schedule: TaskData.Schedule, textLayout: LinearLayout, scheduleView: TextView) {
            val repeatText = if (schedule.repeat == TaskData.Repeat.None) "" else "  " + schedule.repeat.text

            var scheduleVisibility = true

            when (schedule.calenderState) {
                TaskData.Schedule.NOT_SET -> scheduleVisibility = false

                TaskData.Schedule.DATE_CONFIGURED -> {
                    val scheduleText = DateUtils.formatDateTime(
                        context, schedule.timeInMillis,
                        DateUtils.FORMAT_SHOW_DATE or
                                DateUtils.FORMAT_SHOW_WEEKDAY or
                                DateUtils.FORMAT_ABBREV_ALL) + repeatText
                    scheduleView.text = scheduleText
                    setScheduleTextColor(context, scheduleView,
                        schedule.timeInMillis + MainActivity.TIME_MILLIS_OF_DAY)
                }

                TaskData.Schedule.TIME_CONFIGURED -> {
                    val scheduleText = DateUtils.formatDateTime(
                        context, schedule.timeInMillis,
                        DateUtils.FORMAT_SHOW_DATE or
                                DateUtils.FORMAT_SHOW_WEEKDAY or
                                DateUtils.FORMAT_SHOW_TIME or
                                DateUtils.FORMAT_ABBREV_ALL) + repeatText
                    scheduleView.text = scheduleText
                    setScheduleTextColor(context, scheduleView, schedule.timeInMillis)
                }
            }

            if (scheduleVisibility) {
                scheduleView.visibility = View.VISIBLE
                textLayout.setPadding(
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_HORIZONTAL),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_TOP_SHORT),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_HORIZONTAL),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_BOTTOM_SHORT))
            } else {
                scheduleView.visibility = View.GONE
                textLayout.setPadding(
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_HORIZONTAL),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_VERTICAL),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_HORIZONTAL),
                    MainActivity.dpToPx(context, TEXT_LAYOUT_PADDING_VERTICAL))
            }
        }

        private fun setScheduleTextColor(context: Context, scheduleView: TextView, timeInMillis: Long) {

            if (timeInMillis < System.currentTimeMillis()) {
                scheduleView.setTextColor(ContextCompat.getColor(context, R.color.colorRedText))
            } else {
                scheduleView.setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText))
            }
        }
    }
}