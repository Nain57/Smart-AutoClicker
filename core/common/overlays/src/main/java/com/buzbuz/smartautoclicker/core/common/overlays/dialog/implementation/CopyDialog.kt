
package com.buzbuz.smartautoclicker.core.common.overlays.dialog.implementation

import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.annotation.StyleRes

import com.buzbuz.smartautoclicker.core.common.overlays.databinding.DialogBaseCopyBinding
import com.buzbuz.smartautoclicker.core.common.overlays.dialog.OverlayDialog
import com.buzbuz.smartautoclicker.core.ui.bindings.lists.setEmptyText
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setOnDismissClickedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setOnTextChangedListener
import com.buzbuz.smartautoclicker.core.ui.bindings.dialogs.setup

abstract class CopyDialog(
    @StyleRes theme: Int,
) : OverlayDialog(theme) {

    /** ViewBinding containing the views for this dialog. */
    protected lateinit var viewBinding: DialogBaseCopyBinding
    /** The resource id for the dialog title. */
    protected abstract val titleRes: Int
    /** The resource id for the search hint text. */
    protected abstract val searchHintRes: Int
    /** The resource id for the text displayed when there is nothing to copy. */
    protected abstract val emptyRes: Int

    final override fun onCreateView(): ViewGroup {
        viewBinding = DialogBaseCopyBinding.inflate(LayoutInflater.from(context)).apply {
            layoutTopBar.apply {
                setup(titleRes, searchHintRes)
                setOnDismissClickedListener { debounceUserInteraction { back() } }
                setOnTextChangedListener(::onSearchQueryChanged)
            }

            layoutLoadableList.setEmptyText(emptyRes)
        }

        return viewBinding.root
    }

    abstract fun onSearchQueryChanged(newText: String?)
}
