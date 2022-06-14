package io.github.bjxytw.tabtodo.fragment

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import io.github.bjxytw.tabtodo.R
import java.util.*
import android.text.format.DateUtils
import android.text.format.DateUtils.*
import io.github.bjxytw.tabtodo.data.TaskData
import android.app.AlertDialog


abstract class DialogFragmentBase : DialogFragment() {
    private var tabNameList: ArrayList<String>? = null
    private var tabPositionOld: Int = 0
    protected val calender: Calendar = Calendar.getInstance()
    protected var calendarState: Int = TaskData.Schedule.NOT_SET
    protected var repeat: TaskData.Repeat = TaskData.Repeat.None

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_FullScreenDialog)
        arguments?.let {
            tabNameList = it.getStringArrayList(BUNDLE_TAB_NAME_LIST)
            tabPositionOld = it.getInt(BUNDLE_TAB_SELECTED_POSITION)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        super.onCreateView(inflater, container, savedInstanceState)
        val view = inflater.inflate(R.layout.fragment_edit_dialog, container, false)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        val tabSpinner: Spinner = view.findViewById(R.id.tab_spinner)
        val editText: TextInputEditText = view.findViewById(R.id.task_name_edit_text)
        val dateText: TextView = view.findViewById(R.id.add_date_text)
        val timeText: TextView = view.findViewById(R.id.add_time_text)
        val repeatText: TextView = view.findViewById(R.id.add_repeat_text)

        toolbar.apply {
            inflateMenu(R.menu.edit_dialog_menu)
            setNavigationOnClickListener { dismiss() }
        }

        if (tabNameList != null) {
            tabSpinner.apply {
                adapter = ArrayAdapter(context, R.layout.spinner_item, tabNameList!!)
                    .apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
                setSelection(tabPositionOld)
            }
        }

        editText.apply {
            hint = getString(R.string.task_title_hint)
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        }

        val dateSetListener = DatePickerDialog.OnDateSetListener {
                _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->

            calender.set(year, month, dayOfMonth)

            if (calendarState == TaskData.Schedule.NOT_SET) {
                calendarState = TaskData.Schedule.DATE_CONFIGURED
            }

            dateText.text = getDateFormat()
            timeText.visibility = View.VISIBLE
            repeatText.visibility = View.VISIBLE
        }

        val timeSetListener = TimePickerDialog.OnTimeSetListener {
                _: TimePicker, hour: Int, minute: Int ->

            calender.set(Calendar.HOUR_OF_DAY, hour)
            calender.set(Calendar.MINUTE, minute)

            calendarState = TaskData.Schedule.TIME_CONFIGURED
            timeText.text = getTimeFormat()
        }

        dateText.setOnClickListener {
            DatePickerDialog(context!!, dateSetListener,
                calender.get(Calendar.YEAR),
                calender.get(Calendar.MONTH),
                calender.get(Calendar.DAY_OF_MONTH)).apply {
                setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.delete)) { _: DialogInterface, _: Int ->

                    val nowCalendar = Calendar.getInstance()
                    nowCalendar.apply {
                        calender.set(get(Calendar.YEAR),
                            get(Calendar.MONTH),
                            get(Calendar.DAY_OF_MONTH),
                            get(Calendar.HOUR_OF_DAY),
                            get(Calendar.MINUTE))
                    }

                    calendarState = TaskData.Schedule.NOT_SET
                    repeat = TaskData.Repeat.None

                    dateText.text = getString(R.string.add_date)
                    timeText.text = getString(R.string.add_time)
                    repeatText.text = getString(R.string.add_repeat)
                    timeText.visibility = View.GONE
                    repeatText.visibility = View.GONE
                }
                show()
            }
        }

        timeText.setOnClickListener {
            TimePickerDialog(context!!, timeSetListener,
                calender.get(Calendar.HOUR_OF_DAY),
                calender.get(Calendar.MINUTE), true).apply {
                setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.delete)) { _: DialogInterface, _: Int ->
                    setCurrentTime()
                    calendarState = TaskData.Schedule.DATE_CONFIGURED
                    timeText.text = getString(R.string.add_time)
                }
                show()
            }
        }

        repeatText.setOnClickListener {
            val items = arrayOf(
                TaskData.Repeat.None.text,
                TaskData.Repeat.EveryDay.text,
                TaskData.Repeat.EveryWeek.text,
                TaskData.Repeat.EveryOtherWeek.text,
                TaskData.Repeat.EveryMonth.text,
                TaskData.Repeat.EveryOtherMonth.text,
                TaskData.Repeat.EveryYear.text)

            AlertDialog.Builder(context)
                .setItems(items) { _, selectedItem ->
                    repeat = TaskData.Repeat.fromInt(selectedItem)
                    repeatText.text = if (repeat == TaskData.Repeat.None) getString(R.string.add_repeat) else repeat.text
                }
                .show()
        }

        return view
    }

    protected fun setCurrentTime() {
        val nowCalendar = Calendar.getInstance()
        calender.set(Calendar.HOUR_OF_DAY, nowCalendar.get(Calendar.HOUR_OF_DAY))
        calender.set(Calendar.MINUTE, nowCalendar.get(Calendar.MINUTE))
    }

    protected fun getDateFormat(): String {
         return DateUtils.formatDateTime(activity?.applicationContext, calender.timeInMillis,
             FORMAT_SHOW_YEAR or FORMAT_SHOW_DATE or FORMAT_SHOW_WEEKDAY or FORMAT_ABBREV_ALL)
    }

    protected fun getTimeFormat(): String {
        return DateUtils.formatDateTime(activity?.applicationContext, calender.timeInMillis, FORMAT_SHOW_TIME)
    }

    protected fun getConfiguredScheduleData() : TaskData.Schedule {
        val result = Calendar.getInstance().apply { clear() }

        when (calendarState) {
            TaskData.Schedule.NOT_SET -> result.timeInMillis  = Long.MAX_VALUE

            TaskData.Schedule.DATE_CONFIGURED -> {
                calender.apply {
                    result.set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH))
                }
            }

            TaskData.Schedule.TIME_CONFIGURED -> {
                calender.apply {
                    result.set(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH),
                        get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE))
                }
            }
        }

        return TaskData.Schedule(result.timeInMillis, calendarState, repeat)
    }

    override fun onStart() {
        super.onStart()
        val width = ViewGroup.LayoutParams.MATCH_PARENT
        val height = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window?.setLayout(width, height)
    }

    override fun show(manager: FragmentManager, tag: String) {
        if ((manager.findFragmentByTag(tag) is DialogFragment).not()) {
            super.show(manager, tag)
        }
    }

    companion object {
        const val BUNDLE_TAB_NAME_LIST = "tab_name_list"
        const val BUNDLE_TAB_SELECTED_POSITION = "tab_selected_position"
        const val BUNDLE_TITLE = "name"
        const val BUNDLE_CALENDER_STATE = "calender_state"
        const val BUNDLE_CALENDER_TIME = "calender_time"
        const val BUNDLE_CALENDER_REPEAT = "calender_repeat"
    }
}