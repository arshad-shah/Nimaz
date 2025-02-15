package com.arshadshah.nimaz.ui.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.theme.NimazTheme
import com.arshadshah.nimaz.utils.PrivateSharedPreferences
import kotlinx.coroutines.delay
import java.time.LocalDateTime

sealed class BannerVariant {
    object Success : BannerVariant()
    object Error : BannerVariant()
    object Info : BannerVariant()
    object Warning : BannerVariant()
}

enum class BannerDuration(val value: Int) {
    SHORT(3000),
    FOREVER(-1)
}

@Composable
fun BannerSmall(
    modifier: Modifier = Modifier,
    variant: BannerVariant = BannerVariant.Info,
    title: String? = null,
    message: String? = null,
    onClick: () -> Unit = {},
    showFor: Int = BannerDuration.SHORT.value,
    paddingValues: PaddingValues? = null,
    isOpen: MutableState<Boolean> = remember { mutableStateOf(true) },
    dismissable: Boolean = false,
) {
    val sharedPref = PrivateSharedPreferences(LocalContext.current)
    val variantStyles = rememberBannerStyle(variant)

    AutoDismissBanner(isOpen, showFor, dismissable, title, sharedPref)

    if (isOpen.value) {
        ElevatedCard(
            modifier = modifier
                .padding(paddingValues ?: PaddingValues(12.dp))
                .clickable(
                    enabled = true,
                    role = Role.Button,
                    onClickLabel = "$title Banner",
                    onClick = {
                        onClick()
                        if (!dismissable) dismissBanner(isOpen, title, sharedPref)
                    }
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = variantStyles.containerColor,
                contentColor = variantStyles.contentColor
            ),

            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            BannerContent(
                variant = variantStyles,
                title = title,
                message = message,
                dismissable = dismissable,
                onDismiss = { dismissBanner(isOpen, title, sharedPref) }
            )
        }
    }
}

@Composable
private fun rememberBannerStyle(variant: BannerVariant): BannerStyle {
    val successStyle = BannerStyle(
        containerColor = Color(0xFFECFDF3),
        contentColor = Color(0xFF027A48),
        iconRes = Icons.Rounded.CheckCircle
    )
    val errorStyle = BannerStyle(
        containerColor = Color(0xFFFEF3F2),
        contentColor = Color(0xFFB42318),
        iconRes = Icons.Rounded.Error
    )
    val infoStyle = BannerStyle(
        containerColor = Color(0xFFF0F4FF),
        contentColor = Color(0xFF1849A9),
        iconRes = Icons.Rounded.Info
    )
    val warningStyle = BannerStyle(
        containerColor = Color(0xFFFFFAEB),
        contentColor = Color(0xFFB54708),
        iconRes = Icons.Rounded.Warning
    )

    return remember(variant) {
        when (variant) {
            is BannerVariant.Success -> successStyle
            is BannerVariant.Error -> errorStyle
            is BannerVariant.Info -> infoStyle
            is BannerVariant.Warning -> warningStyle
        }
    }
}

@Composable

private fun BannerContent(
    variant: BannerStyle,
    title: String?,
    message: String?,
    dismissable: Boolean,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = variant.contentColor.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = variant.iconRes,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp),
                tint = variant.contentColor
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = variant.contentColor
                )
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = variant.contentColor.copy(alpha = 0.9f)
                )
            }
        }

        if (dismissable) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = variant.contentColor.copy(alpha = 0.1f),
                onClick = onDismiss
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross_icon),
                    contentDescription = "Dismiss",
                    tint = variant.contentColor,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

