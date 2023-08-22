package com.arcxp.thearcxptv.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arcxp.ArcXPMobileSDK
import com.arcxp.content.models.ArcXPSection
import com.arcxp.thearcxptv.cardviews.AbstractCardPresenter
import com.arcxp.thearcxptv.db.VideoToRemember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

fun Context.showErrorDialog(
    title: String = "Error",
    message: String? = null,
    posBtnTxt: String? = null,
    posAction: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(posBtnTxt) { _, _ ->
            posAction?.invoke()
        }.setNegativeButton("Cancel", null)
        .show()
}

fun Context.showAlertDialog(
    title: String = "Error",
    message: String? = null,
    posBtnTxt: String? = null,
    posAction: (() -> Unit)? = null
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(posBtnTxt) { _, _ ->
            posAction?.invoke()
        }
        .show()
}

//This extension allows us to use TAG in any class
val Any.TAG: String
    get() {
        val tag = javaClass.simpleName
        return if (tag.length <= 23) tag else tag.substring(0, 23)
    }

//try to use nav title as expected, but default to name if not populated
fun ArcXPSection.getNameToUseFromSection() =
    if (this.navigation.nav_title != null) {
        this.navigation.nav_title!!
    } else {
        Log.e(TAG, "Nav Title was null! defaulting to Section Name: ${this.name}")
        this.name
    }

private const val thumbnailResizeUrlKey = "thumbnailResizeUrl"

fun VideoToRemember.formatRunningTime(): CharSequence {
    val videoLength = String.format(
        "%02d hr %02d min %02d sec", TimeUnit.MILLISECONDS.toHours(this.playLength),
        TimeUnit.MILLISECONDS.toMinutes(this.playLength) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(this.playLength) % TimeUnit.MINUTES.toSeconds(1)
    )

    return videoLength
}

fun VideoToRemember.formatPositionAndDuration(): CharSequence {
    val videoLength = String.format(
        "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(this.playLength),
        TimeUnit.MILLISECONDS.toMinutes(this.playLength) % TimeUnit.HOURS.toMinutes(1),
        TimeUnit.MILLISECONDS.toSeconds(this.playLength) % TimeUnit.MINUTES.toSeconds(1)
    )
    val playPosition = String.format(
        "%02d:%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(this.playPosition),
        TimeUnit.MILLISECONDS.toMinutes(this.playPosition) % TimeUnit.HOURS.toMinutes(
            1
        ),
        TimeUnit.MILLISECONDS.toSeconds(this.playPosition) % TimeUnit.MINUTES.toSeconds(
            1
        )
    )

    return "$playPosition / $videoLength"
}

private fun createFullImageUrl(url: String) = "${ArcXPMobileSDK.baseUrl}$url"

fun AbstractCardPresenter<*>.toast(text: String, long: Boolean = false) = Toast.makeText(
    context,
    text,
    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
).show()

fun Fragment.toast(text: String, long: Boolean = false) = Toast.makeText(
    requireContext(),
    text,
    if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
).show()

fun Any.log(text: String) = Log.d(TAG, text)

fun <T> FragmentActivity.collectLatestLifeCycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            flow.collectLatest(action = collect)
        }
    }
}
fun <T> FragmentActivity.collectOneTimeEvent(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launchWhenStarted {
        launch {
            flow.collect(collector = collect)
        }
    }
}

fun <T> Fragment.collectLatestLifeCycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(state = Lifecycle.State.STARTED) {
            flow.collectLatest(action = collect)
        }
    }
}
