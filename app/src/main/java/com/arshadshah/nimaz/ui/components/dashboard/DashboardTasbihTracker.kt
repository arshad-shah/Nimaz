package com.arshadshah.nimaz.ui.components.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.data.local.models.LocalTasbih
import com.arshadshah.nimaz.ui.components.common.DropDownHeader
import com.arshadshah.nimaz.ui.components.common.DropdownPlaceholder
import com.arshadshah.nimaz.ui.components.common.FeaturesDropDown
import com.arshadshah.nimaz.ui.components.tasbih.DeleteDialog
import com.arshadshah.nimaz.ui.components.tasbih.GoalEditDialog
import com.arshadshah.nimaz.ui.components.tasbih.TasbihDropdownItem
import com.arshadshah.nimaz.viewModel.DashboardViewmodel
import java.time.LocalDate
import kotlin.reflect.KFunction1

@Composable
fun DashboardTasbihTracker(
    onNavigateToTasbihScreen: (String, String, String, String) -> Unit,
    onNavigateToTasbihListScreen: () -> Unit,
    tasbihList: List<LocalTasbih>,
    handleEvents: KFunction1<DashboardViewmodel.DashboardEvent, Unit>,
    isLoading: State<Boolean>,

    ) {

    if (tasbihList.isEmpty()) {
        Box(
            modifier = Modifier.clickable {
                onNavigateToTasbihListScreen()
            }
        ) {
            DropdownPlaceholder(text = "No Tasbih found")
        }
    } else {
        val showTasbihDialog = remember {
            mutableStateOf(false)
        }
        val showDeleteDialog = remember {
            mutableStateOf(false)
        }
        val tasbihToEdit = remember {
            mutableStateOf(
                LocalTasbih(
                    0,
                    LocalDate.now(),
                    "",
                    "",
                    "",
                    0,
                    0,
                )
            )
        }

        FeaturesDropDown(
            modifier = Modifier.padding(4.dp),
            //the list of tasbih for the date at the index
            items = tasbihList,
            label = "Tasbih",
            dropDownItem = {
                TasbihDropdownItem(
                    it,
                    onClick = { tasbih ->
                        onNavigateToTasbihScreen(
                            tasbih.id.toString(),
                            tasbih.arabicName,
                            tasbih.englishName,
                            tasbih.translationName
                        )
                    },
                    onDelete = { tasbih ->
                        showDeleteDialog.value = true
                        tasbihToEdit.value = tasbih
                    }
                ) { tasbih ->
                    showTasbihDialog.value =
                        true
                    tasbihToEdit.value =
                        tasbih
                }
            }

        )

        GoalEditDialog(tasbihToEdit.value, showTasbihDialog)
        DeleteDialog(tasbihToEdit.value, showDeleteDialog)
    }
}