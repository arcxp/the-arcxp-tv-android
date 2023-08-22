package com.arcxp.thearcxptv.models

data class LiveVideo(
    val uuid: String,
    val videoTitle: String,
    val thumbnailURL: String,
    val description: String,
    val credit: String,
    val displayDate: String,
    val thumbnail: String,
    val fallback: String
)