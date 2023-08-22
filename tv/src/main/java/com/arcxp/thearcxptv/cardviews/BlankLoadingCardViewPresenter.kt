package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.leanback.widget.ImageCardView
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.main.MainViewModel


/**
 * A very basic [ImageCardView] [androidx.leanback.widget.Presenter].You can
 * pass a custom style for the ImageCardView in the constructor. Use the default constructor to
 * create a Presenter with a default ImageCardView style.
 */
class BlankLoadingCardViewPresenter @JvmOverloads constructor(
    context: Context?,
    val vm: MainViewModel,
    cardThemeResId: Int = R.style.HeroCardStyle
) :
    AbstractCardPresenter<BlankLoadingCardView?>(ContextThemeWrapper(context, cardThemeResId)) {

    override fun onCreateView(): BlankLoadingCardView {
        return BlankLoadingCardView(context)
    }


    override fun onBindViewHolder(card: Card?, cardView: BlankLoadingCardView?) {}
}