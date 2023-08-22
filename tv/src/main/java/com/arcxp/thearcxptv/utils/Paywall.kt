package com.arcxp.thearcxptv.utils

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.arcxp.thearcxptv.R
import com.arcxp.thearcxptv.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

/**
 * Dialog to show paywall.  This dialog can either be dismissed or it can take the
 * user to the sign in/sign up flow.
 */
class Paywall : DialogFragment() {

    //Listener to indicate if the dialog was cancelled or if the registration process
    //was invoked.  This way the calling fragment can know how to respond.
    var cancelListener: OnPaywallCancelledListener? = null
    var cancel = true
    private val vm: MainViewModel by sharedViewModel()

    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        return activity?.let {
            val bottomSheetDialog = BottomSheetDialog(requireActivity())
            val inflater = requireActivity().layoutInflater
            val paywallView = inflater.inflate(R.layout.fragment_paywall, null)
            val subscribe = paywallView.findViewById<Button>(R.id.subscribe_btn)
            val signIn = paywallView.findViewById<TextView>(R.id.sign_in)
            val exit = paywallView.findViewById<ImageView>(R.id.exit)
            bottomSheetDialog.setContentView(paywallView)
            bottomSheetDialog.setCanceledOnTouchOutside(false)
            exit.setOnClickListener {
                cancel = false
                vm.disposeVideoPlayer()
                dismiss()
            }
            subscribe.setOnClickListener {
                cancel = false
                dismiss()
                //triggerPaywall
//                (activity as MainActivity).openFragment(
//                    CreateAccountFragment(), true,
//                    tag = getString(R.string.create_account)
//                )
            }
            signIn.setOnClickListener {
                cancel = false
                dismiss()
//                (requireActivity() as MainActivity).navigateToSignIn()
            }
            bottomSheetDialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (cancelListener != null && cancel) {
            cancelListener?.onPaywallCancel()
        }
        requireFragmentManager().popBackStack()
    }

    fun setOnPaywallCancelledListener(listener: OnPaywallCancelledListener) {
        cancelListener = listener
    }

    interface OnPaywallCancelledListener {
        fun onPaywallCancel()
    }

}