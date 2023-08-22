package com.arcxp.thearcxptv.main

import android.app.Application
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arcxp.ArcXPMobileSDK
import com.arcxp.commons.throwables.ArcXPException
import com.arcxp.commons.throwables.ArcXPSDKErrorType
import com.arcxp.commons.util.Either
import com.arcxp.commons.util.Failure
import com.arcxp.commons.util.Success
import com.arcxp.content.extendedModels.*
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.db.RememberVideoDao
import com.arcxp.thearcxptv.db.UpdatePosition
import com.arcxp.thearcxptv.db.VideoToRemember
import com.arcxp.thearcxptv.fragments.HomeFragment
import com.arcxp.thearcxptv.fragments.SearchFragment
import com.arcxp.thearcxptv.fragments.SettingsFragment
import com.arcxp.thearcxptv.models.LiveVideo
import com.arcxp.thearcxptv.utils.TAG
import com.arcxp.thearcxptv.utils.getDateString
import com.arcxp.thearcxptv.utils.log
import com.arcxp.video.ArcMediaPlayer
import com.arcxp.video.ArcMediaPlayerConfig
import com.arcxp.video.ArcVideoStreamCallback
import com.arcxp.video.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * View model class for the app
 */
class MainViewModel(application: Application, val videoDao: RememberVideoDao) :
    AndroidViewModel(application) {

    enum class FragmentView(val tag: String) {
        HOME("home"),
        SEARCH("search"),
        SETTINGS("settings")
    }

    private val supervisorJob = SupervisorJob()
    private val mIoScope = CoroutineScope(context = Dispatchers.IO + supervisorJob)

    var arcMediaPlayer: ArcMediaPlayer? = null

    private var findingLiveVideos = false

    var heroPosition = 0

    private val videoClient = ArcXPMobileSDK.mediaClient()

    //handle our live video call within view model
    private val videoResultsFlow =
        MutableStateFlow<Either<ArcXPException, List<VideoVO>>>(
            value = Success(emptyList())
        )

    //send new live video results to client:
    private val _liveVideoResults = MutableLiveData<List<LiveVideo>>()
    val liveVideoResults: LiveData<List<LiveVideo>> = _liveVideoResults

    init {
        viewModelScope.launch {
            videoResultsFlow.collectLatest {
                if (it is Success) {
                    handleLiveVideos(videos = it.success)
                } else if (it is Failure) {
                    Log.e(
                        TAG,
                        it.failure.message ?: it.failure.localizedMessage
                        ?: application.getString(R.string.live_video_error)
                    )
                }
            }
        }
        viewModelScope.launch {
            videoClient.findByUuid(
                shouldUseVirtualChannel = true,

                uuid = application.getString(R.string.virtual_channel_uuid),
                listener = object : ArcVideoStreamCallback {
                    override fun onError(
                        type: ArcXPSDKErrorType,
                        message: String,
                        value: Any?
                    ) {
                        Log.e(TAG, "Virtual Channel error: $message")
                    }

                    override fun onVideoStreamVirtual(arcVideoStreamVirtualChannel: ArcVideoStreamVirtualChannel?) {
                        arcVideoStreamVirtualChannel?.let {
                            _virtualChannelLoadedEvent.postValue(it)
                        }
                    }
                })

        }
        getSections()
    }

    //results of collection calls will come here, with size of site service result
    private val _collectionResults =
        mutableListOf<MutableLiveData<Either<ArcXPException, Map<Int, ArcXPCollection>>>>()
    val collectionResults: List<LiveData<Either<ArcXPException, Map<Int, ArcXPCollection>>>> =
        _collectionResults

    //Event observed by PlayVideoFragment to request video by id from Video Center
    private val _videoResultEvent = MutableSharedFlow<Either<ArcXPException, ArcVideoStream>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val videoResultEvent = _videoResultEvent.asSharedFlow()

    //Keep the video configuration here so it does not get
    //recreated on device rotation
    val arcMediaPlayerConfigBuilder = ArcMediaPlayerConfig.Builder()

    private val _sectionsLoadEvent = MutableLiveData<Either<ArcXPException, List<String>>>()
    val sectionsLoadEvent: LiveData<Either<ArcXPException, List<String>>> = _sectionsLoadEvent

    private val _virtualChannelLoadedEvent = MutableLiveData<ArcVideoStreamVirtualChannel>()
    val virtualChannelLoadedEvent: LiveData<ArcVideoStreamVirtualChannel> =
        _virtualChannelLoadedEvent

    //will signal to start detail fragment with uuid, isLiveEvent
    private val _openDetailsEvent = Channel<String>()
    val openDetailsEvent = _openDetailsEvent.consumeAsFlow()

    //will signal to start video fragment with pair<uuid, start Position>
    private val _openVideoEvent = Channel<Pair<String, Int>>()
    val openVideoEvent = _openVideoEvent.consumeAsFlow()

    private val _openVirtualVideoEvent = Channel<Boolean>()
    val openVirtualVideoEvent = _openVirtualVideoEvent.consumeAsFlow()

    var currentFragmentTag: FragmentView = FragmentView.HOME

    fun checkAndAdd(arcXPCollection: ArcXPCollection) {
        mIoScope.launch {
            val rememberedVideo = videoDao.getVideoById(arcXPCollection.id)
            if (rememberedVideo == null) {
                val currentVideo =
                    ArcXPMobileSDK.contentManager().getContentSuspend(id = arcXPCollection.id)
                when (currentVideo) {
                    is Success -> {
                        currentVideo.success.apply {
                            videoDao.rememberVideo(
                                VideoToRemember(
                                    uuid = this._id,
                                    videoTitle = this.title(),
                                    playPosition = 0L,
                                    playLength = this.duration ?: 0L,
                                    thumbnailURL = this.thumbnail(),
                                    description = this.subheadlines?.basic ?: "",
                                    credit = this.author(),
                                    displayDate = this.date(),
                                    resizedURL = this.imageUrl(),
                                    fallback = this.fallback()
                                )
                            )
                        }
                    }
                    is Failure -> {
                        Log.e(TAG, "checkAndAdd: ${currentVideo.failure.message}")
                    }
                }

            }
        }
    }

    //initiate signal to start video playback from beginning of video with uuid
    fun openVideoFromStart(uuid: String) {
        findingLiveVideos = false
        viewModelScope.launch {
            _openVideoEvent.send(element = Pair(uuid, 0))
        }
    }

    //initiate signal to start video playback from saved position in db with uuid
    fun openVideoFromSavedPosition(uuid: String) {
        viewModelScope.launch {
            findingLiveVideos = false
            val rememberedVideo = videoDao.getVideoById(uuid)
            val savedPosition = rememberedVideo?.playPosition?.toInt() ?: 0
            val totalTime = rememberedVideo?.playLength?.toInt() ?: 0

            //if video is over, we restart from beginning
            val startPosition = if (savedPosition >= totalTime) {
                0
            } else {
                savedPosition
            }
            viewModelScope.launch {
                _openVideoEvent.send(element = Pair(uuid, startPosition))
            }
        }
    }

    fun openVirtualChannel() {
        findingLiveVideos = false
        viewModelScope.launch {
            _openVirtualVideoEvent.send(element = true)
        }
    }

    val fragmentNames = arrayListOf("Home", "Search", "Settings")

    private val homeFragment = HomeFragment.newInstance()
    private val searchFragment = SearchFragment.newInstance()
    private val settingsFragment = SettingsFragment.newInstance()

    fun getSearchFragment(): SearchFragment {
        return searchFragment
    }

    fun getHomeFragment(): HomeFragment {
        return homeFragment
    }

    fun getSettingsFragment(): SettingsFragment {
        return settingsFragment
    }

    fun getCurrentFragment(): BaseFragmentInterface {
        return when (currentFragmentTag) {
            FragmentView.HOME -> homeFragment
            FragmentView.SEARCH -> searchFragment
            FragmentView.SETTINGS -> settingsFragment
        }
    }

    private fun getSections() {
        mIoScope.launch {
            ArcXPMobileSDK.contentManager().getSectionListSuspend().apply {
                when (this) {
                    is Success -> {
                        _collectionResults.clear()
                        //we create our collection results to observe for each collection from site service and return when ready

                        // we initialize our row
                        //we add three potential slots for our dynamic rows: live, virtual channels, and continue watching
                        repeat(times = this.success.size) {
                            _collectionResults.add(MutableLiveData<Either<ArcXPException, Map<Int, ArcXPCollection>>>())
                        }
                        //at this point we know how many sections we have from site service and can signal to app with this count to create rows
                        _sectionsLoadEvent.postValue(Success(success = this.success.map { it.name }))

                        //we now call for the collection result of each and return to ui
                        val idList = success.map { it.id }
                        val deferred = mutableListOf<Deferred<Unit>>()
                        idList.forEachIndexed { index, id ->
                            deferred.add(
                                async {
                                    val sectionResult =
                                        ArcXPMobileSDK.contentManager()
                                            .getCollectionSuspend(id = id)
                                    _collectionResults[index].postValue(sectionResult)
                                })
                        }

                    }
                    is Failure -> {
                        _sectionsLoadEvent.postValue(Failure(failure = this.failure))
                    }
                }
            }
        }
    }

    fun openDetails(id: String) {
        findingLiveVideos = false
        viewModelScope.launch {
            _openDetailsEvent.send(id)
        }
    }

    //Create an instance of the video player from the Video SDK.  This is done
    //here so that it can be retained upon device rotation.
    fun createVideoPlayer() {
        arcMediaPlayer = ArcMediaPlayer.createPlayer(getApplication())
    }

    //Dispose of the video player.  If this is not called the audio
    //will continue to play even after the video player is not being shown.
    fun disposeVideoPlayer() {
        arcMediaPlayer?.finish()
        arcMediaPlayer = null
    }

    override fun onCleared() {
        super.onCleared()
        disposeVideoPlayer()
        supervisorJob.cancel()
    }

    fun getFragment(index: Int): Fragment = when (index) {
        1 -> searchFragment
        2 -> settingsFragment
        else -> homeFragment
    }

    //Retrieve a video based on ID
    fun loadVideo(id: String) {
        videoClient.findByUuid(uuid = id, listener = object : ArcVideoStreamCallback {

            override fun onVideoStream(videos: List<ArcVideoStream>?) {
                if (videos?.isNotEmpty() == true) {
                    _videoResultEvent.tryEmit(value = Success(success = videos[0]))
                }
            }

            override fun onError(
                type: ArcXPSDKErrorType,
                message: String,
                value: Any?
            ) {
                _videoResultEvent.tryEmit(
                    value = Failure(
                        failure = ArcXPException(
                            type = ArcXPSDKErrorType.SERVER_ERROR,
                            message = message
                        )
                    )
                )

            }

        })
    }

    private fun handleLiveVideos(videos: List<VideoVO>) {
        mIoScope.launch {
            val currentItems = liveVideoResults.value
            val newItems = ArrayList<LiveVideo>()
            videos.forEach {
                try {
                    val dateLong = it.liveEventConfig?.displayDate
                    val displayDate = dateLong?.let { getDateString(time = dateLong) } ?: ""

                    newItems.add(
                        LiveVideo(
                            uuid = it.contentConfig?.uuid ?: "",
                            videoTitle = it.contentConfig?.title ?: "",
                            thumbnailURL = it.promoImage?.image?.url ?: "",
                            credit = it.contentConfig?.credits?.source ?: "",
                            description = it.contentConfig?.blurb ?: "",
                            displayDate = displayDate,
                            thumbnail = it.thumbnail() ?: "",
                            fallback = it.fallback() ?: ""
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (newItems != currentItems) {
                _liveVideoResults.postValue(newItems)
            }
        }
    }

    fun findLiveFlow(duration: Duration = 15.toDuration(unit = DurationUnit.SECONDS)) = flow {
        findingLiveVideos = true
        while (findingLiveVideos) {
            emit(value = Unit)
            videoResultsFlow.value = videoClient.findLiveSuspend()
            log("calling findLive")
            delay(duration = duration)
        }
    }

    fun watchingVideosFromDatabase() = videoDao.getRememberedVideos()

    fun setTabPosition(position: Int) {
        findingLiveVideos = position == 0
        currentFragmentTag = when (position) {
            1 -> FragmentView.SEARCH
            2 -> FragmentView.SETTINGS
            else -> FragmentView.HOME
        }
    }

    //There are a lot of places in the code that need to shut
    //down the video and the fragment.  They all call this method
    fun cleanupVideoOnClose(uuid: String? = null) {

        //save video progress position and data if desired:
        uuid?.let {
            viewModelScope.launch {
                videoDao.updatePosition(
                    update = UpdatePosition(
                        playPosition = arcMediaPlayer?.playerPosition ?: 0L,
                        uuid = it
                    )
                )
            }
        }

        disposeVideoPlayer()
    }

    /**
     * Passes key events to the media player.  Only
     * required for Fire TV devices.
     */
    fun onKeyEvent(event: KeyEvent?): Boolean {
        if (arcMediaPlayer != null) {
            return arcMediaPlayer?.dispatchKeyEvent(event!!)!!
        }
        return false
    }

}