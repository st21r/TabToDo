package io.github.bjxytw.tabtodo.db

import android.provider.BaseColumns


object DBContract {

    object TaskEntry : BaseColumns {
        const val TABLE_NAME = "tasks"
        const val TITLE = "title"
        const val CHECKED = "checked"
        const val SCHEDULE_STATE = "schedule_state"
        const val SCHEDULE_TIME = "schedule_time"
        const val SCHEDULE_REPEAT = "schedule_repeat"
        const val COLOR = "color"
        const val TAB_ID = "tab_id"
        const val POSITION = "position"
    }

    object TabEntry : BaseColumns {
        const val TABLE_NAME = "tabs"
        const val NAME = "name"
        const val SORT = "sort"
        const val POSITION = "position"
    }
}