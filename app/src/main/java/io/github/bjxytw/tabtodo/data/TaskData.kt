package io.github.bjxytw.tabtodo.data

import android.graphics.Color

data class TaskData(val dataId: Int,
                    var title: String,
                    var schedule: Schedule,
                    var tabId: Int) {


    data class Schedule(val timeInMillis: Long, val calenderState: Int, val repeat: Repeat) {
        companion object {
            const val NOT_SET = 0
            const val DATE_CONFIGURED = 1
            const val TIME_CONFIGURED = 2
        }
    }

    enum class Repeat(val num: Int, val text: String) {
        None(0, "設定しない"),
        EveryDay(1, "毎日"),
        EveryWeek(2, "毎週"),
        EveryOtherWeek(3, "隔週"),
        EveryMonth(4, "毎月"),
        EveryOtherMonth(5, "隔月"),
        EveryYear(6, "毎年");

        companion object {
            fun fromInt(num: Int) : Repeat {
                when (num) {
                    None.num -> return None
                    EveryDay.num -> return EveryDay
                    EveryWeek.num -> return EveryWeek
                    EveryOtherWeek.num -> return EveryOtherWeek
                    EveryMonth.num -> return EveryMonth
                    EveryOtherMonth.num -> return EveryOtherMonth
                    EveryYear.num -> return EveryYear
                }
                return None
            }
        }
    }

    companion object {
        const val COLOR_DEFAULT = 0
    }
}