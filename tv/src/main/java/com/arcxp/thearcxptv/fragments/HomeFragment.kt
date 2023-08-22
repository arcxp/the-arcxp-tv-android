package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.*
import androidx.leanback.widget.FocusHighlight.ZOOM_FACTOR_SMALL
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arcxp.commons.throwables.ArcXPException
import com.arcxp.commons.throwables.ArcXPSDKErrorType
import com.arcxp.video.model.ArcVideoStreamVirtualChannel
import com.arcxp.content.extendedModels.ArcXPCollection
import com.arcxp.commons.util.Failure
import com.arcxp.commons.util.Success
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.cardviews.BlankLoadingCardViewPresenter
import com.arcxp.thearcxptv.cardviews.HeroCardViewPresenter
import com.arcxp.thearcxptv.cardviews.LiveCardViewPresenter
import com.arcxp.thearcxptv.cardviews.VideoCardViewPresenter
import com.arcxp.thearcxptv.db.VideoToRemember
import com.arcxp.thearcxptv.main.MainViewModel
import com.arcxp.thearcxptv.models.LiveVideo
import com.arcxp.thearcxptv.utils.CircularArrayObjectAdapter
import com.arcxp.thearcxptv.utils.TAG
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class HomeFragment : RowsSupportFragment(), BaseFragmentInterface {

    private val vm: MainViewModel by sharedViewModel()

    private val rowsAdapter = ArrayObjectAdapter(object : ListRowPresenter(ZOOM_FACTOR_SMALL) {
        override fun isUsingDefaultListSelectEffect() = false
    }.apply { shadowEnabled = false })

    //Local copy of our rows to build UI in order
    private val currentRowData = mutableMapOf<Int, ListRow>()

    private lateinit var cardPresenter: VideoCardViewPresenter
    private lateinit var heroCardPresenter: HeroCardViewPresenter
    private lateinit var liveCardViewPresenter: LiveCardViewPresenter
    private lateinit var blankLoadingCardViewPresenter: BlankLoadingCardViewPresenter

    lateinit var presenter: ListRowPresenter.SelectItemViewHolderTask
    private var isStartup = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cardPresenter = VideoCardViewPresenter(requireContext(), vm)
        heroCardPresenter =
            HeroCardViewPresenter(requireContext(), vm, R.style.DefaultCardThemeHero)
        liveCardViewPresenter = LiveCardViewPresenter(requireContext(), vm)
        blankLoadingCardViewPresenter = BlankLoadingCardViewPresenter(requireContext(), vm)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.watchingVideosFromDatabase().observe(viewLifecycleOwner, ::createWatchingRow)
        adapter = rowsAdapter

        vm.virtualChannelLoadedEvent.observe(viewLifecycleOwner) {
            createVirtualChannelRow(channel = it)
        }

        vm.sectionsLoadEvent.observe(viewLifecycleOwner) {
            when (it) {
                is Success -> {
                    //initialize site service row listeners
                    it.success.indices.forEach { rowIndex ->
                        vm.collectionResults[rowIndex].observe(viewLifecycleOwner) { collectionResult ->
                            when (collectionResult) {
                                is Success -> {
                                    loadRow(
                                        index = rowIndex,
                                        results = collectionResult.success.values.toList(),
                                        name = it.success[rowIndex]
                                    )
                                }
                                is Failure -> {
                                    showSnackBar(
                                        ArcXPException(
                                            type = ArcXPSDKErrorType.SERVER_ERROR,
                                            message = "Collection call failed for ${it.success[rowIndex]}"
                                        ),
                                        requireView(),
                                        R.id.error_message,
                                        false,
                                        requireActivity()
                                    )
                                    Log.e(
                                        TAG,
                                        "onViewCreated: ${collectionResult.failure.message} - ${it.success[rowIndex]}"
                                    )
                                }
                            }
                        }

                    }
                }
                is Failure -> {}
            }
        }
        if (!isStartup) {
            presenter.itemPosition =
                ((rowsAdapter.get(0) as ListRow).adapter.size() / 2) + ((rowsAdapter.get(0) as ListRow).adapter as CircularArrayObjectAdapter).firstEl() - 1 + vm.heroPosition
            setSelectedPosition(
                0,
                false,
                presenter
            )
        }
        vm.liveVideoResults.observe(viewLifecycleOwner, ::createDynamicLiveRow)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.findLiveFlow().collect {}
            }
        }

        if (isStartup) {
            isStartup = false
            displayPlaceHolderRow()
        }
    }

    private fun displayPlaceHolderRow() {
        val listRowAdapter = CircularArrayObjectAdapter(blankLoadingCardViewPresenter)
        listRowAdapter.add("")//add an empty object for placeholder to show in hero while loading.
        ListRow(null, listRowAdapter).apply {
            id = 983475 // non zero id that will be replaced by actual hero row
            currentRowData[HERO_ROW_POSITION] = this
        }
        refreshUI(startup = true)
    }

    //loads a single row with results from a collection
    private fun loadRow(index: Int, name: String, results: List<ArcXPCollection>) {
        if (index == 0) { // Hero Row (Top Row) with larger card
            val listRowAdapter = CircularArrayObjectAdapter(heroCardPresenter)

            results.forEachIndexed { collectionIndex, arcXPCollection ->
                vm.checkAndAdd(arcXPCollection)
                listRowAdapter.add(Pair(arcXPCollection, collectionIndex))
            }
            //we set header to null here to hide the title from row
            ListRow(null, listRowAdapter).apply {
                id = HERO_ROW_POSITION.toLong()
                currentRowData[HERO_ROW_POSITION] = this
            }
            refreshUI(startup = true)
        } else { // Subsequent row(s) from Site Service Collections
            val listRowAdapter = ArrayObjectAdapter(cardPresenter)
            val header = HeaderItem(name)

            results.forEachIndexed { collectionIndex, arcXPCollection ->
                vm.checkAndAdd(arcXPCollection)
                listRowAdapter.add(Pair(arcXPCollection, collectionIndex))
            }//TODO we don't specifically need index here, possibly rewrite translate to take a collection again ? or remain consistent here in case we need it later?

            //we offset the remaining rows with the dynamic rows
            val placeInList = index + DYNAMIC_ROWS
            currentRowData[placeInList] =
                ListRow(header, listRowAdapter).apply { id = placeInList.toLong() }
            refreshUI()
        }
    }

    //loads a row with single virtual channel result provided in strings
    private fun createVirtualChannelRow(channel: ArcVideoStreamVirtualChannel) =
        createDynamicRow(
            items = listOf(channel),
            rowTitle = getString(R.string.virtual_channel_row_title),
            presenter = cardPresenter,
            position = VIRTUAL_CHANNEL_POSITION
        )

    //loads a row from previously watched videos
    private fun createWatchingRow(watchingVideos: List<VideoToRemember>) =
        createDynamicRow(
            items = watchingVideos,
            rowTitle = getString(R.string.watching_videos_row_title),
            presenter = cardPresenter,
            position = CONTINUE_WATCHING_POSITION
        )

    private fun createDynamicRow(
        items: List<Any>,
        rowTitle: String,
        presenter: Presenter,
        position: Int
    ) {
        if (items.isNotEmpty()) {
            val listRowAdapter = ArrayObjectAdapter(presenter)
            items.forEach {
                listRowAdapter.add(it)
            }
            currentRowData[position] =
                ListRow(HeaderItem(rowTitle), listRowAdapter).apply {
                    id = position.toLong()
                }
            refreshUI()
        } else if (currentRowData.containsKey(position)) {// current items for row are empty, so remove row if it is currently displayed:
            currentRowData.remove(position)
            refreshUI()
        } // else do NOT refresh UI
    }

    private fun createDynamicLiveRow(
        items: List<LiveVideo>
    ) {
        if (items.isNotEmpty()) {
            val listRowAdapter = ArrayObjectAdapter(liveCardViewPresenter)
            listRowAdapter.addAll(0, items)
            var refreshUINow = false
            if (currentRowData.containsKey(LIVE_POSITION)) {//already have a live row, update row itself instead of all rows
                val diffCallback = object : DiffCallback<LiveVideo>() {
                    override fun areItemsTheSame(
                        oldItem: LiveVideo,
                        newItem: LiveVideo
                    ) = oldItem.uuid == newItem.uuid

                    override fun areContentsTheSame(
                        oldItem: LiveVideo,
                        newItem: LiveVideo
                    ) = oldItem == newItem
                }
                ((rowsAdapter[LIVE_POSITION] as ListRow).adapter as ArrayObjectAdapter).setItems(
                    items,
                    diffCallback
                )
                rowsAdapter.notifyArrayItemRangeChanged(LIVE_POSITION, 1)
            } else { // if we don't have a live row currently, just send everything to be refreshed (will only update the live row):
                refreshUINow = true
            }
            currentRowData[LIVE_POSITION] =
                ListRow(
                    HeaderItem(getString(R.string.live_videos_row_title)),
                    listRowAdapter
                ).apply {
                    id = LIVE_POSITION.toLong()
                }
            if (refreshUINow) {
                refreshUI()
            }
        } else if (currentRowData.containsKey(LIVE_POSITION)) {//remove entire row from view if new results are empty
            currentRowData.remove(LIVE_POSITION)
            refreshUI()
        } // else do NOT refresh UI
    }

    private fun refreshUI(startup: Boolean = false) {
        val diffCallback = object : DiffCallback<ListRow>() {
            override fun areItemsTheSame(
                oldItem: ListRow,
                newItem: ListRow
            ): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: ListRow,
                newItem: ListRow
            ): Boolean =
                oldItem.id == newItem.id
        }
        rowsAdapter.setItems(currentRowData.toSortedMap().values.toList(), diffCallback)
        if (startup) {
            presenter =
                ListRowPresenter.SelectItemViewHolderTask(
                    ((rowsAdapter.get(0) as ListRow).adapter.size() / 2) + ((rowsAdapter.get(
                        0
                    ) as ListRow).adapter as CircularArrayObjectAdapter).firstEl() - 1
                )
            presenter.isSmoothScroll = false
            scrollToTop()
        }
    }

    fun scrollToTop() {
        setSelectedPosition(
            0,
            true,
            presenter
        )
    }

    override fun isOnBackPressed(): Boolean {
        return false
    }

    companion object {
        //our dedicated 'slots' for ordering of dynamic rows outside of site service
        //first row 0 will be hero(large image) entry 0 from site service
        //then these if possible
        const val HERO_ROW_POSITION = 0
        const val LIVE_POSITION = 1
        const val CONTINUE_WATCHING_POSITION = 2
        const val VIRTUAL_CHANNEL_POSITION = 3
        //then the remaining collections from site service

        //total dynamic rows used
        const val DYNAMIC_ROWS = 3

        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}