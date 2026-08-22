package com.arshadshah.nimaz.core.share

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.arshadshah.nimaz.core.ui.R
import com.arshadshah.nimaz.core.monitoring.CrashReporter
import com.arshadshah.nimaz.core.share.ContentShareManager.sendEmail
import com.arshadshah.nimaz.core.share.ContentShareManager.shareBranded
import com.arshadshah.nimaz.core.share.ContentShareManager.shareFile
import com.arshadshah.nimaz.core.share.ContentShareManager.shareText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The **single entry point** for sharing content through the system share sheet.
 * It owns everything intent-shaped that used to be copy-pasted across features:
 * `ACTION_SEND`/`ACTION_SENDTO`, MIME type, `EXTRA_SUBJECT`/`EXTRA_TEXT`, the
 * localized chooser title, and — for files — the `FileProvider` authority and read
 * grant. No screen constructs a share Intent directly.
 *
 * Map content to a [Shareable] via [Shareables], then call one of:
 * - [shareText] — the universal text path.
 * - [shareBranded] — render a branded Nimaz image (with text fallback); off-thread.
 * - [shareFile] — share an already-generated file (PDF/image) via FileProvider.
 * - [sendEmail] — open the mail app pre-filled (`mailto:`).
 */
object ContentShareManager {

    private fun authority(context: Context) = "${context.packageName}.fileprovider"

    private fun chooserTitle(context: Context) = context.getString(R.string.share_chooser_title)

    /** Share [shareable] as plain text. */
    fun shareText(context: Context, shareable: Shareable) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareable.plainText)
            shareable.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        launchChooser(context, intent)
    }

    /**
     * Share a file (PDF, PNG, …) via [FileProvider], optionally with a text caption
     * and subject. Consolidates the FileProvider + grant-flag wiring that both PDF
     * exporters used to duplicate.
     */
    fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        text: String? = null,
        subject: String? = null,
    ) {
        val uri = FileProvider.getUriForFile(context, authority(context), file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            text?.let { putExtra(Intent.EXTRA_TEXT, it) }
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchChooser(context, intent)
    }

    /**
     * Render [shareable] as a branded Nimaz card image and share it as a PNG —
     * **image only, no caption text**: the card already carries the content, the
     * branding and a "scan to install" QR, so we deliberately do not attach
     * [Shareable.plainText] (that duplicated, unstyled text is what issue #232 asked
     * us to drop). Falls back to [shareText] only when the content has no [ShareCard]
     * or rendering fails. **Suspends** — the bitmap is drawn on [Dispatchers.Default];
     * the chooser launches on the main thread. Call from a coroutine.
     *
     * @param includeText keep [Shareable.plainText] as a caption alongside the image.
     *   Off for content shares (issue #232 — image only). **On for the app invite**,
     *   where the tappable Play Store link is the whole point.
     */
    suspend fun shareBranded(context: Context, shareable: Shareable, includeText: Boolean = false) {
        val card = shareable.card
        if (card == null) {
            withContext(Dispatchers.Main) { shareText(context, shareable) }
            return
        }
        val file = withContext(Dispatchers.Default) {
            runCatching { ShareCardRenderer.renderToCache(context, card) }
                .onFailure { CrashReporter.recordException(it) }
                .getOrNull()
        }
        withContext(Dispatchers.Main) {
            if (file != null) {
                shareFile(
                    context,
                    file,
                    mimeType = "image/png",
                    text = shareable.plainText.takeIf { includeText },
                )
            } else {
                shareText(context, shareable)
            }
        }
    }

    /**
     * Open the user's email app pre-filled. Uses `ACTION_SENDTO` with a `mailto:`
     * URI so only email apps resolve it (no chooser needed).
     */
    fun sendEmail(context: Context, address: String, subject: String? = null) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:$address".toUri()
            subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        }
        runCatching { context.startActivity(intent) }
            .onFailure { CrashReporter.recordException(it) }
    }

    private fun launchChooser(context: Context, intent: Intent) {
        runCatching {
            context.startActivity(Intent.createChooser(intent, chooserTitle(context)))
        }.onFailure { CrashReporter.recordException(it) }
    }
}
