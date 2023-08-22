package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.leanback.widget.BaseCardView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.utils.TAG
import com.arcxp.thearcxptv.main.MainViewModel
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class VideoCardView @JvmOverloads constructor(context: Context, val vm: MainViewModel? = null) :
    BaseCardView(context, null, R.style.DefaultCardImageStyle) {
    init {
        LayoutInflater.from(getContext()).inflate(R.layout.video_card_view, this)
        isFocusable = true
        isFocusableInTouchMode = true
    }

    private val titleText = findViewById<TextView>(R.id.title)
    private val imageView = findViewById<ImageView>(R.id.main_image)
    private val progressBar = findViewById<ProgressBar>(R.id.watched_progress_bar)

    private var length: Long = 0

    private var progressLiveData: LiveData<Long?>? = null

    private val progressObserver = Observer<Long?> {
        it?.let { updateProgressBar(position = it) }
    }

    fun updateUI(card: Card) {

        titleText.text = card.title
        Glide.with(context)
            .asBitmap()
            .transform(CenterCrop(), RoundedCorners(20))
            .load(card.thumbnail)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "onLoadFailed: Resizer failed - Check Key")
                    return false
                }

            })
            .fallback(R.drawable.round_border_style)
            .error(Glide.with(context)
                .asBitmap()
                .transform(CenterCrop(), RoundedCorners(20))
                .load(card.fallback)
                .error(R.drawable.ic_baseline_error_24_black))
            .into(imageView)

        length = card.length
        progressLiveData = vm?.videoDao?.observePosition(uuid = card._id)
    }

    private fun updateProgressBar(position: Long) {

        if (position > 0 && length > 0) {
            val percent = (((position.toDouble()) / (length.toDouble())) * 100).toInt()
            progressBar.progress = percent
            progressBar.max = 100
            progressBar.visibility = VISIBLE
        } else {
            progressBar.visibility = GONE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressLiveData?.removeObserver(progressObserver)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        progressLiveData?.observeForever(progressObserver)
    }
}