package com.arcxp.thearcxptv.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arc.arcvideo.ArcXPVideoSDK
import com.arcxp.commerce.ArcXPCommerceSDK
import com.arcxp.commerce.apimanagers.ArcXPIdentityListener
import com.arcxp.commerce.models.ArcXPProfileManage
import com.arcxp.commerce.util.ArcXPError
import com.arcxp.content.sdk.ArcXPContentSDK
import com.arcxp.content.sdk.models.ArcXPContentError
import com.arcxp.thearcxptv.BaseFragmentInterface
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.databinding.FragmentSettingsBinding
import com.arcxp.thearcxptv.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel


class SettingsFragment : Fragment(), BaseFragmentInterface {

    val vm: MainViewModel by sharedViewModel()

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

//    private var changePasswordFragment = ChangePasswordFragment()
//    private var createAccountFragment = CreateAccountFragment()
//    private val loginFragment = LoginFragment()

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

        if (!ArcXPCommerceSDK.isInitialized()) {
            binding.header.visibility = GONE
            binding.loginLayout.visibility = GONE
            binding.createLayout.visibility = GONE

        } else {
            vm.isLoggedIn().observe(viewLifecycleOwner) {
                when (it) {
                    true -> getUserData()
                    else -> {}
                }
            }

            binding.loginLayout.setOnClickListener {
                if (ArcXPCommerceSDK.commerceManager().sessionIsActive()) {
                    vm.logout(object : ArcXPIdentityListener() {
                        override fun onLogoutSuccess() {
                            binding.usernameTv.visibility = GONE
                            binding.loginTv.text = getString(R.string.login)
                            binding.createAccountTv.text = getString(R.string.create_account)
                        }

                        override fun onLogoutError(error: ArcXPError) {
//                            requireActivity().showErrorDialog(
//                                error.type?.name!!,
//                                error.localizedMessage
//                            )
                        }
                    })
                } else {
//                    (requireActivity() as MainActivity).openFragment(
//                        loginFragment,
//                        true,
//                        getString(R.string.login)
//                    )
                }
            }

            if (!ArcXPCommerceSDK.commerceManager().sessionIsActive()) {
                vm.logout(object : ArcXPIdentityListener() {
                    override fun onLogoutSuccess() {}
                })
            }

//            binding.createLayout.setOnClickListener {
//                if (ArcXPCommerceSDK.commerceManager().sessionIsActive()) {
//                    (requireActivity() as MainActivity).openFragment(
//                        changePasswordFragment,
//                        true,
//                        getString(R.string.change_password)
//                    )
//                } else {
//                    (requireActivity() as MainActivity).openFragment(
//                        createAccountFragment,
//                        true,
//                        getString(R.string.create_account)
//                    )
//                }
//            }
        }

        binding.contentSdkVersion.text =
            "Content: ${ArcXPContentSDK.getVersion(requireContext())}"
        binding.commerceSdkVersion.text =
            "Commerce: ${ArcXPCommerceSDK.getVersion(requireContext())}"
        binding.videoSdkVersion.text = "Video: ${ArcXPVideoSDK.getVersion(requireContext())}"

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

    private fun getUserData() {
        vm.getUserProfile(object : ArcXPIdentityListener() {
            override fun onFetchProfileSuccess(profileResponse: ArcXPProfileManage) {
                showProfile(profileResponse)
            }

            override fun onProfileError(error: ArcXPError) {
//                requireActivity().showErrorDialog(
//                    error.type?.name!!,
//                    error.localizedMessage
//                )
            }
        })
    }

    private fun showProfile(arcXPProfileManage: ArcXPProfileManage) {
        binding.loginTv.text = getString(R.string.logout)
        binding.createAccountTv.text = getString(R.string.change_password)
        binding.usernameTv.text = "${arcXPProfileManage.firstName} ${arcXPProfileManage.lastName}"
        binding.usernameTv.visibility = VISIBLE
    }

    private fun onError(error: ArcXPContentError) {
//        showSnackBar(
//            error = error,
//            view = binding.root,
//            viewId = R.id.account_frag
//        )
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

    override fun onBackPressedHandler(): Boolean {
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