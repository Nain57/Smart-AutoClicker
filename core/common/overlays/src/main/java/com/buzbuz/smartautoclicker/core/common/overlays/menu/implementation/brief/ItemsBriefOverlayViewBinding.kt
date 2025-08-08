
package com.buzbuz.smartautoclicker.core.common.overlays.menu.implementation.brief

import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefLandBinding
import com.buzbuz.smartautoclicker.core.common.overlays.databinding.OverlayViewActionBriefPortBinding
import com.buzbuz.smartautoclicker.core.ui.views.gesturerecord.GestureRecordView
import com.buzbuz.smartautoclicker.core.ui.views.itembrief.ItemBriefView

class ItemsBriefOverlayViewBinding private constructor(
    val root: View,
    val viewBrief: ItemBriefView,
    val viewRecorder: GestureRecordView,
    val layoutInstructions: View,
    val recordingIcon: ImageView,
    val layoutActionList: View,
    val listActions: RecyclerView,
    val textActionIndex: TextView,
    val buttonMovePrevious: Button,
    val buttonMoveNext: Button,
    val buttonDelete: Button,
    val buttonPlay: Button,
    val emptyScenarioCard: View,
    val emptyScenarioText: TextView,
) {

    companion object {

        fun inflate(inflater: LayoutInflater, orientation: Int) =
            if (orientation == Configuration.ORIENTATION_PORTRAIT)
                ItemsBriefOverlayViewBinding(OverlayViewActionBriefPortBinding.inflate(inflater))
            else
                ItemsBriefOverlayViewBinding(OverlayViewActionBriefLandBinding.inflate(inflater))
    }

    constructor(binding: OverlayViewActionBriefPortBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        viewRecorder = binding.viewRecord,
        layoutInstructions = binding.layoutInstructions,
        recordingIcon = binding.iconRecording,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonMovePrevious = binding.buttonMovePrevious,
        buttonMoveNext = binding.buttonMoveNext,
        buttonDelete = binding.buttonDelete,
        buttonPlay = binding.buttonPlayAction,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )

    constructor(binding: OverlayViewActionBriefLandBinding) : this(
        root = binding.root,
        viewBrief = binding.viewBrief,
        viewRecorder = binding.viewRecord,
        layoutInstructions = binding.layoutInstructions,
        recordingIcon = binding.iconRecording,
        layoutActionList = binding.layoutActionList,
        listActions = binding.listActions,
        textActionIndex = binding.textActionIndex,
        buttonMovePrevious = binding.buttonMovePrevious,
        buttonMoveNext = binding.buttonMoveNext,
        buttonDelete = binding.buttonDelete,
        buttonPlay = binding.buttonPlayAction,
        emptyScenarioCard = binding.emptyScenarioCard,
        emptyScenarioText = binding.textEmptyScenario,
    )
}