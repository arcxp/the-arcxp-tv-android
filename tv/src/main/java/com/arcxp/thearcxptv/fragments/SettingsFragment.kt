package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arcxp.ArcXPMobileSDK
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.FragmentSettingsBinding
import com.arcxp.thearcxptv.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class SettingsFragment : Fragment(), BaseFragmentInterface {

    val vm: MainViewModel by sharedViewModel()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private var lastFocus: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.sdkVersion.text = getString(R.string.sdk_colon_version, ArcXPMobileSDK.getVersion(requireContext().applicationContext))

        binding.tosButton.setOnClickListener {
            openFragment(
                WebFragment().openUrl(
                    getString(R.string.tos_url),
                    getString(R.string.terms_of_service)
                ), getString(R.string.terms_of_service)
            )
            lastFocus = binding.tosButton
        }

        binding.ppButton.setOnClickListener {
            openFragment(
                WebFragment().openUrl(
                    getString(R.string.pp_url),
                    getString(R.string.privacy_policy)
                ), getString(R.string.privacy_policy)
            )
            lastFocus = binding.ppButton
        }
    }

    private fun openFragment(
        fragment: Fragment,
        tag: String,
    ) {
        requireActivity().supportFragmentManager
            .beginTransaction()
            .add(R.id.mainFragment, fragment, tag)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun isOnBackPressed(): Boolean {
        val visibleFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.mainFragment)

        return if (visibleFragment is WebFragment) {
            requireActivity().supportFragmentManager.popBackStack()
            lastFocus?.requestFocus()
            true
        } else {
            false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}