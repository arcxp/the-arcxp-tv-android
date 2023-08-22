package com.arcxp.thearcxptv.cardviews

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.leanback.widget.ImageCardView
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.main.MainViewModel


/**
 * A very basic [ImageCardView] [androidx.leanback.widget.Presenter].You can
 * pass a custom style for the ImageCardView in the constructor. Use the default constructor style param to
 * create a Presenter with a default ImageCardView style.
 */
class VideoCardViewPresenter @JvmOverloads constructor(
    context: Context?,
    val vm: MainViewModel,
    cardThemeResId: Int = R.style.DefaultCardTheme
) :
    AbstractCardPresenter<VideoCardView?>(ContextThemeWrapper(context, cardThemeResId)) {

    override fun onCreateView() = VideoCardView(context = context, vm = vm)

    override fun onBindViewHolder(card: Card?, cardView: VideoCardView?) {
        if (card != null) {
            cardView?.updateUI(card = card)
            cardView?.setOnClickListener {
                if (card.isVirtualChannel) {
                    vm.openVirtualChannel()
                } else {
                    vm.openDetails(id = card._id)
                }
            }
        }
    }
}