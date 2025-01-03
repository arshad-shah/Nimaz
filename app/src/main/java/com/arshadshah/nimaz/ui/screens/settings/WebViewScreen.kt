package com.arshadshah.nimaz.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.ui.components.common.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, navController: NavHostController) {
    val context = LocalContext.current
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
                        }
                    )
                },
                navigationIcon = {
                    OutlinedIconButton(
                        modifier = Modifier
                            .testTag("backButton")
                            .padding(start = 8.dp),
                        onClick = {
                            navController.popBackStack()
                        }) {
                        Icon(
                            modifier = Modifier.size(24.dp),
                            painter = painterResource(id = R.drawable.back_icon),
                            contentDescription = "Back"
                        )
                    }
                },
            )
        },
    ) {
        LazyColumn(content = {
            item()
            {
                when (url) {
                    "privacy_policy" -> {
                        val privacy1Text = """
**Privacy Policy**

Arshad shah built the Nimaz app as a Free app. This SERVICE is provided by Arshad shah at no cost and is intended for use as is.

This page is used to inform visitors regarding my policies with the collection, use, and disclosure of Personal Information if anyone decided to use my Service.

If you choose to use my Service, then you agree to the collection and use of information in relation to this policy. The Personal Information that I collect is used for providing and improving the Service. I will not use or share your information with anyone except as described in this Privacy Policy.

The terms used in this Privacy Policy have the same meanings as in our Terms and Conditions, which are accessible at Nimaz unless otherwise defined in this Privacy Policy.

**Information Collection and Use**

For a better experience, while using our Service, I may require you to provide us with certain personally identifiable information. The information that I request will be retained on your device and is not collected by me in any way.

The app does use third-party services that may collect information used to identify you.

Link to the privacy policy of third-party service providers used by the app

*   [Google Play Services](https://www.google.com/policies/privacy/)
*   [Google Analytics for Firebase](https://firebase.google.com/policies/analytics)
*   [Firebase Crashlytics](https://firebase.google.com/support/privacy/)

**Log Data**

I want to inform you that whenever you use my Service, in a case of an error in the app I collect data and information (through third-party products) on your phone called Log Data. This Log Data may include information such as your device Internet Protocol (“IP”) address, device name, operating system version, the configuration of the app when utilizing my Service, the time and date of your use of the Service, and other statistics.

**Cookies**

Cookies are files with a small amount of data that are commonly used as anonymous unique identifiers. These are sent to your browser from the websites that you visit and are stored on your device's internal memory.

This Service does not use these “cookies” explicitly. However, the app may use third-party code and libraries that use “cookies” to collect information and improve their services. You have the option to either accept or refuse these cookies and know when a cookie is being sent to your device. If you choose to refuse our cookies, you may not be able to use some portions of this Service.

**Service Providers**

I may employ third-party companies and individuals due to the following reasons:

*   To facilitate our Service;
*   To provide the Service on our behalf;
*   To perform Service-related services; or
*   To assist us in analyzing how our Service is used.

I want to inform users of this Service that these third parties have access to their Personal Information. The reason is to perform the tasks assigned to them on our behalf. However, they are obligated not to disclose or use the information for any other purpose.

**Security**

I value your trust in providing us your Personal Information, thus we are striving to use commercially acceptable means of protecting it. But remember that no method of transmission over the internet, or method of electronic storage is 100% secure and reliable, and I cannot guarantee its absolute security.

**Links to Other Sites**

This Service may contain links to other sites. If you click on a third-party link, you will be directed to that site. Note that these external sites are not operated by me. Therefore, I strongly advise you to review the Privacy Policy of these websites. I have no control over and assume no responsibility for the content, privacy policies, or practices of any third-party sites or services.

**Children’s Privacy**

These Services do not address anyone under the age of 13. I do not knowingly collect personally identifiable information from children under 13 years of age. In the case I discover that a child under 13 has provided me with personal information, I immediately delete this from our servers. If you are a parent or guardian and you are aware that your child has provided us with personal information, please contact me so that I will be able to do the necessary actions.

**Changes to This Privacy Policy**

I may update our Privacy Policy from time to time. Thus, you are advised to review this page periodically for any changes. I will notify you of any changes by posting the new Privacy Policy on this page.

This policy is effective as of 2023-04-14

**Contact Us**

If you have any questions or suggestions about my Privacy Policy, do not hesitate to contact me at info@arshadshah.com.

This privacy policy page was created at [privacypolicytemplate.net](https://privacypolicytemplate.net) and modified/generated by [App Privacy Policy Generator](https://app-privacy-policy-generator.nisrulz.com/)
							"""
                        MarkdownText(
                            markdown = privacy1Text,
                            color = MaterialTheme.colorScheme.onBackground,
                            onLinkClicked = { url ->
                                // Handle link click
                                Log.d("MarkdownText", "Link clicked: $url")
                                //open github link
                                val urlIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                context.startActivity(urlIntent)
                            },
                        )
                    }

                    "terms_of_service" -> {

                        val TandCtext = """
**Terms & Conditions**

By downloading or using the app, these terms will automatically apply to you – you should make sure therefore that you read them carefully before using the app. You’re not allowed to copy or modify the app, any part of the app, or our trademarks in any way. You’re not allowed to attempt to extract the source code of the app, and you also shouldn’t try to translate the app into other languages or make derivative versions. The app itself, and all the trademarks, copyright, database rights, and other intellectual property rights related to it, still belong to Arshad shah.

Arshad shah is committed to ensuring that the app is as useful and efficient as possible. For that reason, we reserve the right to make changes to the app or to charge for its services, at any time and for any reason. We will never charge you for the app or its services without making it very clear to you exactly what you’re paying for.

The Nimaz app stores and processes personal data that you have provided to us, to provide my Service. It’s your responsibility to keep your phone and access to the app secure. We therefore recommend that you do not jailbreak or root your phone, which is the process of removing software restrictions and limitations imposed by the official operating system of your device. It could make your phone vulnerable to malware/viruses/malicious programs, compromise your phone’s security features and it could mean that the Nimaz app won’t work properly or at all.

The app does use third-party services that declare their Terms and Conditions.

Link to Terms and Conditions of third-party service providers used by the app

*   [Google Play Services](https://policies.google.com/terms)
*   [Google Analytics for Firebase](https://firebase.google.com/terms/analytics)
*   [Firebase Crashlytics](https://firebase.google.com/terms/crashlytics)

You should be aware that there are certain things that Arshad shah will not take responsibility for. Certain functions of the app will require the app to have an active internet connection. The connection can be Wi-Fi or provided by your mobile network provider, but Arshad shah cannot take responsibility for the app not working at full functionality if you don’t have access to Wi-Fi, and you don’t have any of your data allowance left.

If you’re using the app outside of an area with Wi-Fi, you should remember that the terms of the agreement with your mobile network provider will still apply. As a result, you may be charged by your mobile provider for the cost of data for the duration of the connection while accessing the app, or other third-party charges. In using the app, you’re accepting responsibility for any such charges, including roaming data charges if you use the app outside of your home territory (i.e. region or country) without turning off data roaming. If you are not the bill payer for the device on which you’re using the app, please be aware that we assume that you have received permission from the bill payer for using the app.

Along the same lines, Arshad shah cannot always take responsibility for the way you use the app i.e. You need to make sure that your device stays charged – if it runs out of battery and you can’t turn it on to avail the Service, Arshad shah cannot accept responsibility.

With respect to Arshad shah’s responsibility for your use of the app, when you’re using the app, it’s important to bear in mind that although we endeavor to ensure that it is updated and correct at all times, we do rely on third parties to provide information to us so that we can make it available to you. Arshad shah accepts no liability for any loss, direct or indirect, you experience as a result of relying wholly on this functionality of the app.

At some point, we may wish to update the app. The app is currently available on Android – the requirements for the system(and for any additional systems we decide to extend the availability of the app to) may change, and you’ll need to download the updates if you want to keep using the app. Arshad shah does not promise that it will always update the app so that it is relevant to you and/or works with the Android version that you have installed on your device. However, you promise to always accept updates to the application when offered to you, We may also wish to stop providing the app, and may terminate use of it at any time without giving notice of termination to you. Unless we tell you otherwise, upon any termination, (a) the rights and licenses granted to you in these terms will end; (b) you must stop using the app, and (if needed) delete it from your device.

**Changes to This Terms and Conditions**

I may update our Terms and Conditions from time to time. Thus, you are advised to review this page periodically for any changes. I will notify you of any changes by posting the new Terms and Conditions on this page.

These terms and conditions are effective as of 2023-04-14

**Contact Us**

If you have any questions or suggestions about my Terms and Conditions, do not hesitate to contact me at info@arshadshah.com.

This Terms and Conditions page was generated by [App Privacy Policy Generator](https://app-privacy-policy-generator.nisrulz.com/)
							"""
                        MarkdownText(
                            markdown = TandCtext,
                            color = MaterialTheme.colorScheme.onBackground,
                            onLinkClicked = { url ->
                                // Handle link click
                                Log.d("MarkdownText", "Link clicked: $url")
                                //open github link
                                val urlIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                context.startActivity(urlIntent)
                            },
                        )

                    }

                    "help" -> {
                        val internettext = """ 
**Introduction**

Nimaz uses Mathematical formulae to find Prayer times based on the location of the sun in the horizon using the current location of your device.

to achieve a high rate of accuracy some assumptions are made hence an acceptable error rate of 5 minutes can occur in the times.

For this reason it is highly recommended to check the prayer times during critical periods such as ramadan with your local mosque and apply the corrections to Nimaz as appropriate.

To facilitate this customization Nimaz provides complete control over the parameters used to calculate prayer times.

Nimaz provides a number of features to facilitate your daily prayers and Quran recitation.

**Prayer times Calculation**

To Calculate prayer times Nimaz uses two methods:

* **Automatic** - Nimaz uses the location of the sun in the horizon to calculate prayer times

* **Manual** - Nimaz uses the Predefined angles for prayer times calculation

	Manual Calculation provides a list of methods to choose from and allows you to customize the angles and times for each method.
	* **MWL** - Muslim World League
	* **EGYPTIAN** - Egyptian General Authority of Survey
	* **KARACHI** - University of Islamic Sciences, Karachi
	* **MAKKAH** - Umm al-Qura University, Makkah
	* **DUBAI** - Dubai
	* **ISNA** - Islamic Society of North America (ISNA)
	* **KUWAIT** - Kuwait
	* **TEHRAN** - Institute of Geophysics, University of Tehran
	* **SHIA** - Shia Ithna Ashari, Leva Institute, Qum
	* **GULF** - Gulf Region
	* **QATAR** - Qatar
	* **SINGAPORE** - Singapore
	* **FRANCE** - France
	* **TURKEY** - Turkey
	* **RUSSIA** - Russia
	* **MOONSIGHTING** - Moonsighting Committee
	* **IRELAND** - Ireland
	* **OTHER** - Other (Custom)
	
	The **Other** method allows you to create a custom method by specifying the angles and times for each prayer.
	It defaults all values to 0 and you can change them to the values you want.
	
**Alarms and Notifications**

All settings for alarms and notifications are available from the app settings screen.

If the alarms do not work please check the following:
* Make sure that the device is not in silent mode
* Make sure that the device is not in Do not disturb mode
* Make sure that the device is not in Airplane mode

	**Troubleshooting**
	
	Nimaz provides a troubleshooting options for alarms and notifications.
	All alarms can be reset from the troubleshooting screen. 
	In addition to this a test alarm can be triggered to test the alarms.
					"""

                        MarkdownText(
                            markdown = internettext,
                            color = MaterialTheme.colorScheme.onBackground,
                            onLinkClicked = { url ->
                                // Handle link click
                                Log.d("MarkdownText", "Link clicked: $url")
                                //open github link
                                val urlIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(url)
                                )
                                context.startActivity(urlIntent)
                            },
                        )
                    }
                }

            }
        }, contentPadding = it)
    }

}
