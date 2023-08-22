package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import com.arc.arcvideo.listeners.ArcVideoEventsListener
import com.arc.arcvideo.model.ArcVideoStream
import com.arc.arcvideo.model.ArcVideoStreamVirtualChannel
import com.arc.arcvideo.model.TrackingType
import com.arc.arcvideo.model.TrackingType.ON_PLAY_COMPLETED
import com.arc.arcvideo.model.TrackingType.ON_PLAY_STARTED
import com.arc.arcvideo.model.TrackingTypeData
import com.arc.arcvideo.util.isLive
import com.arcxp.content.sdk.models.ArcXPContentError
import com.arcxp.content.sdk.models.ArcXPContentSDKErrorType
import com.arcxp.content.sdk.util.Failure
import com.arcxp.content.sdk.util.Success
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.FragmentPlayvideoBinding
import com.arcxp.thearcxptv.main.MainViewModel
import com.arcxp.thearcxptv.utils.TAG
import com.arcxp.thearcxptv.utils.collectLatestLifeCycleFlow
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class PlayVideoFragment : Fragment(), BaseFragmentInterface {
    val vm: MainViewModel by sharedViewModel()
    private var _binding: FragmentPlayvideoBinding? = null
    private val binding get() = _binding!!

    private lateinit var uuid: String
    private var startPosition = 0
    private var isThisVideoLive = false
    private var isThisVirtualChannel = false

    private var videoHasStartedPlayback = false

    private val endPlayHandler = Handler(Looper.getMainLooper())
    private val endPlayRunnable = Runnable {
        vm.cleanupVideoOnClose(uuid)
        parentFragmentManager.popBackStack()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayvideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressedHandler()
                }
            })

        uuid = requireArguments().getString(UUID, "")
        startPosition = requireArguments().getInt(START_POSITION, 0)
        isThisVirtualChannel = requireArguments().getBoolean(IS_VIRTUAL_VIDEO)

        if (vm.arcMediaPlayer == null) {
            loadVideo(uuid = uuid)
        } else {
            restartVideo()
        }
        if (!isThisVirtualChannel) {
            collectLatestLifeCycleFlow(flow = vm.videoResultEvent) {
                when (it) {
                    is Success -> playVideo(arcVideoStream = it.success)
                    is Failure -> {
                        onError(ArcXPContentError(ArcXPContentSDKErrorType.SERVER_ERROR, getString(
                                            R.string.video_failed)))
                        Log.e(TAG, "onViewCreated: Video failed to play - ${it.failure.message}")
                    }
                }
            }
        }
    }

    private fun loadVideo(uuid: String) {
        vm.createVideoPlayer()
        vm.arcMediaPlayerConfigBuilder.setVideoFrame(videoFrame = binding.videoFrame)
        vm.arcMediaPlayerConfigBuilder.enablePip(enable = false)
        vm.arcMediaPlayerConfigBuilder.setActivity(activity = requireActivity())
        vm.arcMediaPlayerConfigBuilder.setAutoStartPlay(play = true)
        vm.arcMediaPlayerConfigBuilder.setShouldShowBackButton(shouldShowBackButton = false)
        vm.arcMediaPlayerConfigBuilder.setShouldShowFullScreenButton(shouldShowFullScreenButton = false)
        vm.arcMediaPlayerConfigBuilder.showSeekButton(show = true)
        vm.arcMediaPlayerConfigBuilder.useDialogForFullscreen(use = false)
        vm.arcMediaPlayerConfigBuilder.setStartMuted(muted = false)
        vm.arcMediaPlayerConfigBuilder.setShouldShowVolumeButton(showVolumeButton = false)
        vm.arcMediaPlayerConfigBuilder.setShouldShowTitleOnControls(shouldShowTitleOnControls = true)
        vm.arcMediaPlayer?.configureMediaPlayer(config = vm.arcMediaPlayerConfigBuilder.build())
        setVideoTracking()
        if (isThisVirtualChannel) {
            vm.virtualChannelLoadedEvent.observe(viewLifecycleOwner, this::playVideo)
        } else {
            vm.loadVideo(id = uuid)
        }
    }

    private fun playVideo(arcVideoStream: ArcVideoStream) {
        isThisVideoLive = arcVideoStream.isLive()
        if (arcVideoStream.status == "ended") {
            onError(ArcXPContentError(ArcXPContentSDKErrorType.SERVER_ERROR, getString(R.string.live_event_ended_error_description)))
        }
        vm.arcMediaPlayer?.initMedia(video = arcVideoStream)
        vm.arcMediaPlayer?.seekTo(ms = startPosition)
        vm.arcMediaPlayer?.setFullscreen(full = true)
        vm.arcMediaPlayer?.displayVideo()
    }

    private fun playVideo(arcVideoStreamVirtualChannel: ArcVideoStreamVirtualChannel) {

        vm.arcMediaPlayer?.initMedia(arcVideoStreamVirtualChannel = arcVideoStreamVirtualChannel)
        vm.arcMediaPlayer?.setFullscreen(full = true)
        vm.arcMediaPlayer?.displayVideo()
    }

    private fun restartVideo() {
        vm.arcMediaPlayerConfigBuilder.setVideoFrame(binding.videoFrame)
        vm.arcMediaPlayer?.configureMediaPlayer(vm.arcMediaPlayerConfigBuilder.build())
        vm.arcMediaPlayer?.displayVideo()
        setVideoTracking()
    }

    private fun setVideoTracking() {
        vm.arcMediaPlayer?.trackMediaEvents(object : ArcVideoEventsListener {
            override fun onVideoTrackingEvent(
                type: TrackingType?,
                videoData: TrackingTypeData.TrackingVideoTypeData?
            ) {
                when (type) {
                    ON_PLAY_STARTED -> {
                        videoHasStartedPlayback = true
                    }
                    ON_PLAY_COMPLETED -> {
                        videoHasStartedPlayback = false
                        endPlayHandler.postDelayed(endPlayRunnable, 5000)
                    }
                    else -> {}
                }
            }

            override fun onAdTrackingEvent(
                type: TrackingType?,
                adData: TrackingTypeData.TrackingAdTypeData?
            ) {
            }

            override fun onSourceTrackingEvent(
                type: TrackingType?,
                source: TrackingTypeData.TrackingSourceTypeData?
            ) {
            }

            override fun onError(
                type: TrackingType?,
                video: TrackingTypeData.TrackingErrorTypeData?
            ) {
            }
        })
    }

    private fun onError(error: ArcXPContentError) {
        returnHome()
        showSnackBar(error, binding.root, R.id.error_message, false, requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

   fun onBackPressedHandler() {
        if (videoHasStartedPlayback && vm.arcMediaPlayer?.isControlsVisible == true) {
            vm.arcMediaPlayer?.hideControls()
        } else {
            endPlayHandler?.removeCallbacks(endPlayRunnable)
            if (isThisVideoLive) {
                vm.cleanupVideoOnClose()
            } else {
                vm.cleanupVideoOnClose(uuid = uuid)
            }
            videoHasStartedPlayback = false
            parentFragmentManager.popBackStack()
        }
    }

    private fun returnHome() {
        vm.cleanupVideoOnClose()
        parentFragmentManager.popBackStack()//return to details
        parentFragmentManager.popBackStack()//return home
    }

    override fun onPause() {
        if (videoHasStartedPlayback) {
            parentFragmentManager.popBackStack()
            videoHasStartedPlayback = false
        }
        super.onPause()
    }

    companion object {

        private const val UUID = "id"
        private const val START_POSITION = "start position"
        private const val IS_VIRTUAL_VIDEO = "virtual video"

        @JvmStatic
        fun newInstance(
            uuid: String,
            startPosition: Int = 0,
            isVirtualChannel: Boolean = false
        ): PlayVideoFragment {
            val fragment = PlayVideoFragment()

            val args = Bundle()
            args.putString(UUID, uuid)
            args.putInt(START_POSITION, startPosition)
            args.putBoolean(IS_VIRTUAL_VIDEO, isVirtualChannel)
            fragment.arguments = args

            return fragment
        }
    }

    override fun isOnBackPressed(): Boolean {
        return false
    }
}