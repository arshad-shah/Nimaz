package com.arshadshah.nimaz.presentation.components.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.core.common.ThematicLink
import com.arshadshah.nimaz.core.common.ThematicMarkup
import com.arshadshah.nimaz.presentation.theme.NimazTheme

/**
 * Renders the thematic layer's four-tag markup — the only place in the app that reads it.
 *
 * One `Text` per paragraph rather than one with embedded newlines, so paragraph spacing is
 * layout instead of blank lines, and a long section is a column of independently measured
 * blocks rather than a single 40 KB string.
 *
 * The links are the reason this is a component and not a `Text` with `AnnotatedString.fromHtml`.
 * A `quran:` reference is a *destination in this app*, not a URL: it opens the reader at the
 * verses the sentence is talking about, and a `topic:` reference opens that subject. Both are
 * delivered as [ThematicLink] so the caller decides navigation and this stays presentation.
 *
 * Parsing is `remember`ed on the raw markup. Without it, every recomposition of an expanded
 * section re-scans its text — and the longest of these is most of a printed page.
 */
@Composable
fun ThematicText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    onLinkClick: ((ThematicLink) -> Unit)? = null,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
        ),
        pressedStyle = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )

    val paragraphs = remember(html, onLinkClick, linkStyles) {
        ThematicMarkup.parse(html).map { paragraph ->
            buildAnnotatedString {
                paragraph.spans.forEach { span ->
                    val link = span.link
                    if (link != null && onLinkClick != null) {
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = link.tag,
                                styles = linkStyles,
                                linkInteractionListener = { onLinkClick(link) },
                            )
                        ) { appendStyled(span.text, span.bold, span.italic) }
                    } else {
                        appendStyled(span.text, span.bold, span.italic)
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        paragraphs.forEach { paragraph ->
            Text(
                text = paragraph,
                style = style,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
            )
        }
    }
}

private fun AnnotatedString.Builder.appendStyled(text: String, bold: Boolean, italic: Boolean) {
    if (!bold && !italic) {
        append(text)
        return
    }
    withStyle(
        SpanStyle(
            fontWeight = if (bold) FontWeight.SemiBold else null,
            fontStyle = if (italic) FontStyle.Italic else null,
        )
    ) { append(text) }
}

/**
 * A stable identifier for the destination, used as the annotation tag.
 *
 * `LinkAnnotation.Clickable` compares by tag as well as by listener, so two links to different
 * verses in the same paragraph must not share one.
 */
private val ThematicLink.tag: String
    get() = when (this) {
        is ThematicLink.Verses -> "quran:$surah:${from ?: 0}-${to ?: 0}"
        is ThematicLink.Topic -> "topic:$id"
    }

@Preview(showBackground = true)
@Composable
private fun ThematicTextPreview() {
    NimazTheme {
        ThematicText(
            html = "<p>This Surah is named <strong>Al-Fatihah</strong> because of its " +
                    "subject matter — see <a href=\"quran:1:1-7\">the opening verses</a>.</p>" +
                    "<p>It is <em>a prayer</em> that Allah has taught to all those who want to " +
                    "make a study of His book.</p>",
            onLinkClick = {},
        )
    }
}
