package com.arshadshah.nimaz.ui.components.tasbih

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.arshadshah.nimaz.constants.AppConstants
import com.arshadshah.nimaz.data.remote.models.Tasbih
import com.arshadshah.nimaz.viewModel.TasbihViewModel
import java.time.LocalDate

@Composable
//function to open the dialog with the tasbih data
fun GoalEditDialog(tasbih: Tasbih, showTasbihDialog: MutableState<Boolean>) {
    val context = LocalContext.current

    val goal = remember {
        mutableStateOf(tasbih.goal.toString())
    }
    goal.value = tasbih.goal.toString()

    val viewModel = viewModel(
        key = AppConstants.TASBIH_VIEWMODEL_KEY,
        initializer = { TasbihViewModel(context) },
        viewModelStoreOwner = LocalContext.current as ComponentActivity
    )
    TasbihGoalDialog(
        onConfirm = {
            viewModel.handleEvent(
                TasbihViewModel.TasbihEvent.UpdateTasbihGoal(
                    Tasbih(
                        id = tasbih.id,
                        arabicName = tasbih.arabicName,
                        englishName = tasbih.englishName,
                        translationName = tasbih.translationName,
                        goal = it.toInt(),
                        count = tasbih.count,
                        date = LocalDate.now().toString(),
                    )
                )
            )
            //save the objective
            context.getSharedPreferences("tasbih", 0).edit()
                .putString("objective-${tasbih.id}", it)
                .apply()
        },
        isOpen = showTasbihDialog,
        state = goal
    )
}