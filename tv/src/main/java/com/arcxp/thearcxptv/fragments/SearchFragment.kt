package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import androidx.leanback.widget.*
import androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_SMALL
import androidx.lifecycle.lifecycleScope
import com.arcxp.content.sdk.ArcXPContentSDK
import com.arcxp.content.sdk.extendedModels.*
import com.arcxp.content.sdk.models.ArcXPContentError
import com.arcxp.content.sdk.models.ArcXPContentSDKErrorType
import com.arcxp.content.sdk.util.Failure
import com.arcxp.content.sdk.util.Success
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.cardviews.VideoCardViewPresenter
import com.arcxp.thearcxptv.db.VideoToRemember
import com.arcxp.thearcxptv.main.MainViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider,
    BaseFragmentInterface {

    private val vm: MainViewModel by sharedViewModel()

    private val mRowsAdapter = ArrayObjectAdapter(object: ListRowPresenter(ZOOM_FACTOR_SMALL) {
        override fun isUsingDefaultListSelectEffect() = false

    }.apply {
        shadowEnabled = false
    })

    private var mResultsFound = false

    lateinit var cardPresenter: VideoCardViewPresenter

    lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cardPresenter = VideoCardViewPresenter(requireContext(), vm)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        progressBar = view.findViewById(R.id.lb_progress_bar)
    }

    override fun onResume() {
        super.onResume()
        setSearchResultProvider(this)
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return mRowsAdapter
    }

    override fun onQueryTextChange(query: String?): Boolean {
        //search(query!!)
        return true
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        progressBar.visibility = View.VISIBLE
        query?.let { search(it.lowercase()) }
        return true
    }

    private fun search(query: String) {
        ArcXPContentSDK.contentManager().searchVideos(
                searchTerm = query,
                from = 0,
                size = 20
            ).observe(viewLifecycleOwner) {
            progressBar.visibility = View.INVISIBLE
                when (it) {
                    is Success -> {
                        populateData(it.success)
                    }
                    is Failure -> {
                        showSnackBar(ArcXPContentError(ArcXPContentSDKErrorType.SERVER_ERROR, it.failure.message), requireView(), R.id.error_message, false, requireActivity())
                    }
                }

            }
    }

    private fun populateData(response: Map<Int, ArcXPContentElement>) {
        viewLifecycleOwner.lifecycleScope.launch {
            mResultsFound = true
            mRowsAdapter.clear()

            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            val header = HeaderItem("Search Results")

            response.values.forEach {
                listRowAdapter.add(it)
                vm.videoDao.rememberVideo(
                    VideoToRemember(
                        uuid = it._id,
                        videoTitle = it.title(),
                        playPosition = 0L,
                        playLength = it.duration ?: 0L,
                        thumbnailURL = it.thumbnail(),
                        description = it.subheadlines?.basic ?: "",
                        credit = it.author(),
                        displayDate = it.date(),
                        resizedURL = it.imageUrl(),
                        fallback = it.fallback()
                    )
                )
            }
            mRowsAdapter.add(ListRow(header, listRowAdapter))
        }
    }

    override fun isOnBackPressed(): Boolean {
        return false
    }

    companion object {

        @JvmStatic
        fun newInstance() = SearchFragment()
    }
}