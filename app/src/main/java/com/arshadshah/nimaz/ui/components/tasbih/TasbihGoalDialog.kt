package com.arshadshah.nimaz.ui.components.tasbih

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.ui.components.common.AlertDialogNimaz
import es.dmoral.toasty.Toasty

@OptIn(ExperimentalMaterial3Api::class)
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
        contentHeight = 100.dp,
        contentDescription = "Goal for tasbih",
        title = "Set Daily Goal",
        contentToShow = {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OutlinedTextField(
                    shape = MaterialTheme.shapes.extraLarge,
                    value = state.value,
                    maxLines = 1,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val isInt = state.value.toIntOrNull()
                            if (isInt != null && state.value != "") {
                                if (state.value.toInt() > 0) {
                                    onConfirm(state.value)
                                    isOpen.value = false
                                } else {
                                    Toasty
                                        .error(
                                            context,
                                            "Goal must be greater than 0",
                                            Toasty.LENGTH_SHORT
                                        )
                                        .show()
                                }
                            } else {
                                Toasty
                                    .error(
                                        context,
                                        "Goal must be greater than 0",
                                        Toasty.LENGTH_SHORT
                                    )
                                    .show()
                            }
                        }),
                    onValueChange = {
                        state.value = it
                    },
                    label = { Text(text = "Daily Goal") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                )
            }
        },
        onDismissRequest = {
            isOpen.value = false
        },
        onConfirm = {
            val isInt = state.value.toIntOrNull()
            if (isInt != null && state.value != "") {
                if (state.value.toInt() > 0) {
                    onConfirm(state.value)
                    isOpen.value = false
                } else {
                    Toasty
                        .error(
                            context,
                            "Goal must be greater than 0",
                            Toasty.LENGTH_SHORT
                        )
                        .show()
                }
            } else {
                Toasty
                    .error(
                        context,
                        "Goal must be greater than 0",
                        Toasty.LENGTH_SHORT
                    )
                    .show()
            }

        },
        onDismiss = {
            isOpen.value = false
        })
}