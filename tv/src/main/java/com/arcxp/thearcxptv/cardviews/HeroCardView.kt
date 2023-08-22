package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.leanback.widget.BaseCardView
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.utils.spinner
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import javax.sql.DataSource

class HeroCardView(context: Context) : BaseCardView(context, null, R.style.HeroCardStyle) {
    init {
        LayoutInflater.from(getContext()).inflate(R.layout.hero_card_view, this)

        isFocusable = true
        isFocusableInTouchMode = true
    }


    fun updateUI(card: Card) {
        val textView = findViewById<TextView>(R.id.text_view)
        val imageView = findViewById<ImageView>(R.id.main_image)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        textView.text = card.title
        Glide.with(imageView.context)
            .asBitmap()
            .placeholder(R.drawable.empty_card_loading_background)
            .transform(CenterCrop(), RoundedCorners(50))
            .load(card.imageUrl)
            .error(Glide.with(imageView.context)
                .asBitmap()
                .load(card.fallback)
                .error(R.drawable.ic_baseline_error_24_black))
            .into(imageView)

        progressBar.visibility = GONE
    }
}