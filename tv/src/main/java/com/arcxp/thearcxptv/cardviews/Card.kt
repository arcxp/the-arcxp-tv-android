package com.arcxp.thearcxptv.cardviews

import android.graphics.Color
import android.util.Log
import com.google.gson.annotations.SerializedName
import java.net.URI
import java.net.URISyntaxException


/**
 * This is a generic example of a custom data object, containing info we might want to keep with
 * each card on the home screen
 */
class Card {
    public var _id = ""

    @SerializedName("title")
    public var title = ""

    @SerializedName("description")
    public var description = ""

    @SerializedName("extraText")
    public var extraText = ""

    @SerializedName("card")
    public var imageUrl: String? = null

    @SerializedName("footerColor")
    private var mFooterColor: String? = null

    @SerializedName("selectedColor")
    private var mSelectedColor: String? = null

    @SerializedName("localImageResource")
    public var localImageResourceName: String? = null

    @SerializedName("footerIconLocalImageResource")
    public var footerLocalImageResourceName: String? = null
        set

    @SerializedName("type")
    public var type: Type? = null

    @SerializedName("thumbnail")
    public var thumbnail = ""

    @SerializedName("fallback")
    public var fallback = ""

    @SerializedName("resized")
    public var resized = ""

    @SerializedName("id")
    public var id = 0

    public var position: Long = 0
    public var length: Long = 0

    var rowPosition: Int = 0

    var isVirtualChannel = false

    @SerializedName("width")
    var width = 0

    @SerializedName("height")
    var height = 0
    val footerColor: Int
        get() = if (mFooterColor == null) -1 else Color.parseColor(mFooterColor)

    fun setFooterColor(footerColor: String?) {
        mFooterColor = footerColor
    }

    val selectedColor: Int
        get() = if (mSelectedColor == null) -1 else Color.parseColor(mSelectedColor)

    fun setSelectedColor(selectedColor: String?) {
        mSelectedColor = selectedColor
    }

    val imageURI: URI?
        get() = if (imageUrl == null) null else try {
            URI(imageUrl)
        } catch (e: URISyntaxException) {
            Log.d("URI exception: ", imageUrl!!)
            null
        }

//    fun getLocalImageResourceId(context: Context): Int {
//        return context.resources.getIdentifier(
//            localImageResourceName, "drawable",
//            context.packageName
//        )
//    }

    enum class Type {
        MOVIE_COMPLETE, MOVIE, MOVIE_BASE, ICON, SQUARE_BIG, SINGLE_LINE, GAME, SQUARE_SMALL, DEFAULT, SIDE_INFO, SIDE_INFO_TEST_1, TEXT, CHARACTER, GRID_SQUARE, VIDEO_GRID
    }
}