package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import com.arshadshah.nimaz.ui.components.common.NimazTextField
import com.arshadshah.nimaz.ui.components.common.NimazTextFieldType
import androidx.compose.foundation.text.KeyboardOptions
import es.dmoral.toasty.Toasty

@Composable
fun TasbihGoalDialog(
    onConfirm: (String) -> Unit,
    isOpen: MutableState<Boolean>,
    state: MutableState<String>,
) {
    val context = LocalContext.current
    if (!isOpen.value) return

    AlertDialogNimaz(
        cardContent = false,
        bottomDivider = false,
        topDivider = false,
        contentHeight = 120.dp,
        contentDescription = "Goal for tasbih",
        title = "Set Daily Goal",
        contentToShow = {
            NimazTextField(
                value = state.value,
                onValueChange = { state.value = it },
                type = NimazTextFieldType.NUMBER,
                label = "Daily Goal",
                placeholder = "Enter target count",
                requestFocus = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val isInt = state.value.toIntOrNull()
                        if (isInt != null && state.value != "" && state.value.toInt() > 0) {
                            onConfirm(state.value)
                            isOpen.value = false
                        } else {
                            Toasty.error(context, "Goal must be greater than 0", Toasty.LENGTH_SHORT).show()
                        }
                    }
                )
            )
        },
        onDismissRequest = { isOpen.value = false },
        onConfirm = {
            val isInt = state.value.toIntOrNull()
            if (isInt != null && state.value != "" && state.value.toInt() > 0) {
                onConfirm(state.value)
                isOpen.value = false
            } else {
                Toasty.error(context, "Goal must be greater than 0", Toasty.LENGTH_SHORT).show()
            }
        },
        onDismiss = { isOpen.value = false }
    )
}