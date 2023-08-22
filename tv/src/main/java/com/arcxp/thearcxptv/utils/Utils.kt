package com.arcxp.thearcxptv.utils

import android.content.Context
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.arc.arcvideo.model.ArcVideoStreamVirtualChannel
import com.arc.arcvideo.model.fallback

import com.arc.arcvideo.model.thumbnail
import com.arcxp.content.sdk.extendedModels.*

import com.arcxp.thearcxptv.cardviews.Card
import com.arcxp.thearcxptv.db.VideoToRemember
import com.arcxp.thearcxptv.models.LiveVideo
import java.text.SimpleDateFormat
import java.util.*

fun spinner(context: Context): CircularProgressDrawable {
    val circularProgressDrawable = CircularProgressDrawable(context)
    circularProgressDrawable.strokeWidth = 5f
    circularProgressDrawable.centerRadius = 30f
    circularProgressDrawable.start()
    return circularProgressDrawable
}

fun getDateString(time: Long): String =
    SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).format(time)

fun translateToCard(item: Any) =
    Card().apply {
        when (item) {
            //Collections returned by Websked have a curated order that isn't present in the data object itself, we preserve this order in row position
            //because it does not change from this predetermined value, we can reference it in the card
            is Pair<*, *> -> {
                if (item.first is ArcXPCollection) {
                    (item.first as ArcXPCollection).also {
                        _id = it.id
                        title = it.headlines.basic ?: ""
                        description = it.description?.basic.toString()
                        imageUrl = it.imageUrl()
                        length = it.duration ?: 0L
                        thumbnail = it.thumbnail()
                        fallback = it.fallback()
                    }
                }
                if (item.second is Int) {
                    rowPosition = item.second as Int
                }
            }
            is ArcXPContentElement -> {
                _id = item._id
                title = item.headlines?.basic ?: ""
                description = item.description?.basic.toString()
                imageUrl = item.imageUrl()
                thumbnail = item.thumbnail()
                fallback = item.fallback()
            }
            is VideoToRemember -> {
                _id = item.uuid
                title = item.videoTitle
                description = item.description
                thumbnail = item.thumbnailURL
                position = item.playPosition
                length = item.playLength
                resized = item.resizedURL

            }
            is LiveVideo -> {
                _id = item.uuid
                title = item.videoTitle
                description = item.description
                imageUrl = item.thumbnailURL
                thumbnail = item.thumbnail
                fallback = item.fallback
            }
            is ArcVideoStreamVirtualChannel -> {
                _id = item.id
                title = item.name ?: ""
                description = ""
                isVirtualChannel = true
                imageUrl = item.programs?.get(0)?.imageUrl ?: ""
                thumbnail = item.thumbnail()
                fallback = item.fallback()
            }
            else -> {
                _id = ""
                title = ""
                description = ""
                isVirtualChannel = false
                imageUrl = null
            }
        }
    }
