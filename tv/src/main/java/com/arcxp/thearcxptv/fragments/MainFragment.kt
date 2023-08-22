package com.arcxp.thearcxptv.fragments

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import com.arcxp.content.sdk.util.Failure
import com.arcxp.content.sdk.util.Success
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.FragmentMainBinding
import com.arcxp.thearcxptv.main.MainViewModel
import com.google.android.material.tabs.TabLayout
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Loads a grid of cards with movies to browse.
 */
class MainFragment : Fragment() {

    val vm: MainViewModel by sharedViewModel()

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object: OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    onBackPressedHandler()
                }
            })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        binding.pager.adapter = PagesAdapter()
        binding.tabLayout.setupWithViewPager(binding.pager)

        binding.tabLayout.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                vm.setTabPosition(tab!!.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.sectionsLoadEvent.observe(viewLifecycleOwner) {
            when (it) {
                is Success -> {
                    binding.contentLayout.visibility = VISIBLE
                    binding.loadingLayout.visibility = GONE
                }
                is Failure -> {}
            }
        }
    }

    private inner class PagesAdapter:
        FragmentPagerAdapter(childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getPageTitle(position: Int): CharSequence {
            return vm.fragmentNames[position]
        }

        override fun getCount(): Int = vm.fragmentNames.size

        override fun getItem(position: Int): Fragment {
            return vm.getFragment(position)
        }
    }

    fun onBackPressedHandler() {
        if (!vm.getCurrentFragment().onBackPressedHandler()) {
            val focusedView = requireActivity().currentFocus

            if (focusedView is TabLayout.TabView) {
                if (binding.tabLayout.selectedTabPosition != 0) {
                    binding.tabLayout[0].requestFocus()
                } else {
                    showExitDialog()
                }
            } else {
                if (vm.currentFragmentTag == MainViewModel.FragmentView.HOME) {
                    vm.getHomeFragment().scrollToTop()
                }
                focusTabLayout()
            }
        }
    }

    fun focusTabLayout() {
        binding.tabLayout.requestFocus()
    }

    private fun showExitDialog() {
        var alert: AlertDialog? = null
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        val customLayout: View = layoutInflater.inflate(R.layout.dialog_exit, null)
        alertDialog.setView(customLayout)
        val okButton: View = customLayout.findViewById(R.id.okButton)
        okButton.setOnClickListener {
            alert?.dismiss()
            requireActivity().finish()
        }
        val cancelButton: View = customLayout.findViewById(R.id.cancelButton)
        cancelButton.setOnClickListener {
            alert?.dismiss()
        }

        alert = alertDialog.create()
        alert.window?.setBackgroundDrawable(ColorDrawable(0))
        cancelButton.requestFocus()
        alert.show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = MainFragment()
    }
}