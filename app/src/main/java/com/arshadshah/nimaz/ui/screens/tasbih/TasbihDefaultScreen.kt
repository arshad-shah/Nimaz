package com.arshadshah.nimaz.ui.screens.tasbih

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.ButtonElevation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.BottomSheetForTasbihList
import com.arshadshah.nimaz.ui.components.bLogic.tasbih.Counter
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.ui.theme.quranFont
import compose.icons.FeatherIcons
import compose.icons.feathericons.*
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@OptIn( ExperimentalMaterialApi::class)
@Composable
fun TasbihDefaultScreen(paddingValues : PaddingValues)
{
	val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
			bottomSheetState = BottomSheetState(BottomSheetValue.Collapsed)
																   )

	BottomSheetForTasbihList(bottomSheetScaffoldState,paddingValues)
}