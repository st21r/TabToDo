package io.github.bjxytw.tabtodo.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.data.TaskData

class CheckedTaskRecyclerAdapter(private val context: Context,
                                 private val listener: ItemUncheckListener,
                                 private val itemList : ArrayList<TaskData>) :
    RecyclerView.Adapter<CheckedTaskRecyclerAdapter.ViewHolder>() {

    private var recyclerView : RecyclerView? = null

    fun removeAll() {
        itemList.clear()
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : CheckedTaskRecyclerAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task_list_item, parent, false)
        val textLayout: LinearLayout = view.findViewById(R.id.task_list_text_layout)
        val titleView: TextView = view.findViewById(R.id.task_list_title)
        val checkButton: ImageButton = view.findViewById(R.id.task_check_button)


        recyclerView?.let { recycler ->

            textLayout.setOnLongClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnLongClickListener true

                AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.confirm_delete_task))
                    .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                        listener.onTaskDeleted(itemList[position])
                        itemList.removeAt(position)
                        notifyItemRemoved(position)
                    }
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show()

                true
            }

            checkButton.setOnClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnClickListener

                val checkedTask = itemList[position]

                checkButton.setImageResource(R.drawable.ic_check_circle_24dp)

                titleView.apply {
                    setTextColor(ContextCompat.getColor(context, R.color.colorPrimaryText))
                    paint.flags = paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                itemList.removeAt(position)
                notifyItemRemoved(position)
                listener.onTaskUnchecked(checkedTask)
            }
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val textLayout = holder.textLayout
        val titleView = holder.titleView
        val scheduleView = holder.subTitleView
        val checkButton = holder.checkButton

        titleView.apply {
            text = itemList[position].title
            setTextColor(ContextCompat.getColor(context, R.color.colorSecondaryText))
            paint.flags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        checkButton.setImageResource(R.drawable.ic_check_circle_checked_24dp)
        TaskItemRecyclerAdapter.setScheduleText(context, itemList[position].schedule, textLayout, scheduleView)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    interface ItemUncheckListener {
        fun onTaskUnchecked(data: TaskData)
        fun onTaskDeleted(data: TaskData)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textLayout: LinearLayout = itemView.findViewById(R.id.task_list_text_layout)
        val titleView: TextView = itemView.findViewById(R.id.task_list_title)
        val subTitleView: TextView = itemView.findViewById(R.id.task_list_sub_title)
        val checkButton : ImageButton = itemView.findViewById(R.id.task_check_button)
    }
}