fun BannerLarge(
    modifier: Modifier = Modifier,
    variant: BannerVariant = BannerVariant.Info,
    title: String,
    message: String? = null,
    onClick: () -> Unit = {},
    showFor: Int = BannerDuration.SHORT.value,
    isOpen: MutableState<Boolean>,
    onDismiss: () -> Unit,
) {
    val variantStyles = rememberBannerStyle(variant)

    AutoDismissBanner(isOpen, showFor, false, title, null)

    if (isOpen.value) {
        ElevatedCard(
            modifier = modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = variantStyles.containerColor,
                contentColor = variantStyles.contentColor
            ),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 2.dp,
                pressedElevation = 4.dp
            )
        ) {
            LargeBannerContent(
                variant = variantStyles,
                title = title,
                message = message,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun LargeBannerContent(
    variant: BannerStyle,
    title: String,
    message: String?,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = variant.contentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = variant.iconRes,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        tint = variant.contentColor
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = variant.contentColor
                )
            }
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = variant.contentColor.copy(alpha = 0.1f),
                onClick = onDismiss
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross_icon),
                    contentDescription = "Dismiss",
                    tint = variant.contentColor,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp)
                )
            }
        }

        if (!message.isNullOrEmpty()) {
            Surface(
                color = variant.contentColor.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = variant.contentColor.copy(alpha = 0.9f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoDismissBanner(
    isOpen: MutableState<Boolean>,
    showFor: Int,
    dismissable: Boolean,
    title: String?,
    sharedPref: PrivateSharedPreferences?
) {
    LaunchedEffect(Unit) {
        if (!dismissable && showFor > 0) {
            delay(showFor.toLong())
            isOpen.value = false
            if (sharedPref != null && title != null) {
                dismissBanner(isOpen, title, sharedPref)
            }
        }
    }
}

private fun dismissBanner(
    isOpen: MutableState<Boolean>,
    title: String?,
    sharedPref: PrivateSharedPreferences
) {
    isOpen.value = false
    title?.let {
        sharedPref.saveDataBoolean("$it-bannerIsOpen", false)
        sharedPref.saveData(
            "$it-bannerIsOpen-time",
            LocalDateTime.now().toString()
        )
    }
}

private data class BannerStyle(
    val containerColor: Color,
    val contentColor: Color,
    val iconRes: ImageVector
)


@Preview(
    showBackground = true,
)
@Composable
fun BannerPreviewWarning() {
    BannerSmall(
        variant = BannerVariant.Warning,
        title = "Warning",
        message = "This is a warning banner",
    )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewError() {
    BannerSmall(
        variant = BannerVariant.Error,
        title = "Error",
        message = "This is an error banner",
    )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewSuccess() {
    BannerSmall(
        variant = BannerVariant.Success,
        title = "Success",
        message = "This is a success banner",
    )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewInfo() {
    BannerSmall(
        variant = BannerVariant.Info,
        title = "Info",
        message = "This is an info banner",
    )
}

@Preview(showBackground = true)
@Composable
fun BannerPreviewInfoDismiss() {
    NimazTheme(
        darkTheme = true
    ) {
        BannerSmall(
            variant = BannerVariant.Info,
            title = "Info",
            message = "This is an info banner",
            dismissable = true,
        )
    }
}

//a dismissable banner
@Preview(showBackground = true)
@Composable
fun BannerPreviewDismissable() {
    val isOpen = remember {
        mutableStateOf(true)
    }
    NimazTheme(
        darkTheme = true
    ) {
        Column {
            BannerLarge(
                variant = BannerVariant.Success,
                title = "Success",
                message = "This is an info banner with a dismiss button and a lot of text to show how it looks when the text is too long",
                isOpen = isOpen,
                onDismiss = {
                    isOpen.value = false
                },
            )
            BannerLarge(
                variant = BannerVariant.Error,
                title = "Error",
                message = "This is an info banner with a dismiss button and a lot of text to show how it looks when the text is too long",
                isOpen = isOpen,
                onDismiss = {
                    isOpen.value = false
                },
            )
            BannerLarge(
                variant = BannerVariant.Warning,
                title = "Warning",
                message = "This is an info banner with a dismiss button and a lot of text to show how it looks when the text is too long",
                isOpen = isOpen,
                onDismiss = {
                    isOpen.value = false
                },
            )
            BannerLarge(
                variant = BannerVariant.Info,
                title = "Info",
                message = "This is an info banner with a dismiss button and a lot of text to show how it looks when the text is too long",
                isOpen = isOpen,
                onDismiss = {
                    isOpen.value = false
                },
            )
        }
    }
}