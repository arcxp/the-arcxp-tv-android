package com.arcxp.thearcxptv.cardviews


import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View.OnFocusChangeListener
import androidx.leanback.widget.ImageCardView
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.main.MainViewModel


/**
 * A very basic [ImageCardView] [androidx.leanback.widget.Presenter].You can
 * pass a custom style for the ImageCardView in the constructor. Use the default constructor to
 * create a Presenter with a default ImageCardView style.
 */
class HeroCardViewPresenter @JvmOverloads constructor(
    context: Context?,
    val vm: MainViewModel,
    cardThemeResId: Int = R.style.DefaultCardTheme
) :
    AbstractCardPresenter<HeroCardView?>(ContextThemeWrapper(context, cardThemeResId)) {

    override fun onCreateView(): HeroCardView {
        return HeroCardView(context)
    }


    override fun onBindViewHolder(card: Card?, cardView: HeroCardView?) {
        if (card != null) {
            cardView?.updateUI(card)
            cardView?.setOnClickListener {
                vm.openDetails(id = card._id)
            }
            //so the focus change listener from leanback does some stuff like making the view bigger when focused,
            //so we don't want to lose those changes
            //this in essence adds to that behavior without overriding
            val originalFocusChangeListener = cardView?.onFocusChangeListener
            cardView?.onFocusChangeListener =
                OnFocusChangeListener { view, focused ->
                    originalFocusChangeListener?.onFocusChange(view, focused)
                    if (focused) {
                        vm.heroPosition = card.rowPosition
                    }
                }
        }
    }
}