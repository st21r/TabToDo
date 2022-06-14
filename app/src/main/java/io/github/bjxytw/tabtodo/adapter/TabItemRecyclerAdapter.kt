package io.github.bjxytw.tabtodo.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.data.TabData
import java.util.*
import kotlin.collections.ArrayList


class TabItemRecyclerAdapter(private val context: Context,
                             private val itemList : ArrayList<TabData>,
                             private val listener: ItemClickListener) :
    RecyclerView.Adapter<TabItemRecyclerAdapter.ViewHolder>() {

    private var recyclerView : RecyclerView? = null
    private val deletedItemIdList = ArrayList<Int>()
    var edited = false

    fun addTab(name: String) {
        itemList.add(TabData(TabData.DataState.ID_NOT_SET, name, TabData.Sort.Default, TabData.DataState.Created))
        notifyItemInserted(itemList.size)
        edited = true
    }

    fun changeName(name: String, position: Int) {
        itemList[position].apply {
            this.name = name
            dataState = TabData.DataState.getEditedState(dataState)
        }
        notifyItemChanged(position)
        edited = true
    }

    fun moveItem(from: Int, to: Int) {
        if (from < to) {
            for (i in from..to)
                itemList[i].dataState = TabData.DataState.getEditedState(itemList[i].dataState)

            for (i in from until to)
                Collections.swap(itemList, i, i + 1)
        } else {
            for (i in from downTo to)
                itemList[i].dataState = TabData.DataState.getEditedState(itemList[i].dataState)

            for (i in from downTo to + 1)
                Collections.swap(itemList, i, i - 1)
        }
        notifyItemMoved(from, to)
        edited = true
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : TabItemRecyclerAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.tab_list_item, parent, false)
        val textView:TextView = view.findViewById(R.id.tab_list_text)
        val deleteButton: ImageButton = view.findViewById(R.id.tab_delete_button)
        val handleView: ImageView = view.findViewById(R.id.tab_list_handle)
        val holder = ViewHolder(view)

        recyclerView?.let { recycler ->
            textView.setOnClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnClickListener

                listener.onTextClick(position, itemList[position].name)
            }
            deleteButton.setOnClickListener {
                val position = recycler.getChildAdapterPosition(view)
                if (position == -1) return@setOnClickListener

                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.confirm_delete_tab_title))
                    .setMessage(context.getString(R.string.confirm_delete_tab_message))
                    .setPositiveButton(context.getString(R.string.delete)) { _, _ ->
                        deletedItemIdList.add(itemList[position].dataId)
                        itemList.removeAt(position)
                        notifyItemRemoved(position)
                        edited = true
                    }
                    .setNegativeButton(context.getString(R.string.cancel), null)
                    .show()
            }
        }

        handleView.setOnTouchListener { _: View, event: MotionEvent ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener.onDragStart(holder)
            }
            return@setOnTouchListener false
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val textView = holder.textView
        textView.text = itemList[position].name
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    fun getAllItems(): Pair<ArrayList<TabData>, ArrayList<Int>> {
        return itemList to deletedItemIdList
    }

    interface ItemClickListener {
        fun onTextClick(position: Int, title: String)
        fun onDragStart(viewHolder: ViewHolder)
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.tab_list_text)
    }
}