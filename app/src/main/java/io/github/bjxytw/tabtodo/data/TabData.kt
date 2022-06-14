package io.github.bjxytw.tabtodo.data

data class TabData(val dataId: Int, var name: String, var sort: Sort, var dataState: DataState) {
    enum class DataState {
        Default, Edited, Created;

        companion object {
            const val ID_NOT_SET = -1
            fun getEditedState(dataState: DataState) : DataState {
                if (dataState == Created) return Created
                return Edited
            }
        }
    }
    enum class Sort(val num: Int, val text: String) {
        Default(0, "カスタム"),
        Date(1, "日付順");

        companion object {
            fun fromInt(num: Int) : Sort {
                when (num) {
                    Default.num -> return Default
                    Date.num -> return Date
                }
                return Default
            }
        }
    }
}