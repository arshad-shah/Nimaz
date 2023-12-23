package com.arshadshah.nimaz.ui.components.tasbih

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import es.dmoral.toasty.Toasty

@Composable
fun Editbutton(
    count: MutableState<Int>,
    context: Context,
    showObjectiveDialog: MutableState<Boolean>,
    objective: MutableState<String>,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ElevatedButton(
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onSurface,
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.38f),
            ),
            onClick = {
                //if the tasbih count is greater then show toast saying that the tasbih count must be 0 to edit the objective
                if (count.value > 0) {
                    Toasty.info(
                        context,
                        "Objective can only be changed when the tasbih count is 0",
                        Toasty.LENGTH_SHORT
                    ).show()
                } else {
                    showObjectiveDialog.value = true
                }
            }) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = objective.value,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 26.sp
                )
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.edit_icon),
                    contentDescription = "Edit"
                )
            }
        }
    }
}