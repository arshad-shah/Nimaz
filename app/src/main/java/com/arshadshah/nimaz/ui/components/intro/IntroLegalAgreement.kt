package com.arshadshah.nimaz.ui.components.intro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.utils.FirebaseLogger
import com.arshadshah.nimaz.viewModel.IntroductionViewModel

@Composable
fun IntroLegalAgreement(
    navController: NavController,
    viewModel: IntroductionViewModel = hiltViewModel()
) {
    // Get state from ViewModel
    val legalState by viewModel.legalSettingsState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    // Get Firebase Logger from ViewModel
    val firebaseLogger = viewModel.firebaseLogger

    // Display variables for UI
    val termsAccepted = legalState.termsAccepted
    val privacyPolicyAccepted = legalState.privacyPolicyAccepted
    val bothAccepted = termsAccepted && privacyPolicyAccepted
    val showError = uiState.showLegalError

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section with Legal Status
            Surface(
                color = if (bothAccepted)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.document_icon),
                            contentDescription = null,
                            tint = if (bothAccepted)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Legal Agreement",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (bothAccepted)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = if (bothAccepted) "Accepted" else "Required",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (bothAccepted)
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    if (bothAccepted) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Terms of Service Section
            LegalFeature(
                icon = R.drawable.document_icon,
                title = "Terms of Service",
                description = "App usage conditions and limitations",
                isChecked = termsAccepted,
                onCheckedChange = { isChecked ->
                    viewModel.handleEvent(
                        IntroductionViewModel.IntroEvent.AcceptTerms(isChecked)
                    )

                    // Log analytics event
                    firebaseLogger.logEvent(
                        "terms_acceptance_changed",
                        mapOf("accepted" to isChecked),
                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                    )
                },
                onButtonClick = {
                    navController.navigate("web_view_screen/terms_of_service")

                    // Log analytics event
                    firebaseLogger.logEvent(
                        "terms_of_service_opened",
                        null,
                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                    )
                },
                buttonText = "Read Terms of Service",
                checkboxText = "I agree to the Terms of Service",
                isHighlighted = !termsAccepted && showError
            )

            // Privacy Policy Section
            LegalFeature(
                icon = R.drawable.privacy_policy_icon,
                title = "Privacy Policy",
                description = "How your data is handled and protected",
                isChecked = privacyPolicyAccepted,
                onCheckedChange = { isChecked ->
                    viewModel.handleEvent(
                        IntroductionViewModel.IntroEvent.AcceptPrivacyPolicy(isChecked)
                    )

                    // Log analytics event
                    firebaseLogger.logEvent(
                        "privacy_policy_acceptance_changed",
                        mapOf("accepted" to isChecked),
                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                    )
                },
                onButtonClick = {
                    navController.navigate("web_view_screen/privacy_policy")

                    // Log analytics event
                    firebaseLogger.logEvent(
                        "privacy_policy_opened",
                        null,
                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                    )
                },
                buttonText = "Read Privacy Policy",
                checkboxText = "I agree to the Privacy Policy",
                isHighlighted = !privacyPolicyAccepted && showError
            )

            // Accept All Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (bothAccepted)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else if (showError)
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Surface(
                        shape = CircleShape,
                        color = if (bothAccepted)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable {
                                if (!bothAccepted) {
                                    viewModel.handleEvent(
                                        IntroductionViewModel.IntroEvent.AcceptAllLegalTerms(true)
                                    )

                                    // Log analytics event
                                    firebaseLogger.logEvent(
                                        "all_legal_terms_toggle",
                                        mapOf("accepted" to true),
                                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                                    )
                                } else {
                                    viewModel.handleEvent(
                                        IntroductionViewModel.IntroEvent.AcceptAllLegalTerms(false)
                                    )

                                    // Log analytics event
                                    firebaseLogger.logEvent(
                                        "all_legal_terms_toggle",
                                        mapOf("accepted" to false),
                                        FirebaseLogger.Companion.EventCategory.USER_ACTION
                                    )
                                }
                            }
                    ) {
                        Icon(
                            imageVector = if (bothAccepted)
                                Icons.Rounded.Check
                            else
                                Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (bothAccepted) "Both accepted" else "Both not accepted",
                            tint = if (bothAccepted)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                    Text(
                        text = "I accept all terms and policies",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (showError && !bothAccepted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Loading Indicator
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }

            // Error message
            AnimatedVisibility(visible = showError && !bothAccepted) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "You must accept both the Terms of Service and Privacy Policy to continue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalFeature(
    icon: Int,
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onButtonClick: () -> Unit,
    buttonText: String,
    checkboxText: String,
    isHighlighted: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (isHighlighted)
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else if (isChecked)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        border = if (isHighlighted)
            BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = if (isChecked)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Image(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (isHighlighted)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Button
            OutlinedButton(
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isChecked)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            onCheckedChange(!isChecked)
                        }
                ) {
                    Icon(
                        imageVector = if (isChecked)
                            Icons.Rounded.Check
                        else
                            Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isChecked) "$title accepted" else "$title not accepted",
                        tint = if (isChecked)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }
                Text(
                    text = checkboxText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isChecked)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )

            }
        }
    }
}