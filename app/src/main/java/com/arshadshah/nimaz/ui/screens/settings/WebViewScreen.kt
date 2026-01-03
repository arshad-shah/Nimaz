package com.arshadshah.nimaz.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.BackButton
import com.arshadshah.nimaz.ui.components.common.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, navController: NavHostController) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (url) {
                            "privacy_policy" -> "Privacy Policy"
                            "terms_of_service" -> "Terms of Service"
                            "help" -> "Help"
                            else -> ""
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackButton {
                        navController.popBackStack()
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                when (url) {
                    "privacy_policy" -> PrivacyPolicyContent()
                    "terms_of_service" -> TermsContent()
                    "help" -> HelpContent()
                }
            }
        }
    }
}

@Composable
private fun PrivacyPolicyContent() {
    ContentSection(
        title = "Privacy Policy",
        icon = R.drawable.privacy_policy_icon
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Project Information
            SectionContent(
                title = "About This App",
                content = """
## Personal Project Notice

Nimaz is a free app created by Arshad Shah as a personal project. This service is:
* Provided at no cost
* Developed by an individual
* Intended for use as-is
* Made to serve the Muslim community

Your privacy is a top priority in this personal project.

---"""
            )

            // Basic Information
            SectionContent(
                title = "Overview",
                content = """
## Privacy Basics

Key points about your privacy:

* Most data stays on your device
* No personal information is collected
* No data is sold or shared
* Third-party services are limited to essential functions

By using Nimaz, you agree to the terms in this privacy policy.

---"""
            )

            // Information Collection
            SectionContent(
                title = "Data Collection",
                content = """
## Information Usage

### On-Device Data
* Prayer time preferences
* App settings
* Location data (when permitted)
* All stored locally on your device

### Collected Information
* Limited crash reports
* Anonymous usage statistics
* App performance data

No personally identifiable information leaves your device except as described below.

---"""
            )

            // Third Party Services
            SectionContent(
                title = "Third-Party Services",
                content = """
## External Services

The app uses these essential services:

* [Google Play Services](https://policies.google.com/terms)
  * Core Android functionality
  * App updates and security

* [Firebase Analytics](https://firebase.google.com/terms/analytics)
  * Anonymous usage data
  * App improvement insights

* [Firebase Crashlytics](https://firebase.google.com/terms/crashlytics)
  * Crash reporting
  * Bug identification

Each service has its own privacy policy that you should review.

---"""
            )

            // Log Data
            SectionContent(
                title = "Technical Data",
                content = """
## Log Information

For app improvement, we may collect:

* Device information
  * Operating system version
  * Device model
  * App configuration

* Error data
  * Crash reports
  * Performance metrics
  * Technical diagnostics

This data is:
* Anonymized
* Used only for app improvement
* Handled securely
* Never sold or shared

---"""
            )

            // Security
            SectionContent(
                title = "Security Measures",
                content = """
## Data Protection

While this is a personal project, I take security seriously:

* Data is stored securely on your device
* Standard security practices are followed
* Third-party services are carefully selected
* Regular security updates are provided

However, please note that no method of internet transmission is 100% secure.

---"""
            )

// Children's Privacy
            SectionContent(
                title = "Children's Privacy",
                content = """
## Young Users

Nimaz is suitable for Muslims of all ages, including children. However, regarding privacy:

* The app is designed to be family-friendly and safe
* All prayer times, Quran, and Islamic content is appropriate for children
* Most data (prayer times, settings, etc.) stays locally on the device
* Location access requires device permissions (parental guidance recommended)
* No personal information is collected from any users
* Firebase analytics and crash reporting are anonymous
* Parents/guardians should review app permissions

For families:
* Parents are encouraged to use the app together with young children
* Supervise location permission settings
* Help children understand prayer times and Islamic features
* Guide them in building healthy spiritual habits

For additional information or concerns about your child's use of the app, please contact us at info@arshadshah.com

---"""
            )

            // External Links
            SectionContent(
                title = "External Sites",
                content = """
## Third-Party Links

When using external links:

* Links are provided for convenience
* External sites have different policies
* We're not responsible for external content
* Review external privacy policies separately

---"""
            )

            // Updates to Policy
            SectionContent(
                title = "Policy Updates",
                content = """
## Changes & Updates

* Policy may be updated periodically
* Changes will be posted here
* Effective date: 2023-04-14
* Check back for updates

---"""
            )

            // Contact Information
            SectionContent(
                title = "Contact",
                content = """
## Questions?

Get in touch:
* 📧 Email: info@arshadshah.com
* 🌐 GitHub: [Project Page](https://github.com/arshad-shah/nimaz)

*Privacy policy template from [privacypolicytemplate.net](https://privacypolicytemplate.net) and modified via [App Privacy Policy Generator](https://app-privacy-policy-generator.nisrulz.com/)*

---"""
            )
        }
    }
}

