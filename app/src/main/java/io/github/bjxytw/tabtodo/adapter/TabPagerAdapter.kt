package io.github.bjxytw.tabtodo.adapter

import android.support.v4.view.PagerAdapter
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import io.github.bjxytw.tabtodo.data.TabData

class TabPagerAdapter(private val tabList: ArrayList<Pair<RecyclerView, String>>): PagerAdapter() {

    override fun getCount(): Int {
        return tabList.size
    }

    override fun isViewFromObject(view: View, any: Any): Boolean {
        return view === any
    }

    override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
        container.removeView(tabList[position].first)
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = tabList[position].first
        container.addView(view)
        return view
    }

    override fun getItemPosition(any: Any): Int {
        for ((index, tab) in tabList.withIndex())
            if (any === tab.first) return index
        return POSITION_NONE
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return tabList[position].second
    }
}