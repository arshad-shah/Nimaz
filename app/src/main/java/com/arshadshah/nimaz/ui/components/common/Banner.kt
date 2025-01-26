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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.arshadshah.nimaz.R
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
                .padding(paddingValues ?: PaddingValues(8.dp))
                .clickable(
                    enabled = true,
                    role = Role.Button,
                    onClickLabel = "$title Banner",
                    onClick = {
                        onClick()
                        if (!dismissable) dismissBanner(isOpen, title, sharedPref)
                    }
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = variantStyles.containerColor,
                contentColor = variantStyles.contentColor
            ),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
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
        containerColor = Color(0xFFE7F6EC),
        contentColor = Color(0xFF18794E),
        iconRes = R.drawable.checkbox_icon
    )
    val errorStyle = BannerStyle(
        containerColor = Color(0xFFFFEEEE),
        contentColor = Color(0xFFD92D20),
        iconRes = R.drawable.cross_circle_icon
    )
    val infoStyle = BannerStyle(
        containerColor = Color(0xFFEEF4FF),
        contentColor = Color(0xFF1570EF),
        iconRes = R.drawable.info_icon
    )
    val warningStyle = BannerStyle(
        containerColor = Color(0xFFFEF4E6),
        contentColor = Color(0xFFB93815),
        iconRes = R.drawable.warning_icon
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
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = variantStyles.containerColor,
                contentColor = variantStyles.contentColor
            ),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
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
        Icon(
            painter = painterResource(variant.iconRes),
            contentDescription = null,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp),
            tint = variant.contentColor
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = variant.contentColor
                )
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (dismissable) {
            OutlinedIconButton(
                modifier = Modifier.size(32.dp),
                onClick = onDismiss
            ) {
                Icon(
                    painter = painterResource(R.drawable.cross_icon),
                    contentDescription = "Dismiss",
                    tint = variant.contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
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
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(variant.iconRes),
                    contentDescription = null,
                    tint = variant.contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = variant.contentColor
                )
            }
            OutlinedIconButton(modifier = Modifier.size(32.dp), onClick = onDismiss) {
                Icon(
                    painter = painterResource(R.drawable.cross_icon),
                    contentDescription = "Dismiss",
                    tint = variant.contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (!message.isNullOrEmpty()) {
            Surface(
                color = variant.contentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = variant.contentColor,
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
    val iconRes: Int
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
    BannerSmall(
        variant = BannerVariant.Info,
        title = "Info",
        message = "This is an info banner",
        dismissable = true,
    )
}

//a dismissable banner
@Preview(showBackground = true)
@Composable
fun BannerPreviewDismissable() {
    val isOpen = remember {
        mutableStateOf(true)
    }
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