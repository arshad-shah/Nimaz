package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arshadshah.nimaz.data.local.models.LocalTasbih

@Composable
//function to open the dialog with the tasbih data
fun GoalEditDialog(
    tasbih: LocalTasbih,
    showTasbihDialog: MutableState<Boolean>,
    onUpdateTasbih: (String) -> Unit,
) {
    val context = LocalContext.current

    val goal = remember {
        mutableStateOf(tasbih.goal.toString())
    }
    goal.value = tasbih.goal.toString()
    TasbihGoalDialog(
        onConfirm = {
            onUpdateTasbih(goal.value)
            //save the objective
            context.getSharedPreferences("tasbih", 0).edit()
                .putString("objective-${tasbih.id}", it)
                .apply()
        },
        isOpen = showTasbihDialog,
        state = goal
    )
}