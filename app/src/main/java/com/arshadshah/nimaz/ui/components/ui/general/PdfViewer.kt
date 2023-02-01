package com.arshadshah.nimaz.ui.components.ui.general

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.net.toFile
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.net.URI

@Composable
fun PdfViewer(
	modifier : Modifier = Modifier ,
	uri : Uri ,
	verticalArrangement : Arrangement.Vertical = Arrangement.spacedBy(8.dp)
			 ) {

	val rendererScope = rememberCoroutineScope()
	val mutex = remember { Mutex() }
	val renderer by produceState<PdfRenderer?>(null , uri) {
		rendererScope.launch(Dispatchers.IO) {
			//uri is an InputStream of the pdf file so we need to use it to create a ParcelFileDescriptor
			val input = ParcelFileDescriptor.open(uri.toFile() , ParcelFileDescriptor.MODE_READ_ONLY)
			value = PdfRenderer(input)
		}
		awaitDispose {
			val currentRenderer = value
			rendererScope.launch(Dispatchers.IO) {
				mutex.withLock {
					currentRenderer?.close()
				}
			}
		}
	}
	val imageLoadingScope = rememberCoroutineScope()
	BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
		val width = with(LocalDensity.current) { maxWidth.toPx() }.toInt()
		val height = (width * kotlin.math.sqrt(2f)).toInt()
		val pageCount by remember(renderer) { derivedStateOf { renderer?.pageCount ?: 0 } }
		LazyColumn(
				verticalArrangement = verticalArrangement
				  ) {
			items(
					count = pageCount,
					key = { index -> "$uri-$index" }
				 ) { index ->
				val cacheKey = "$uri-$index"
				var bitmap by remember(cacheKey) { mutableStateOf<Bitmap?>(null) }
				if (bitmap == null) {
					DisposableEffect(uri, index) {
						val job = imageLoadingScope.launch(Dispatchers.IO) {
							val destinationBitmap = Bitmap.createBitmap(width , height , Bitmap.Config.ARGB_8888)
							mutex.withLock {
								Log.d("PdfViewer" , "Rendering page $index")
								if (!coroutineContext.isActive) return@launch
								try {
									renderer?.let {
										it.openPage(index).use { page ->
											page.render(
													destinationBitmap,
													null,
													null,
													PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
													   )
										}
									}
								} catch (e: Exception) {
									//Just catch and return in case the renderer is being closed
									return@launch
								}
							}
							bitmap = destinationBitmap
						}
						onDispose {
							job.cancel()
						}
					}
					Box(modifier = Modifier.background(Color.White).aspectRatio((1f / kotlin.math.sqrt(
							2f
																									  )).toFloat()).fillMaxWidth())
				}
			}
		}
	}
}