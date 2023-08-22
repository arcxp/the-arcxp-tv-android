package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.BaseCardView
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.utils.TAG
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

class LiveCardView(context: Context) : BaseCardView(context, null, R.style.DefaultCardImageStyle) {
    init {
        LayoutInflater.from(getContext()).inflate(R.layout.live_card_view, this)

        isFocusable = true
        isFocusableInTouchMode = true
    }


    fun updateUI(card: Card) {
        val titleText = findViewById<TextView>(R.id.title)
        val imageView = findViewById<ImageView>(R.id.main_image)

        titleText.text = card.title
        Glide.with(context)
            .asBitmap()
            .transform(CenterCrop(), RoundedCorners(30))
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
                .transform(CenterCrop(), RoundedCorners(30))
                .load(card.fallback)
                .error(Glide.with(context)
                    .asBitmap()
                    .transform(FitCenter(), RoundedCorners(50))
                    .load(R.drawable.ic_baseline_error_24_black)))
            .into(imageView)
    }
}