package com.arcxp.thearcxptv.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.FragmentDetailsBinding
import com.arcxp.thearcxptv.db.VideoToRemember
import com.arcxp.thearcxptv.main.MainViewModel
import com.arcxp.thearcxptv.models.LiveVideo
import com.arcxp.thearcxptv.utils.TAG
import com.arcxp.thearcxptv.utils.collectLatestLifeCycleFlow
import com.arcxp.thearcxptv.utils.formatPositionAndDuration
import com.arcxp.thearcxptv.utils.formatRunningTime

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DetailsFragment : Fragment() {

    val vm: MainViewModel by sharedViewModel()

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val id = requireArguments().getString(UUID_KEY, "")
        val isLive = requireArguments().getBoolean(IS_LIVE_EVENT, false)

        binding.resumeButton.setOnClickListener {
            vm.openVideoFromSavedPosition(id)
        }

        binding.playButton.setOnClickListener {
            vm.openVideoFromStart(id)
        }
        binding.startOverButton.setOnClickListener {
            vm.openVideoFromStart(id)
        }
        binding.watchAgain.setOnClickListener {
            vm.openVideoFromStart(id)
        }

        collectLatestLifeCycleFlow(flow = vm.videoDao.getVideoFlowById(uuid = id)) {
            initUi(response = it)

        }
    }

    private fun initUi(response: VideoToRemember) {

        binding.date.text = response.displayDate
        binding.position.text = response.formatRunningTime()
        if (response.playPosition > 0 && response.playPosition < response.playLength) {
            binding.resumeButton.visibility = VISIBLE
            binding.startOverButton.visibility = VISIBLE
            binding.playButton.visibility = GONE
            binding.resumeButton.requestFocus()
            binding.timeleft.text = response.formatPositionAndDuration()
        } else if (response.playPosition >= response.playLength) {
            binding.resumeButton.visibility = GONE
            binding.startOverButton.visibility = GONE
            binding.playButton.visibility = GONE
            binding.watchAgain.visibility = VISIBLE
            binding.watchAgain.requestFocus()
        } else {
            binding.playButton.requestFocus()
        }

        updateUI(
            title = response.videoTitle,
            description = response.description,
            url = response.resizedURL,
            credit = response.credit,
            fallback = response.fallback
        )
    }

    private fun updateUI(
        title: String,
        description: String,
        url: String,
        fallback: String? = null,
        credit: String
    ) {
        binding.detailsContent.visibility = VISIBLE
        binding.progressBar.visibility = GONE
        binding.title.text = title
        binding.description.text = description
        binding.credit.text = credit

        Glide.with(requireContext())
            .asBitmap()
            .transform(CenterInside(), RoundedCorners(20))
            .load(url)
            .listener(object : RequestListener<Bitmap> {
                override fun onResourceReady(
                    resource: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    isFirstResource: Boolean
                ): Boolean {
                    Log.d(TAG, "onLoadFailed: Resizer failed - Check Key")
                    return false

                }
            })
            .error(
                Glide.with(requireContext())
                    .asBitmap()
                    .load(fallback)
                    .error(
                        Glide.with(requireContext())
                            .asBitmap()
                            .load(R.drawable.ic_baseline_error_24_black)
                    )
            )
            .into(binding.imageBackground)
    }

    companion object {
        private const val UUID_KEY = "uuid_key"
        private const val IS_LIVE_EVENT = "is this a live event"

        @JvmStatic
        fun newInstance(id: String) =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(UUID_KEY, id)
                }
            }
    }
}