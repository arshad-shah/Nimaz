package com.arshadshah.nimaz.core.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * The one place the app builds a share `Intent`, asserted on the intent it actually starts.
 *
 * `ContentShareManager` exists because `ACTION_SEND`, the MIME type, the chooser title and the
 * `mailto:` wiring were copy-pasted across features. The failures it prevents are ones nobody
 * debugging Nimaz would ever see: a share that resolves to the wrong set of apps, or a subject
 * that never arrives. So the assertions here are on the extras and the action, not on a screen.
 *
 * Three things about the harness are worth stating, because each was a wrong diagnosis first.
 *
 * The context is an **Activity**, not the Application. `launchChooser` wraps its `startActivity`
 * in `runCatching`, and starting an activity from an application context without
 * `FLAG_ACTIVITY_NEW_TASK` throws — which that `runCatching` swallows into `CrashReporter`. The
 * test then records no intent at all and reads as "the manager did nothing", which is the wrong
 * diagnosis of a right guard.
 *
 * `shareBranded` hops to `Dispatchers.Main` to launch the chooser, and under Robolectric that is
 * the real main looper — which the test thread is already blocking inside `runTest`, so the posted
 * continuation never runs and the test **hangs** rather than fails. Substituting the Main
 * dispatcher makes the hop run inline.
 *
 * **`shareFile`'s `FileProvider` lookup is not reachable from a library unit test.** The authority
 * is `<packageName>.fileprovider`, declared in `:app`'s manifest against `res/xml/file_paths`; a
 * library's own test manifest has neither, so `getUriForFile` throws before an intent exists. That
 * path is exercised by `:app`'s instrumented suite. Registering a stub provider here would pin the
 * test's own scaffolding rather than the app's wiring.
 */
@RunWith(RobolectricTestRunner::class)
class ContentShareManagerTest {

    /** An activity context, so the chooser's `startActivity` is legal. */
    private val activity: Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    private val context: Context get() = activity

    @Before
    fun substituteMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    /** The `ACTION_SEND` inside the chooser the manager actually launched. */
    private fun lastShareIntent(): Intent {
        val chooser = shadowOf(activity).nextStartedActivity
        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        return requireNotNull(
            @Suppress("DEPRECATION")
            chooser.getParcelableExtra(Intent.EXTRA_INTENT) as Intent?
        )
    }

    @Test
    fun `a text share sends the body as plain text`() {
        ContentShareManager.shareText(context, Shareable(plainText = "Ayat al-Kursi"))

        val intent = lastShareIntent()
        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.type).isEqualTo("text/plain")
        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("Ayat al-Kursi")
        assertThat(intent.hasExtra(Intent.EXTRA_SUBJECT)).isFalse()
    }

    @Test
    fun `a subject travels only when the shareable has one`() {
        ContentShareManager.shareText(
            context,
            Shareable(plainText = "June times", subject = "Nimaz export"),
        )

        assertThat(lastShareIntent().getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Nimaz export")
    }

    @Test
    fun `a share always goes through a chooser rather than straight to one app`() {
        // `Intent.createChooser` with a localized title. Starting the `ACTION_SEND` directly would
        // send to whichever app the system last defaulted to, with no way back.
        ContentShareManager.shareText(context, Shareable(plainText = "anything"))

        val chooser = shadowOf(activity).nextStartedActivity
        assertThat(chooser.action).isEqualTo(Intent.ACTION_CHOOSER)
        assertThat(chooser.getCharSequenceExtra(Intent.EXTRA_TITLE)).isNotNull()
    }

    @Test
    fun `a branded share with no card falls back to text`() = runTest {
        // The `card == null` arm — content where an image adds nothing (a bookmark, an invite by
        // mail) must still share, and must not silently do nothing.
        ContentShareManager.shareBranded(context, Shareable(plainText = "No card here"))

        val intent = lastShareIntent()
        assertThat(intent.type).isEqualTo("text/plain")
        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo("No card here")
    }

    @Test
    fun `an email opens the mail app rather than a chooser`() {
        // `ACTION_SENDTO` with a `mailto:` URI, so only email apps resolve it. A plain
        // `ACTION_SEND` here would put every messaging app in front of the user instead.
        ContentShareManager.sendEmail(context, "help@example.com", subject = "Feedback")

        val intent = shadowOf(activity).nextStartedActivity
        assertThat(intent.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(intent.data.toString()).isEqualTo("mailto:help@example.com")
        assertThat(intent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("Feedback")
    }

    @Test
    fun `an email may carry no subject`() {
        ContentShareManager.sendEmail(context, "help@example.com")

        val intent = shadowOf(activity).nextStartedActivity
        assertThat(intent.hasExtra(Intent.EXTRA_SUBJECT)).isFalse()
    }
}