@Composable
private fun TermsContent() {
    ContentSection(
        title = "Terms of Service",
        icon = R.drawable.document_icon // Assuming you have a document icon
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Personal Project Disclaimer
            SectionContent(
                title = "Project Information",
                content = """
## Personal Project Notice

**Important**: Nimaz is a personal project created by Arshad Shah, developed as an individual initiative to serve the Muslim community. This is not a commercial product or service, and is provided free of charge.

* This app is maintained by an individual developer
* No commercial entity or organization is behind this project
* The service is provided "as is" without warranty
* Support is provided on a best-effort basis

---"""
            )

            // Basic Terms
            SectionContent(
                title = "Basic Terms",
                content = """
## Terms of Use

By downloading or using Nimaz, you agree to these terms:

* The app is provided free of charge
* You may not modify or redistribute the app
* You may not extract or reverse engineer the code
* All rights remain with the developer (Arshad Shah)

The app and its trademarks, copyright, and other intellectual property rights belong to Arshad Shah.

---"""
            )

            // App Updates & Changes
            SectionContent(
                title = "Updates and Modifications",
                content = """
## App Updates

As a personal project, please note:

* The app may be updated periodically
* Features may change without notice
* Updates are recommended but optional
* The app is currently Android-only
* Service may be discontinued at any time

We strive to keep the app relevant and functional but make no guarantees about future updates or compatibility.

---"""
            )

            // Data & Privacy
            SectionContent(
                title = "Data and Privacy",
                content = """
## Data Usage

The app:
* Stores minimal personal data
* Uses data only for core functionality
* Requires certain permissions to function

### Security Recommendations
* Keep your device secure
* Avoid rooting/jailbreaking
* Maintain latest version
* Use official download sources

---"""
            )

            // Third Party Services
            SectionContent(
                title = "Third-Party Services",
                content = """
## External Services

The app utilizes these third-party services:

* [Google Play Services](https://policies.google.com/terms)
  * Core functionality
  * Essential updates

* [Firebase Analytics](https://firebase.google.com/terms/analytics)
  * Usage insights
  * App improvements

* [Firebase Crashlytics](https://firebase.google.com/terms/crashlytics)
  * Stability monitoring
  * Issue identification

Each service has its own terms and privacy policies.

---"""
            )

            // Limitations
            SectionContent(
                title = "Limitations",
                content = """
## Service Limitations

Please be aware:

1. **Internet Requirements**
   * Some features need internet
   * Data charges may apply
   * Wi-Fi recommended

2. **Device Compatibility**
   * Android version requirements
   * Device-specific limitations
   * Performance variations

3. **No Liability**
   * No warranty provided
   * Use at your own risk
   * No damage responsibility

---"""
            )

            // Changes to Terms
            SectionContent(
                title = "Terms Updates",
                content = """
## Changes to Terms

* Terms may be updated periodically
* Changes will be posted here
* Continued use implies acceptance
* Last updated: 2023-04-14

---"""
            )

            // Contact Information
            SectionContent(
                title = "Contact",
                content = """
## Questions or Concerns?

Contact the developer:
* 📧 Email: info@arshadshah.com
* 🌐 GitHub: [Project Repository](https://github.com/arshad-shah/nimaz)

Terms generated using [App Privacy Policy Generator](https://app-privacy-policy-generator.nisrulz.com/)

---"""
            )
        }
    }
}

