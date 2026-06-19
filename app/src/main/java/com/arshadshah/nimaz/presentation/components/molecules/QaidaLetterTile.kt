package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.domain.model.QaidaLetter
import com.arshadshah.nimaz.presentation.components.atoms.ArabicText
import com.arshadshah.nimaz.presentation.components.atoms.ArabicTextSize
import com.arshadshah.nimaz.presentation.theme.NimazCornerRadius

/**
 * One letter on the alphabet board. A square card with the big Arabic letter;
 * a gold star in the corner marks letters the child has already heard.
 */
@Composable
fun QaidaLetterTile(
    letter: QaidaLetter,
    heard: Boolean,
    onClick: (QaidaLetter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val desc = "Letter ${letter.nameTransliteration}" + if (heard) ", heard" else ""
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = desc }
            .clickable { onClick(letter) },
        shape = RoundedCornerShape(NimazCornerRadius.Large),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            ArabicText(
                text = letter.letterArabic,
                size = ArabicTextSize.EXTRA_LARGE,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (heard) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(14.dp),
                )
            }
        }
    }
}
