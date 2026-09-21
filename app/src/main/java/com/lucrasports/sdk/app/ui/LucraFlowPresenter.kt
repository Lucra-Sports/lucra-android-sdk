package com.lucrasports.sdk.app.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.lucrasports.sdk.core.LucraClient
import com.lucrasports.sdk.core.ui.LucraUiProvider.LucraFlow
import java.lang.ref.WeakReference

/**
 * Presents Lucra flows, and dismisses them from whichever screen presented them.
 *
 * The SDK asks the host to dismiss a flow through one `LucraFlowListener`, registered
 * once at startup — but a DialogFragment shown in one activity's FragmentManager is
 * invisible to another's. Without routing, a flow presented from any screen other than
 * the listener's owner can never be dismissed: the X on a Lucra screen fires
 * `onLucraExit`, the lookup finds nothing, and the flow just sits there.
 *
 * A real integration with more than one entry point needs the same routing.
 */
internal object LucraFlowPresenter {

    /**
     * Weak on purpose: this outlives every activity it hands out, and holding a strong
     * reference would leak the last one that presented a flow.
     */
    private var presenter: WeakReference<AppCompatActivity>? = null

    fun present(activity: AppCompatActivity, flow: LucraFlow) {
        presenter = WeakReference(activity)
        LucraClient().getLucraDialogFragment(flow)
            .show(activity.supportFragmentManager, flow.toString())
    }

    /**
     * @param listenerOwner the FragmentManager of the activity holding the
     *   `LucraFlowListener`, tried second so flows presented without going through
     *   [present] still resolve.
     * @return true if a fragment was found and dismissed.
     */
    fun dismiss(flow: LucraFlow, listenerOwner: FragmentManager): Boolean =
        candidateManagers(listenerOwner).any { it.dismissFlow(flow) }

    private fun candidateManagers(listenerOwner: FragmentManager): List<FragmentManager> =
        listOfNotNull(
            presenter?.get()
                ?.takeIf { !it.isFinishing && !it.isDestroyed }
                ?.supportFragmentManager,
            listenerOwner,
        ).distinct()

    /**
     * State-loss tolerant: the SDK can request a dismiss after the host activity has
     * run `onSaveInstanceState`, and the plain `dismiss()`/`commit()` variants throw
     * `IllegalStateException` in that window. The dialog is transient UI that should
     * not survive the restore anyway, so dropping it from the saved state is correct.
     */
    private fun FragmentManager.dismissFlow(flow: LucraFlow): Boolean {
        val fragment = findFragmentByTag(flow.toString()) ?: return false
        if (fragment is DialogFragment) {
            fragment.dismissAllowingStateLoss()
        } else {
            beginTransaction().remove(fragment).commitAllowingStateLoss()
        }
        return true
    }
}