@Composable
private fun HelpContent() {
    ContentSection(
        title = "Help & Support",
        icon = R.drawable.help_icon
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Introduction Section
            SectionContent(
                title = "Introduction",
                content = """
## Getting Started

Nimaz is your companion for accurate prayer times and Islamic guidance. Here's what you need to know:

* Nimaz uses mathematical formulas based on the sun's position to calculate prayer times
* Location data is used to ensure accuracy within 5 minutes
* During Ramadan, verify times with your local mosque
* Full customization options are available for precise adjustments

---"""
            )

            // Prayer Times Calculation Section
            SectionContent(
                title = "Prayer Times Calculation",
                content = """
## Calculation Methods

Nimaz offers two primary calculation methods:

1. **Automatic Method**
   * Uses sun position for calculations
   * Requires location access
   * Most accurate for standard locations

2. **Manual Method**
   * Uses predefined calculation angles
   * Customizable for local preferences
   * Perfect for matching mosque timings

---"""
            )

            // Available Methods Section
            SectionContent(
                title = "Available Methods",
                content = """
## Standard Methods

Choose from these widely-accepted calculation methods:

* **MWL** - Muslim World League
* **EGYPTIAN** - Egyptian General Authority of Survey
* **KARACHI** - University of Islamic Sciences, Karachi
* **MAKKAH** - Umm al-Qura University, Makkah
* **ISNA** - Islamic Society of North America
* **KUWAIT** - Kuwait Standard Method
* **QATAR** - Qatar Standard Method
* **SINGAPORE** - Singapore Standard Method
* **FRANCE** - French Muslims Method
* **TURKEY** - Turkish Diyanet Method
* **RUSSIA** - Russian Muslims Method
* **MOONSIGHTING** - Moonsighting Committee
* **IRELAND** - Irish Muslims Method

## Custom Method
The **Other** option allows full customization:
* Set custom angles for each prayer
* Adjust individual prayer times
* Fine-tune for your specific location

---"""
            )

            // Notifications Section
            SectionContent(
                title = "Alarms & Notifications",
                content = """
## Prayer Time Alerts

Configure your notifications in Settings:

### Troubleshooting Checklist
If alerts aren't working, verify:
* ✓ Silent mode is off
* ✓ Do Not Disturb is disabled
* ✓ Airplane mode is off
* ✓ App notifications are enabled

### Quick Actions
1. **Reset Alarms**: Reconfigure all prayer notifications
2. **Test Notification**: Send a test alert
3. **System Settings**: Adjust system-level permissions

---"""
            )

            // Quick Guide Section
            SectionContent(
                title = "Quick Guide",
                content = """
## 5-Minute Setup

1. **Grant Permissions**
   * Allow location access
   * Enable notifications
   
2. **Choose Method**
   * Select calculation method
   * Verify with local mosque
   
3. **Configure Alerts**
   * Set notification style
   * Choose adhan sound
   * Adjust volume levels

4. **Fine-tune Settings**
   * Set time adjustments
   * Configure Jumuah times
   * Set Ramadan preferences

---"""
            )

            // FAQ Section
            SectionContent(
                title = "FAQ",
                content = """
## Common Questions

**Q: Why do prayer times vary from my mosque?**
* Local conventions may differ
* Use manual adjustments to match
* Contact mosque for official times

**Q: How accurate is the app?**
* Within 5 minutes typically
* Accuracy depends on location
* Verify during daylight savings

**Q: Can I use custom settings?**
* Yes, use "Other" method
* Adjust individual prayers
* Save presets for later

---"""
            )

            // Support Section
            SectionContent(
                title = "Get Help",
                content = """
## Need Assistance?

### Contact Us
* 📧 [info@arshadshah.com](mailto:info@arshadshah.com)
* 🌐 [Visit Website](https://nimaz.arshadshah.com)
* 📱 [Rate on Play Store](https://play.google.com/store/apps/details?id=com.arshadshah.nimaz)

### Community
* Follow updates on [GitHub](https://github.com/arshad-shah/nimaz)

---"""
            )
        }
    }
}

@Composable
private fun SectionContent(
    title: String,
    content: String
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        MarkdownText(
            markdown = content,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            onLinkClicked = { url ->
                // Handle link click
                Log.d("MarkdownText", "Link clicked: $url")
                val urlIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
                context.startActivity(urlIntent)
            }
        )
    }
}


@Composable
private fun ContentSection(
    title: String,
    icon: Int,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Content Section
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    content()
                }
            }
        }
    }
}