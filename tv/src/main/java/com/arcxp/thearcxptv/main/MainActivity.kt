package com.arcxp.thearcxptv.main

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.ActivityMainBinding
import com.arcxp.thearcxptv.fragments.DetailsFragment
import com.arcxp.thearcxptv.fragments.MainFragment
import com.arcxp.thearcxptv.fragments.PlayVideoFragment
import com.arcxp.thearcxptv.utils.collectOneTimeEvent
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Loads [MainFragment].
 */
class MainActivity : FragmentActivity() {

    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModel()

    private val mainFragment = MainFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)

        supportFragmentManager.beginTransaction().add(R.id.mainFragment, mainFragment).commit()

        collectOneTimeEvent(flow = vm.openVideoEvent, collect = ::openVideo)

        collectOneTimeEvent(flow = vm.openDetailsEvent, collect = ::openDetails)

        collectOneTimeEvent(flow = vm.openVirtualVideoEvent, collect = ::openVirtualChannel)
    }

    private fun openVideo(pair: Pair<String, Int>) =
        replaceMainFragment(
            fragment = PlayVideoFragment.newInstance(
                uuid = pair.first,
                startPosition = pair.second
            )
        )


    private fun openDetails(uuid: String) =
        replaceMainFragment(
            fragment = DetailsFragment.newInstance(
                id = uuid
            )
        )


    private fun openVirtualChannel(ignored: Boolean) =
        replaceMainFragment(
            fragment = PlayVideoFragment.newInstance(
                uuid = getString(R.string.virtual_channel_uuid),
                isVirtualChannel = true
            )
        )


    private fun replaceMainFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.mainFragment, fragment)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    override fun onStop() {
        super.onStop()
        vm.cleanupVideoOnClose()
    }

    /**
     * This method is required to pass key events to the player
     * when running on a Fire TV device.  It is not required for
     * Android TV devices.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (android.os.Build.MODEL.contains("AFT")) {
            if (!vm.onKeyEvent(event)) {
                return super.onKeyUp(keyCode, event)
            }
        }
        return super.onKeyUp(keyCode, event)
    }
}