package io.github.bjxytw.tabtodo.fragment

import android.os.Bundle
import android.support.design.widget.TextInputEditText
import android.support.v7.widget.Toolbar
import io.github.bjxytw.tabtodo.R
import android.view.*
import android.widget.Spinner
import io.github.bjxytw.tabtodo.data.TaskData

class CreateDialogFragment : DialogFragmentBase() {
    private lateinit var listener: OnAddItemListener

    fun setOnAddItemListener(listener: OnAddItemListener) {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) : View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val tabSpinner: Spinner = view.findViewById(R.id.tab_spinner)
        val editText: TextInputEditText = view.findViewById(R.id.task_name_edit_text)
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)

        toolbar.apply {
            title = getString(R.string.task_create_title)

            setOnMenuItemClickListener {
                if (it?.itemId == R.id.edit_dialog_done) {
                    listener.onAddItem(editText.text.toString(), getConfiguredScheduleData(), tabSpinner.selectedItemPosition)
                    dismiss()
                }
                return@setOnMenuItemClickListener true
            }
        }
        return view
    }

    interface OnAddItemListener {
        fun onAddItem(title: String, schedule: TaskData.Schedule, tabPosition: Int)
    }

    companion object {
        const val TAG = "create_dialog"
    }
}