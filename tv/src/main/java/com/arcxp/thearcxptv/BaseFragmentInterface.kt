package com.arcxp.thearcxptv

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.text.bold
import com.arcxp.commons.throwables.ArcXPException
import com.arcxp.thearcxptv.main.MainActivity
import com.google.android.material.snackbar.Snackbar

interface BaseFragmentInterface {

    fun isOnBackPressed(): Boolean

    fun showSnackBar(
        error: ArcXPException,
        view: View,
        viewId: Int,
        dismissible: Boolean = true,
        activity: Activity,
        onDismiss: () -> Unit = {}
    ) {
        val message = SpannableStringBuilder()
            .bold { append("${error.type?.name}:\n") }
            .append(error.message)
        val snackBar =
            Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
        if (dismissible) {
            snackBar.setAction(activity.getString(R.string.dismiss_error)) { onDismiss() }
        }
        snackBar.setActionTextColor(Color.WHITE)
        snackBar.duration = 10000
        snackBar.view.setBackgroundColor(Color.RED)
        snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text).maxLines =
            5

        if (view.layoutParams is CoordinatorLayout.LayoutParams) {
            val layoutParams = snackBar.view.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.anchorId = viewId
            layoutParams.anchorGravity = Gravity.CENTER_HORIZONTAL
            layoutParams.width = CoordinatorLayout.LayoutParams.MATCH_PARENT
        } else {
            val appBarView =
                (activity as MainActivity).findViewById<TextView>(R.id.error_message)
            val appBarParams = appBarView.layoutParams
            val frameLayoutParams = snackBar.view.layoutParams as FrameLayout.LayoutParams
            frameLayoutParams.topMargin = appBarParams.height
            frameLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
            frameLayoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        }

        if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && view.layoutParams is FrameLayout.LayoutParams) {
            val frameLayoutParams = snackBar.view.layoutParams as FrameLayout.LayoutParams
            frameLayoutParams.topMargin = 0
            frameLayoutParams.gravity = Gravity.CENTER_HORIZONTAL
            frameLayoutParams.width = FrameLayout.LayoutParams.MATCH_PARENT
        }
        snackBar.view.elevation = 150F

        snackBar.show()
    }
}
