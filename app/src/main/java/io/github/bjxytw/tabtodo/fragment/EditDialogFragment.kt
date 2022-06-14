package io.github.bjxytw.tabtodo.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Spinner
import android.widget.TextView
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.data.TaskData

class EditDialogFragment : DialogFragmentBase() {
    private var titleOld: String? = null
    private lateinit var listener: OnChangeItemListener

    fun setOnChangeItemListener(listener: OnChangeItemListener) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { bundle ->
            titleOld = bundle.getString(BUNDLE_TITLE)
            calendarState = bundle.getInt(BUNDLE_CALENDER_STATE)
            repeat = TaskData.Repeat.fromInt(bundle.getInt(BUNDLE_CALENDER_REPEAT))

            when (calendarState) {
                TaskData.Schedule.NOT_SET -> {}

                TaskData.Schedule.DATE_CONFIGURED -> {
                    calender.timeInMillis = bundle.getLong(BUNDLE_CALENDER_TIME)
                    setCurrentTime()
                }

                TaskData.Schedule.TIME_CONFIGURED -> {
                    calender.timeInMillis = bundle.getLong(BUNDLE_CALENDER_TIME)
                }
            }

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val tabSpinner: Spinner = view.findViewById(R.id.tab_spinner)
        val editText: TextInputEditText = view.findViewById(R.id.task_name_edit_text)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val dateText: TextView = view.findViewById(R.id.add_date_text)
        val timeText: TextView = view.findViewById(R.id.add_time_text)
        val repeatText: TextView = view.findViewById(R.id.add_repeat_text)

        toolbar.apply {
            title = getString(R.string.task_edit_title)

            menu.findItem(R.id.edit_dialog_delete).isVisible = true

            editText.setText(titleOld)
            editText.selectAll()

            setOnMenuItemClickListener {
                when (it?.itemId) {
                    R.id.edit_dialog_done -> {
                        listener.onChangeItem(editText.text.toString(), getConfiguredScheduleData(), tabSpinner.selectedItemPosition)
                        dismiss()
                    }

                    R.id.edit_dialog_delete -> {
                        AlertDialog.Builder(activity)
                            .setMessage(getString(R.string.confirm_delete_task))
                            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                                listener.onDeleteItem()
                                dismiss()
                            }
                            .setNegativeButton(getString(R.string.cancel), null)
                            .show()
                    }
                }

                return@setOnMenuItemClickListener true
            }
        }

        when (calendarState) {
            TaskData.Schedule.NOT_SET -> {}

            TaskData.Schedule.DATE_CONFIGURED -> {
                dateText.text = getDateFormat()
                if (repeat != TaskData.Repeat.None) repeatText.text = repeat.text

                timeText.visibility = View.VISIBLE
                repeatText.visibility = View.VISIBLE
            }

            TaskData.Schedule.TIME_CONFIGURED -> {
                dateText.text = getDateFormat()
                timeText.text = getTimeFormat()
                if (repeat != TaskData.Repeat.None) repeatText.text = repeat.text


                timeText.visibility = View.VISIBLE
                repeatText.visibility = View.VISIBLE
            }
        }

        return view
    }

    interface OnChangeItemListener {
        fun onChangeItem(title: String, schedule: TaskData.Schedule, newPosition: Int)
        fun onDeleteItem()
    }

    companion object {
        const val TAG = "edit_dialog"
    }
}