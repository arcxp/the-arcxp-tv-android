package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.view.LayoutInflater
import androidx.leanback.widget.BaseCardView
import com.arcxp.thearcxptv.R

class BlankLoadingCardView(context: Context) : BaseCardView(context, null, R.style.HeroCardStyle) {
    init {
        LayoutInflater.from(getContext()).inflate(R.layout.empty_card_view, this)

        isFocusable = false
        isFocusableInTouchMode = false
    }
}