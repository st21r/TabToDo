package io.github.bjxytw.tabtodo.fragment

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.support.design.widget.NavigationView
import android.support.v4.app.DialogFragment
import android.support.v4.app.FragmentManager
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.bjxytw.tabtodo.MainActivity
import io.github.bjxytw.tabtodo.R
import io.github.bjxytw.tabtodo.settings.SettingsActivity

class BottomNavigationDrawerFragment: BottomSheetDialogFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_bottom_drawer, container, false)
        val navigationView: NavigationView = view.findViewById(R.id.navigation_view)
        navigationView.setNavigationItemSelectedListener {menuItem ->
            when (menuItem.itemId) {
                R.id.tab_management -> MainActivity.startTabManagementActivity(activity)
                R.id.settings -> startActivity(Intent(activity, SettingsActivity::class.java))
                R.id.send_feedback -> {
                    val emailIntent = Intent(Intent.ACTION_SENDTO,
                        Uri.fromParts("mailto", "bjxytw@gmail.com", null))
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "TAB ToDo Feedback")
                    startActivity(Intent.createChooser(emailIntent, "メールを送信"))
                }
            }
            return@setNavigationItemSelectedListener true
        }
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog.window?.let {
            if (Build.VERSION.SDK_INT >= 27) {
                val metrics = DisplayMetrics()
                it.windowManager.defaultDisplay.getMetrics(metrics)
                val navigationBarDrawable = GradientDrawable()
                navigationBarDrawable.shape = GradientDrawable.RECTANGLE
                navigationBarDrawable.setColor(Color.WHITE)
                val layers = arrayOf<Drawable>(GradientDrawable(), navigationBarDrawable)
                val windowBackground = LayerDrawable(layers)
                windowBackground.setLayerInsetTop(1, metrics.heightPixels)
                it.setBackgroundDrawable(windowBackground)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        dismissAllowingStateLoss()
    }

    override fun show(manager: FragmentManager, tag: String) {
        if ((manager.findFragmentByTag(tag) is DialogFragment).not()) {
            super.show(manager, tag)
        }
    }

    companion object {
        private const val TAG = "bottom_navigation_dialog"
        fun show(fragmentManager: FragmentManager) {
            val dialog = BottomNavigationDrawerFragment()
            dialog.show(fragmentManager, TAG)
        }
    }
}