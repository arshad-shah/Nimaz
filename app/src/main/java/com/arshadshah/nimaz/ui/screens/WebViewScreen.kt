package com.arshadshah.nimaz.ui.screens

import android.text.Html
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WebViewScreen(url : String , paddingValues : PaddingValues)
{
	LazyColumn(content = {
	 item()
	 {
			if (url == "privacy_policy")
			{
				val privacy1Text = "<p>\n" +
						"        <b>Arshad Shah</b> built the <b>Nimaz</b> app as\n" +
						"        a Free app. This SERVICE is provided by\n" +
						"        <b>Arshad Shah</b> at no cost and is intended for use as\n" +
						"        is.\n" +
						"    </p>\n" +
						"    <p>\n" +
						"        This page is used to inform visitors regarding my\n" +
						"        policies with the collection, use, and disclosure of Personal\n" +
						"        Information if anyone decided to use my Service.\n" +
						"    </p>\n" +
						"    <p>\n" +
						"        If you choose to use my Service, then you agree to\n" +
						"        the collection and use of information in relation to this\n" +
						"        policy. The Personal Information that I collect is\n" +
						"        used for providing and improving the Service. I will not use or share your information with\n" +
						"        anyone except as described in this Privacy Policy.\n" +
						"    </p>\n" +
						"    <p>\n" +
						"        The terms used in this Privacy Policy have the same meanings\n" +
						"        as in our Terms and Conditions, which is accessible at\n" +
						"        <b>Nimaz</b> unless otherwise defined in this Privacy Policy.\n" +
						"    </p>\n" +
						"    <H1><strong>Information Collection and Use</strong></H1>\n" +
						"    <p>\n" +
						"        For a better experience, while using our Service, I\n" +
						"        may require you to provide us with certain personally\n" +
						"        identifiable information. The information that\n" +
						"        I request will be retained on your device and is not collected by me in any way.\n" +
						"    </p>\n" +
						"    <div>\n" +
						"        <p>\n" +
						"            The app does use third party services that may collect\n" +
						"            information used to identify you.\n" +
						"        </p>\n" +
						"        <p>\n" +
						"            Link to privacy policy of third party service providers used\n" +
						"            by the app\n" +
						"        </p>\n" +
						"        <ul>\n" +
						"            <li><a href=\"https://www.google.com/policies/privacy/\" target=\"_blank\" rel=\"noopener noreferrer\">Google Play\n" +
						"                    Services <br/>https://www.google.com/policies/privacy</a></li>\n" +
						"\n" +
						"        </ul>\n" +
						"    </div>\n" +
						"    <p><strong>Log Data</strong></p>\n" +
						"    <p>\n" +
						"        I want to inform you that whenever you\n" +
						"        use my Service, in a case of an error in the app\n" +
						"        I collect data and information (through third party\n" +
						"        products) on your phone called Log Data. This Log Data may\n" +
						"        include information such as your device Internet Protocol\n" +
						"        (“IP”) address, device name, operating system version, the\n" +
						"        configuration of the app when utilizing my Service,\n" +
						"        the time and date of your use of the Service, and other\n" +
						"        statistics.\n" +
						"    </p>\n" +
						"    <p><strong>Cookies</strong></p>\n" +
						"    <p>\n" +
						"        Cookies are files with a small amount of data that are\n" +
						"        commonly used as anonymous unique identifiers. These are sent\n" +
						"        to your browser from the websites that you visit and are\n" +
						"        stored on your device's internal memory.\n" +
						"    </p>\n" +
						"    <p>\n" +
						"        This Service does not use these “cookies” explicitly. However,\n" +
						"        the app may use third party code and libraries that use\n" +
						"        “cookies” to collect information and improve their services.\n" +
						"        You have the option to either accept or refuse these cookies\n" +
						"        and know when a cookie is being sent to your device. If you\n" +
						"        choose to refuse our cookies, you may not be able to use some\n" +
						"        portions of this Service.\n" +
						"    </p>\n" +
						"    <p><strong>Service Providers</strong></p>\n" +
						"    <p>\n" +
						"        I may employ third-party companies and\n" +
						"        individuals due to the following reasons:\n" +
						"    </p>\n" +
						"    <ul>\n" +
						"        <li>To facilitate our Service;</li>\n" +
						"        <li>To provide the Service on our behalf;</li>\n" +
						"        <li>To perform Service-related services; or</li>\n" +
						"        <li>To assist us in analyzing how our Service is used.</li>\n" +
						"    </ul>\n" +
						"    <p>\n" +
						"        I want to inform users of this Service\n" +
						"        that these third parties have access to your Personal\n" +
						"        Information. The reason is to perform the tasks assigned to\n" +
						"        them on our behalf. However, they are obligated not to\n" +
						"        disclose or use the information for any other purpose.\n" +
						"    </p>\n" +
						"    <p><strong>Security</strong></p>\n" +
						"    <p>\n" +
						"        I value your trust in providing us your\n" +
						"        Personal Information, thus we are striving to use commercially\n" +
						"        acceptable means of protecting it. But remember that no method\n" +
						"        of transmission over the internet, or method of electronic\n" +
						"        storage is 100% secure and reliable, and I cannot\n" +
						"        guarantee its absolute security.\n" +
						"    </p>\n" +
						"    <p><strong>Links to Other Sites</strong></p>\n" +
						"    <p>\n" +
						"        This Service may contain links to other sites. If you click on\n" +
						"        a third-party link, you will be directed to that site. Note\n" +
						"        that these external sites are not operated by me.\n" +
						"        Therefore, I strongly advise you to review the\n" +
						"        Privacy Policy of these websites. I have\n" +
						"        no control over and assume no responsibility for the content,\n" +
						"        privacy policies, or practices of any third-party sites or\n" +
						"        services.\n" +
						"    </p>\n" +
						"    <p><strong>Children’s Privacy</strong></p>\n" +
						"    <p>\n" +
						"        These Services do not address anyone under the age of 13.\n" +
						"        I do not knowingly collect personally\n" +
						"        identifiable information from children under 13 years of age. In the case\n" +
						"        I discover that a child under 13 has provided\n" +
						"        me with personal information, I immediately\n" +
						"        delete this from our servers. If you are a parent or guardian\n" +
						"        and you are aware that your child has provided us with\n" +
						"        personal information, please contact me so that\n" +
						"        I will be able to do necessary actions.\n" +
						"    </p>\n" +
						"    <p><strong>Changes to This Privacy Policy</strong></p>\n" +
						"    <p>\n" +
						"        I may update our Privacy Policy from\n" +
						"        time to time. Thus, you are advised to review this page\n" +
						"        periodically for any changes. I will\n" +
						"        notify you of any changes by posting the new Privacy Policy on\n" +
						"        this page.\n" +
						"    </p>\n" +
						"    <p>This policy is effective as of 10-02-2023</p>\n" +
						"    <p><strong>Contact Us</strong></p>\n" +
						"    <p>\n" +
						"        If you have any questions or suggestions about my\n" +
						"        Privacy Policy, do not hesitate to contact me at info@arshadshah.com.\n" +
						"    </p>\n" +
						"    <p>This privacy policy page was created at <a href=\"https://privacypolicytemplate.net\" target=\"_blank\"\n" +
						"            rel=\"noopener noreferrer\">https://privacypolicytemplate.net </a> and modified/generated by <a\n" +
						"            href=\"https://app-privacy-policy-generator.nisrulz.com/\" target=\"_blank\" rel=\"noopener noreferrer\">App\n" +
						"            Privacy\n" +
						"            Policy Generator<br/>https://app-privacy-policy-generator.nisrulz.com</a></p>\n "

				Text(text = Html.fromHtml(privacy1Text, Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH).toString() , modifier = Modifier.padding(16.dp))
			}
			else if (url == "terms_of_service")
			{

				val TandCtext = "            <p>\n" +
						"                By downloading or using the app, these terms will\n" +
						"                automatically apply to you – you should make sure therefore\n" +
						"                that you read them carefully before using the app." +
						"                And you also shouldn’t try\n" +
						"                to translate the app into other languages, or make derivative\n" +
						"                versions. The app itself, and all the trade marks, copyright,\n" +
						"                database rights and other intellectual property rights related\n" +
						"                to it, still belong to <b>Arshad Shah</b>.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                <b>Arshad Shah</b> is committed to ensuring that the app is\n" +
						"                as useful and efficient as possible. For that reason, we\n" +
						"                reserve the right to make changes to the app or to charge for\n" +
						"                its services, at any time and for any reason. We will never\n" +
						"                charge you for the app or its services without making it very\n" +
						"                clear to you exactly what you’re paying for.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                The <b>Nimaz</b> app stores and processes personal data that\n" +
						"                you have provided to us, in order to provide my\n" +
						"                Service. It’s your responsibility to keep your phone and\n" +
						"                access to the app secure. We therefore recommend that you do\n" +
						"                not jailbreak or root your phone, which is the process of\n" +
						"                removing software restrictions and limitations imposed by the\n" +
						"                official operating system of your device. It could make your\n" +
						"                phone vulnerable to malware/viruses/malicious programs,\n" +
						"                compromise your phone’s security features and it could mean\n" +
						"                that the <b>Nimaz</b> app won’t work properly or at all.\n" +
						"            </p>\n" +
						"            <div>\n" +
						"                <p>\n" +
						"                    The app does use third party services that declare their own\n" +
						"                    Terms and Conditions.\n" +
						"                </p>\n" +
						"                <p>\n" +
						"                    Link to Terms and Conditions of third party service\n" +
						"                    providers used by the app\n" +
						"                </p>\n" +
						"                <ul>\n" +
						"                    <li><a href=\"https://policies.google.com/terms\" target=\"_blank\" rel=\"noopener noreferrer\">Google\n" +
						"                            Play\n" +
						"                            Services<br/>https://policies.google.com/terms</a></li>\n" +
						"                </ul>\n" +
						"            </div>\n" +
						"            <p>\n" +
						"                You should be aware that there are certain things that\n" +
						"                <b>Arshad Shah</b> will not take responsibility for. Certain\n" +
						"                functions of the app will require the app to have an active\n" +
						"                internet connection. The connection can be Wi-Fi, or provided\n" +
						"                by your mobile network provider, but <b>Arshad Shah</b>\n" +
						"                cannot take responsibility for the app not working at full\n" +
						"                functionality if you don’t have access to Wi-Fi, and you don’t\n" +
						"                have any of your data allowance left.\n" +
						"            </p>\n" +
						"            <p></p>\n" +
						"            <p>\n" +
						"                If you’re using the app outside of an area with Wi-Fi, you\n" +
						"                should remember that your terms of the agreement with your\n" +
						"                mobile network provider will still apply. As a result, you may\n" +
						"                be charged by your mobile provider for the cost of data for\n" +
						"                the duration of the connection while accessing the app, or\n" +
						"                other third party charges. In using the app, you’re accepting\n" +
						"                responsibility for any such charges, including roaming data\n" +
						"                charges if you use the app outside of your home territory\n" +
						"                (i.e. region or country) without turning off data roaming. If\n" +
						"                you are not the bill payer for the device on which you’re\n" +
						"                using the app, please be aware that we assume that you have\n" +
						"                received permission from the bill payer for using the app.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                Along the same lines, <b>Arshad Shah</b> cannot always take\n" +
						"                responsibility for the way you use the app i.e. You need to\n" +
						"                make sure that your device stays charged – if it runs out of\n" +
						"                battery and you can’t turn it on to avail the Service,\n" +
						"                <b>Arshad Shah</b> cannot accept responsibility.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                With respect to <b>Arshad Shah's</b> responsibility for your\n" +
						"                use of the app, when you’re using the app, it’s important to\n" +
						"                bear in mind that although we endeavour to ensure that it is\n" +
						"                updated and correct at all times, we do rely on third parties\n" +
						"                to provide information to us so that we can make it available\n" +
						"                to you. <b>Arshad Shah</b> accepts no liability for any\n" +
						"                loss, direct or indirect, you experience as a result of\n" +
						"                relying wholly on this functionality of the app.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                At some point, we may wish to update the app. The app is\n" +
						"                currently available on Android – the requirements for\n" +
						"                system(and for any additional systems we\n" +
						"                decide to extend the availability of the app to) may change,\n" +
						"                and you’ll need to download the updates if you want to keep\n" +
						"                using the app. <b>Arshad Shah</b> does not promise that it\n" +
						"                will always update the app so that it is relevant to you\n" +
						"                and/or works with the Android version that you have\n" +
						"                installed on your device. However, you promise to always\n" +
						"                accept updates to the application when offered to you, We may\n" +
						"                also wish to stop providing the app, and may terminate use of\n" +
						"                it at any time without giving notice of termination to you.\n" +
						"                Unless we tell you otherwise, upon any termination, (a) the\n" +
						"                rights and licenses granted to you in these terms will end;\n" +
						"                (b) you must stop using the app, and (if needed) delete it\n" +
						"                from your device.\n" +
						"            </p>\n" +
						"            <p><strong>Changes to This Terms and Conditions</strong></p>\n" +
						"            <p>\n" +
						"                I may update our Terms and Conditions\n" +
						"                from time to time. Thus, you are advised to review this page\n" +
						"                periodically for any changes. I will\n" +
						"                notify you of any changes by posting the new Terms and\n" +
						"                Conditions on this page.\n" +
						"            </p>\n" +
						"            <p>\n" +
						"                These terms and conditions are effective as of 10-02-2023\n" +
						"            </p>\n" +
						"            <p><strong>Contact Us</strong></p>\n" +
						"            <p>\n" +
						"                If you have any questions or suggestions about my\n" +
						"                Terms and Conditions, do not hesitate to contact me\n" +
						"                at <br/>" +
						"info@arshadshah.com \n" +
						"            </p>\n" +
						"            <p>This Terms and Conditions page was generated by <a\n" +
						"                    href=\"https://app-privacy-policy-generator.nisrulz.com/\" target=\"_blank\"\n" +
						"                    rel=\"noopener noreferrer\">https://app-privacy-policy-generator.nisrulz.com</a></p>"
				Text(text = Html.fromHtml(TandCtext, Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH).toString(), modifier = Modifier.padding(16.dp))

			}

		}
	} , contentPadding = paddingValues)

}
