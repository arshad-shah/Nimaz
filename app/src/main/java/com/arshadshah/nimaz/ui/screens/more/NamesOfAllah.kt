package com.arshadshah.nimaz.ui.screens.more

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arshadshah.nimaz.R
import com.arshadshah.nimaz.constants.AppConstants.TEST_TAG_NAMES_OF_ALLAH
import com.arshadshah.nimaz.ui.theme.utmaniQuranFont

@Composable
fun NamesOfAllah(paddingValues: PaddingValues) {
    val resources = LocalContext.current.resources
    val englishNames = resources.getStringArray(R.array.English)
    val arabicNames = resources.getStringArray(R.array.Arabic)
    val translationNames = resources.getStringArray(R.array.translation)

    Card(
        modifier = Modifier
            .padding(16.dp)
            .padding(paddingValues)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(8.dp),
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TEST_TAG_NAMES_OF_ALLAH),
        ) {
            items(englishNames.size) { index ->
                NamesOfAllahRow(
                    index,
                    englishNames[index],
                    arabicNames[index],
                    translationNames[index]
                )
                if (index < englishNames.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        thickness = 1.dp
                    )
                }
            }
        }
    }
}

@Composable
fun NamesOfAllahRow(
    index: Int,
    englishName: String,
    arabicName: String,
    translationName: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    val nameDetails = DivineNamesRepository.getNameDetails(index)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (index % 2 == 0)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .clickable { isExpanded = !isExpanded }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Decorated Number Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Main Content
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Translation with subtle background
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = translationName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }


                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = arabicName,
                                textAlign = TextAlign.Start,
                                fontFamily = utmaniQuranFont,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            )
                        }

                    // English Transliteration with icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Translate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = englishName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Expand/Collapse Icon
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded)
                            Icons.Outlined.KeyboardArrowUp
                        else
                            Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Show less" else "Show more",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 60.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Additional information when expanded
                    InfoRow(
                        title = "Root Word:",
                        content = nameDetails.rootWord, // Add actual content
                        icon = Icons.Outlined.Source
                    )
                    InfoRow(
                        title = "Occurrence in Quran:",
                        content = nameDetails.occurrence, // Add actual content
                        icon = Icons.Outlined.Book
                    )
                    InfoRow(
                        title = "Significance:",
                        content = nameDetails.significance, // Add actual content
                        icon = Icons.Outlined.Info
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    title: String,
    content: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

data class NameDetails(
    val rootWord: String,
    val occurrence: String,
    val significance: String
)

object DivineNamesRepository {
    private val nameDetails = mapOf(
        0 to NameDetails(
            rootWord = "ر-ح-م (Ra-Ha-Mim)",
            occurrence = "Appears 57 times in the Quran",
            significance = "Ar-Rahman signifies Allah's all-encompassing mercy that covers all creation. This name is so significant that it appears in the opening of 113 surahs in 'Bismillah ir-Rahman ir-Raheem'. It represents the vastness of Allah's mercy that encompasses all creation, regardless of their faith or actions."
        ),
        1 to NameDetails(
            rootWord = "ر-ح-م (Ra-Ha-Mim)",
            occurrence = "Appears 95 times in the Quran",
            significance = "Ar-Raheem represents Allah's special and specific mercy, particularly directed towards the believers. While Ar-Rahman is general mercy, Ar-Raheem is specific and lasting mercy, especially manifested in the hereafter."
        ),
        2 to NameDetails(
            rootWord = "م-ل-ك (Mim-Lam-Kaf)",
            occurrence = "Appears 49 times in the Quran",
            significance = "Al-Malik denotes absolute sovereignty and kingship. Allah is the true King who owns and controls everything in existence, with complete authority over all creation."
        ),
        3 to NameDetails(
            rootWord = "ق-د-س (Qaf-Dal-Sin)",
            occurrence = "Appears 4 times in the Quran",
            significance = "Al-Quddus means The Most Holy, The Most Pure. This name denotes absolute purity and freedom from any imperfection, deficiency, or fault."
        ),
        4 to NameDetails(
            rootWord = "س-ل-م (Sin-Lam-Mim)",
            occurrence = "Appears 33 times in the Quran",
            significance = "As-Salaam means The Source of Peace. This name indicates that Allah is free from any defect or imperfection and is the source of all peace and safety."
        ),
        5 to NameDetails(
            rootWord = "أ-م-ن (Alif-Mim-Nun)",
            occurrence = "Appears 6 times in the Quran",
            significance = "Al-Mu'min means The Granter of Security. This name indicates that Allah is the one who confirms the truth of His promises, grants security to His servants, and protects them."
        ),
        6 to NameDetails(
            rootWord = "ه-م-ن (Ha-Mim-Nun)",
            occurrence = "Appears 2 times in the Quran",
            significance = "Al-Muhaymin means The Guardian, The Protector. This name indicates Allah's complete watchfulness and protection over all creation."
        ),
        7 to NameDetails(
            rootWord = "ع-ز-ز (Ayn-Zay-Zay)",
            occurrence = "Appears 92 times in the Quran",
            significance = "Al-Azeez means The Almighty, The Most Powerful. This name represents Allah's complete and perfect might and honor, indicating that He cannot be overcome or resisted."
        ),
        8 to NameDetails(
            rootWord = "ج-ب-ر (Jim-Ba-Ra)",
            occurrence = "Appears 10 times in the Quran",
            significance = "Al-Jabbaar means The Compeller, The Restorer. This name indicates Allah's ability to compel and restore all affairs, making the broken whole and compelling creation to His will."
        ),
        9 to NameDetails(
            rootWord = "ك-ب-ر (Kaf-Ba-Ra)",
            occurrence = "Appears 6 times in the Quran",
            significance = "Al-Mutakabbir means The Supreme, The Majestic. This name indicates Allah's legitimate right to be proud and His supremacy above all creation."
        ),
        10 to NameDetails(
            rootWord = "خ-ل-ق (Kha-Lam-Qaf)",
            occurrence = "Appears 261 times in the Quran",
            significance = "Al-Khaaliq means The Creator. This name indicates Allah's power of creation, bringing things into existence from nothingness."
        ),
        11 to NameDetails(
            rootWord = "ب-ر-أ (Ba-Ra-Hamza)",
            occurrence = "Appears 31 times in the Quran",
            significance = "Al-Baari means The Maker, The Creator who creates with perfect proportion. This name specifically refers to Allah's ability to create with amazing harmony and order from pre-existing matter."
        ),
        12 to NameDetails(
            rootWord = "ص-و-ر (Sad-Waw-Ra)",
            occurrence = "Appears 19 times in the Quran",
            significance = "Al-Musawwir means The Fashioner, The Shaper. This name represents Allah's ability to give unique shape, form, and characteristics to His creation, making each creation distinct and unique."
        ),
        13 to NameDetails(
            rootWord = "غ-ف-ر (Ghayn-Fa-Ra)",
            occurrence = "Appears 91 times in the Quran",
            significance = "Al-Ghaffaar means The Ever-Forgiving, The All-Forgiving. This name emphasizes Allah's continuous forgiveness of sins for those who repent, no matter how many times they may sin and return to Him."
        ),
        14 to NameDetails(
            rootWord = "ق-ه-ر (Qaf-Ha-Ra)",
            occurrence = "Appears 11 times in the Quran",
            significance = "Al-Qahhaar means The Subduer, The All-Dominant. This name represents Allah's absolute dominance over His creation, emphasizing His power to overcome all things."
        ),
        15 to NameDetails(
            rootWord = "و-ه-ب (Waw-Ha-Ba)",
            occurrence = "Appears 25 times in the Quran",
            significance = "Al-Wahhaab means The Bestower, The Supreme Giver. This name indicates Allah's attribute of giving freely without any expectation of return or reward, giving out of pure generosity."
        ),
        16 to NameDetails(
            rootWord = "ر-ز-ق (Ra-Zay-Qaf)",
            occurrence = "Appears 123 times in the Quran",
            significance = "Ar-Razzaaq means The Provider, The All-Sustainer. This name represents Allah's attribute of providing sustenance to all creation, both material and spiritual provision."
        ),
        17 to NameDetails(
            rootWord = "ف-ت-ح (Fa-Ta-Ha)",
            occurrence = "Appears 38 times in the Quran",
            significance = "Al-Fattaah means The Opener, The Judge. This name represents Allah's power to open the ways of success and victory, both in worldly matters and spiritual matters, and His perfect judgment between truth and falsehood."
        ),
        18 to NameDetails(
            rootWord = "ع-ل-م (Ayn-Lam-Mim)",
            occurrence = "Appears 148 times in the Quran",
            significance = "Al-'Aleem means The All-Knowing, The Omniscient. This name indicates Allah's perfect and complete knowledge of all things, both apparent and hidden, past, present, and future."
        ),
        19 to NameDetails(
            rootWord = "ق-ب-ض (Qaf-Ba-Dad)",
            occurrence = "Appears 9 times in the Quran",
            significance = "Al-Qaabid means The Withholder, The Constrictor. This name represents Allah's power to withhold or restrict things such as provision, life, or souls, according to His wisdom."
        ),
        20 to NameDetails(
            rootWord = "ب-س-ط (Ba-Sin-Ta)",
            occurrence = "Appears 25 times in the Quran",
            significance = "Al-Baasit means The Extender, The Expander. This name represents Allah's attribute of expanding and extending provision, mercy, and knowledge to whom He wills, working in harmony with Al-Qaabid."
        ),

        21 to NameDetails(
            rootWord = "خ-ف-ض (Kha-Fa-Dad)",
            occurrence = "Appears 5 times in the Quran",
            significance = "Al-Khaafid means The Abaser, The One Who Lowers. This name indicates Allah's power to lower and humble those who are arrogant or transgress bounds, working in perfect balance with Ar-Rafi'."
        ),
        22 to NameDetails(
            rootWord = "ر-ف-ع (Ra-Fa-Ayn)",
            occurrence = "Appears 28 times in the Quran",
            significance = "Ar-Raafi' means The Elevator, The Exalter. This name represents Allah's ability to raise in honor and status whom He wills, particularly those who are humble and righteous."
        ),
        23 to NameDetails(
            rootWord = "ع-ز-ز (Ayn-Zay-Zay)",
            occurrence = "Appears 119 times in the Quran",
            significance = "Al-Mu'iz means The Bestower of Honor. This name indicates Allah's power to grant honor, dignity, and might to whomever He wills, often paired with Al-Mudhil."
        ),
        24 to NameDetails(
            rootWord = "ذ-ل-ل (Thal-Lam-Lam)",
            occurrence = "Appears 24 times in the Quran",
            significance = "Al-Mudhil means The Giver of Dishonor. This name represents Allah's attribute of humbling and lowering those who deserve to be brought low through their actions and choices."
        ),
        25 to NameDetails(
            rootWord = "س-م-ع (Sin-Mim-Ayn)",
            occurrence = "Appears 185 times in the Quran",
            significance = "As-Samee' means The All-Hearing. This name indicates Allah's perfect ability to hear all things, from the loudest sound to the most secret whisper, simultaneously and completely."
        ),
        26 to NameDetails(
            rootWord = "ب-ص-ر (Ba-Sad-Ra)",
            occurrence = "Appears 148 times in the Quran",
            significance = "Al-Baseer means The All-Seeing. This name represents Allah's perfect ability to see all things, both physical and spiritual, apparent and hidden, at all times and in all places."
        ),
        27 to NameDetails(
            rootWord = "ح-ك-م (Ha-Kaf-Mim)",
            occurrence = "Appears 310 times in the Quran",
            significance = "Al-Hakam means The Judge, The Arbitrator. This name indicates Allah's role as the supreme judge who judges with perfect justice and truth, making final decisions between His creation."
        ),
        28 to NameDetails(
            rootWord = "ع-د-ل (Ayn-Dal-Lam)",
            occurrence = "Appears 28 times in the Quran",
            significance = "Al-'Adl means The Utterly Just. This name represents Allah's perfect justice and fairness in all matters, never oppressing anyone and giving everyone their due rights."
        ),
        29 to NameDetails(
            rootWord = "ل-ط-ف (Lam-Ta-Fa)",
            occurrence = "Appears 7 times in the Quran",
            significance = "Al-Lateef means The Most Subtle and Kind. This name indicates Allah's gentleness and kindness in how He deals with His creation, as well as His ability to be aware of the finest details and most delicate matters."
        ),
        30 to NameDetails(
            rootWord = "خ-ب-ر (Kha-Ba-Ra)",
            occurrence = "Appears 45 times in the Quran",
            significance = "Al-Khabeer means The All-Aware. This name represents Allah's complete awareness of the inner nature and hidden reality of all things, being perfectly informed of all affairs."
        ),
        31 to NameDetails(
            rootWord = "ح-ل-م (Ha-Lam-Mim)",
            occurrence = "Appears 15 times in the Quran",
            significance = "Al-Haleem means The Most Forbearing. This name describes Allah's attribute of patience and forbearance, showing restraint in punishing the sinners despite having complete power to do so, giving them time to repent."
        ),
        32 to NameDetails(
            rootWord = "ع-ظ-م (Ayn-Zha-Mim)",
            occurrence = "Appears 108 times in the Quran",
            significance = "Al-'Azeem means The Magnificent, The Supreme. This name represents Allah's absolute greatness and magnificence that is beyond human comprehension, encompassing all aspects of greatness."
        ),
        33 to NameDetails(
            rootWord = "غ-ف-ر (Ghayn-Fa-Ra)",
            occurrence = "Appears 234 times in the Quran",
            significance = "Al-Ghafoor means The Most Forgiving. While similar to Al-Ghaffar, this name emphasizes the vastness and completeness of Allah's forgiveness, covering and removing sins entirely."
        ),
        34 to NameDetails(
            rootWord = "ش-ك-ر (Shin-Kaf-Ra)",
            occurrence = "Appears 75 times in the Quran",
            significance = "Ash-Shakoor means The Most Appreciative. This name indicates that Allah appreciates and rewards even the smallest good deeds abundantly, multiplying the rewards of His servants."
        ),
        35 to NameDetails(
            rootWord = "ع-ل-و (Ayn-Lam-Waw)",
            occurrence = "Appears 45 times in the Quran",
            significance = "Al-'Aliyy means The Most High. This name represents Allah's transcendence above creation in His essence, attributes, and authority, indicating His absolute superiority."
        ),
        36 to NameDetails(
            rootWord = "ك-ب-ر (Kaf-Ba-Ra)",
            occurrence = "Appears 161 times in the Quran",
            significance = "Al-Kabeer means The Greatest. This name indicates Allah's absolute greatness in all aspects - His essence, attributes, names, and actions are all characterized by ultimate greatness."
        ),
        37 to NameDetails(
            rootWord = "ح-ف-ظ (Ha-Fa-Zha)",
            occurrence = "Appears 44 times in the Quran",
            significance = "Al-Hafeez means The Preserver, The Guardian. This name represents Allah's perfect preservation and protection of all things, maintaining records of all deeds and preserving His creation."
        ),
        38 to NameDetails(
            rootWord = "ق-و-ت (Qaf-Waw-Ta)",
            occurrence = "Appears 2 times in the Quran",
            significance = "Al-Muqeet means The Sustainer, The Maintainer. This name indicates Allah's power to provide nourishment and sustenance to all creation, both physically and spiritually."
        ),
        39 to NameDetails(
            rootWord = "ح-س-ب (Ha-Sin-Ba)",
            occurrence = "Appears 109 times in the Quran",
            significance = "Al-Haseeb means The Reckoner, The Sufficient. This name represents Allah's attribute of being sufficient for His servants and His perfect ability to take account of all things."
        ),
        40 to NameDetails(
            rootWord = "ج-ل-ل (Jim-Lam-Lam)",
            occurrence = "Appears 73 times in the Quran",
            significance = "Al-Jaleel means The Majestic. This name represents Allah's attribute of absolute majesty and glory, indicating His perfect attributes that inspire both awe and reverence."
        ),
        41 to NameDetails(
            rootWord = "ك-ر-م (Kaf-Ra-Mim)",
            occurrence = "Appears 47 times in the Quran",
            significance = "Al-Kareem means The Most Generous, The Most Noble. This name represents Allah's unlimited generosity and nobility, giving abundantly without expectation of return and treating His creation with honor."
        ),
        42 to NameDetails(
            rootWord = "ر-ق-ب (Ra-Qaf-Ba)",
            occurrence = "Appears 24 times in the Quran",
            significance = "Ar-Raqeeb means The Watchful. This name indicates Allah's perfect and complete watchfulness over all creation, observing and monitoring every action, thought, and intention."
        ),
        43 to NameDetails(
            rootWord = "ج-و-ب (Jim-Waw-Ba)",
            occurrence = "Appears 28 times in the Quran",
            significance = "Al-Mujeeb means The Responsive One. This name represents Allah's attribute of answering prayers and responding to the calls and supplications of His servants, especially those in need."
        ),
        44 to NameDetails(
            rootWord = "و-س-ع (Waw-Sin-Ayn)",
            occurrence = "Appears 32 times in the Quran",
            significance = "Al-Waasi' means The All-Encompassing, The Vast. This name indicates Allah's infinite capacity and vastness in His knowledge, mercy, provision, and all other attributes."
        ),
        45 to NameDetails(
            rootWord = "ح-ك-م (Ha-Kaf-Mim)",
            occurrence = "Appears 97 times in the Quran",
            significance = "Al-Hakeem means The All-Wise. This name represents Allah's perfect wisdom in all matters, creating and commanding with purpose and executing everything with supreme wisdom."
        ),
        46 to NameDetails(
            rootWord = "و-د-د (Waw-Dal-Dal)",
            occurrence = "Appears 11 times in the Quran",
            significance = "Al-Wadood means The Most Loving. This name indicates Allah's perfect and complete love for His creation, particularly for those who believe and do good deeds, showing affection through blessings and mercy."
        ),
        47 to NameDetails(
            rootWord = "م-ج-د (Mim-Jim-Dal)",
            occurrence = "Appears 9 times in the Quran",
            significance = "Al-Majeed means The Most Glorious. This name represents Allah's perfect and complete glory, honor, and nobility, encompassing all meanings of greatness and magnificence."
        ),
        48 to NameDetails(
            rootWord = "ب-ع-ث (Ba-Ayn-Tha)",
            occurrence = "Appears 65 times in the Quran",
            significance = "Al-Baa'ith means The Resurrector. This name indicates Allah's power to raise the dead and resurrect all creation for the Day of Judgment, as well as His ability to revive hearts with faith."
        ),
        49 to NameDetails(
            rootWord = "ش-ه-د (Shin-Ha-Dal)",
            occurrence = "Appears 160 times in the Quran",
            significance = "Ash-Shaheed means The Witness. This name represents Allah's complete awareness and witnessing of all things, being present and observant of every occurrence, both apparent and hidden."
        ),
        50 to NameDetails(
            rootWord = "ح-ق-ق (Ha-Qaf-Qaf)",
            occurrence = "Appears 287 times in the Quran",
            significance = "Al-Haqq means The Truth, The Real. This name indicates that Allah is the absolute truth in His essence, attributes, and actions. Everything He says and does is true and real, and He is the source of all truth."
        ),
        51 to NameDetails(
            rootWord = "و-ك-ل (Waw-Kaf-Lam)",
            occurrence = "Appears 44 times in the Quran",
            significance = "Al-Wakeel means The Trustee, The Disposer of Affairs. This name represents Allah's perfect guardianship and protection of His creation, being sufficient for those who place their trust in Him."
        ),
        52 to NameDetails(
            rootWord = "ق-و-ي (Qaf-Waw-Ya)",
            occurrence = "Appears 42 times in the Quran",
            significance = "Al-Qawiyy means The All-Strong. This name indicates Allah's perfect and absolute strength, power, and might, for whom nothing is difficult or impossible."
        ),
        53 to NameDetails(
            rootWord = "م-ت-ن (Mim-Ta-Nun)",
            occurrence = "Appears 1 time in the Quran",
            significance = "Al-Mateen means The Firm, The Steadfast. This name represents Allah's perfect power combined with wisdom, indicating strength that never weakens and firmness that never wavers."
        ),
        54 to NameDetails(
            rootWord = "و-ل-ي (Waw-Lam-Ya)",
            occurrence = "Appears 233 times in the Quran",
            significance = "Al-Waliyy means The Protecting Friend, The Supporter. This name indicates Allah's perfect guardianship and support of the believers, being their closest ally and protector."
        ),
        55 to NameDetails(
            rootWord = "ح-م-د (Ha-Mim-Dal)",
            occurrence = "Appears 68 times in the Quran",
            significance = "Al-Hameed means The Praiseworthy. This name represents Allah as being worthy of all praise, whose every action and decree deserves praise and gratitude, being praised for His perfect attributes."
        ),
        56 to NameDetails(
            rootWord = "ح-ص-ي (Ha-Sad-Ya)",
            occurrence = "Appears 11 times in the Quran",
            significance = "Al-Muhsi means The Counter, The Appraiser. This name indicates Allah's perfect knowledge that encompasses even the minutest details, counting and recording everything with absolute precision."
        ),
        57 to NameDetails(
            rootWord = "ب-د-أ (Ba-Dal-Hamza)",
            occurrence = "Appears 15 times in the Quran",
            significance = "Al-Mubdi' means The Originator, The Initiator. This name represents Allah's power to bring things into existence for the first time, creating without any prior example."
        ),
        58 to NameDetails(
            rootWord = "ع-و-د (Ayn-Waw-Dal)",
            occurrence = "Appears 27 times in the Quran",
            significance = "Al-Mu'eed means The Restorer, The Repeater. This name indicates Allah's power to bring things back into existence after their destruction, particularly referring to resurrection."
        ),
        59 to NameDetails(
            rootWord = "ح-ي-ي (Ha-Ya-Ya)",
            occurrence = "Appears 83 times in the Quran",
            significance = "Al-Muhyi means The Giver of Life. This name represents Allah's power to give life, both physical life to creation and spiritual life to hearts through faith and guidance."
        ),
        60 to NameDetails(
            rootWord = "م-و-ت (Mim-Waw-Ta)",
            occurrence = "Appears 168 times in the Quran",
            significance = "Al-Mumeet means The Bringer of Death, The Taker of Life. This name indicates Allah's power over death, causing all living things to die at their appointed time according to His wisdom."
        ),
        61 to NameDetails(
            rootWord = "ح-ي-ي (Ha-Ya-Ya)",
            occurrence = "Appears 71 times in the Quran",
            significance = "Al-Hayy means The Ever-Living. This name represents Allah's perfect, eternal life that has no beginning and no end, being the source of all life and existing without dependence on anything."
        ),
        62 to NameDetails(
            rootWord = "ق-و-م (Qaf-Waw-Mim)",
            occurrence = "Appears 45 times in the Quran",
            significance = "Al-Qayyum means The Self-Subsisting, The Sustainer of All. This name indicates Allah's attribute of existing independently while all creation depends on Him for their existence and sustenance."
        ),
        63 to NameDetails(
            rootWord = "و-ج-د (Waw-Jim-Dal)",
            occurrence = "Appears 107 times in the Quran",
            significance = "Al-Waajid means The Finder, The Perceiver. This name represents Allah's attribute of finding everything He wants whenever He wants, nothing being absent from His knowledge or power."
        ),
        64 to NameDetails(
            rootWord = "م-ج-د (Mim-Jim-Dal)",
            occurrence = "Appears 9 times in the Quran",
            significance = "Al-Maajid means The Noble, The Glorious. This name represents Allah's perfect nobility and vast generosity, indicating His limitless excellence in His essence and attributes."
        ),
        65 to NameDetails(
            rootWord = "و-ح-د (Waw-Ha-Dal)",
            occurrence = "Appears 68 times in the Quran",
            significance = "Al-Waahid means The One, The Unique. This name indicates Allah's absolute oneness in His essence, attributes, and actions, having no partners or equals."
        ),
        66 to NameDetails(
            rootWord = "أ-ح-د (Alif-Ha-Dal)",
            occurrence = "Appears 52 times in the Quran",
            significance = "Al-Ahad means The One, The Only One. While similar to Al-Waahid, this name emphasizes absolute uniqueness and indivisibility, particularly in divinity and worship."
        ),
        67 to NameDetails(
            rootWord = "ص-م-د (Sad-Mim-Dal)",
            occurrence = "Appears 1 time in the Quran",
            significance = "As-Samad means The Eternal Refuge, The Perfect. This name represents Allah as the one upon whom all creation depends for their needs while He depends on none, being absolutely perfect and self-sufficient."
        ),
        68 to NameDetails(
            rootWord = "ق-د-ر (Qaf-Dal-Ra)",
            occurrence = "Appears 132 times in the Quran",
            significance = "Al-Qadir means The Able, The Capable. This name indicates Allah's complete and perfect power to do anything He wills, having absolute ability over all things."
        ),
        69 to NameDetails(
            rootWord = "ق-د-ر (Qaf-Dal-Ra)",
            occurrence = "Appears 47 times in the Quran",
            significance = "Al-Muqtadir means The All Determiner, The Dominant. This is an intensified form of Al-Qadir, emphasizing Allah's absolute power and control over all affairs."
        ),
        70 to NameDetails(
            rootWord = "ق-د-م (Qaf-Dal-Mim)",
            occurrence = "Appears 27 times in the Quran",
            significance = "Al-Muqaddim means The Expediter, The Promoter. This name represents Allah's power to bring forward whatever He wills, advancing some of His creation over others according to His wisdom."
        ),
        71 to NameDetails(
            rootWord = "أ-خ-ر (Alif-Kha-Ra)",
            occurrence = "Appears 163 times in the Quran",
            significance = "Al-Mu'akhkhir means The Postponer, The Delayer. This name represents Allah's wisdom in putting things back or delaying them according to His perfect knowledge and plan."
        ),
        72 to NameDetails(
            rootWord = "أ-و-ل (Alif-Waw-Lam)",
            occurrence = "Appears 40 times in the Quran",
            significance = "Al-Awwal means The First. This name indicates that Allah is the First with no beginning, existing before all creation and being the source of everything that exists."
        ),
        73 to NameDetails(
            rootWord = "أ-خ-ر (Alif-Kha-Ra)",
            occurrence = "Appears 40 times in the Quran",
            significance = "Al-Aakhir means The Last. This name indicates that Allah is the Last with no end, remaining after all creation has perished and being the ultimate end of all matters."
        ),
        74 to NameDetails(
            rootWord = "ظ-ه-ر (Zha-Ha-Ra)",
            occurrence = "Appears 61 times in the Quran",
            significance = "Az-Zaahir means The Manifest, The Apparent. This name indicates that Allah is apparent through His signs and creation, His existence being clear through His manifestations."
        ),
        75 to NameDetails(
            rootWord = "ب-ط-ن (Ba-Ta-Nun)",
            occurrence = "Appears 25 times in the Quran",
            significance = "Al-Baatin means The Hidden, The Imperceptible. This name indicates that Allah's essence cannot be perceived by human senses or comprehended fully by human minds."
        ),
        76 to NameDetails(
            rootWord = "و-ل-ي (Waw-Lam-Ya)",
            occurrence = "Appears 124 times in the Quran",
            significance = "Al-Waali means The Governor, The Patron. This name represents Allah's authority over all creation, managing and governing all affairs with perfect wisdom."
        ),
        77 to NameDetails(
            rootWord = "ع-ل-و (Ayn-Lam-Waw)",
            occurrence = "Appears 45 times in the Quran",
            significance = "Al-Muta'ali means The Most Exalted. This name represents Allah's absolute transcendence above creation, being far above any imperfection or limitation."
        ),
        78 to NameDetails(
            rootWord = "ب-ر-ر (Ba-Ra-Ra)",
            occurrence = "Appears 32 times in the Quran",
            significance = "Al-Barr means The Most Kind and Righteous. This name indicates Allah's perfect kindness and vast goodness towards His creation, fulfilling His promises and being generous in His giving."
        ),
        79 to NameDetails(
            rootWord = "ت-و-ب (Ta-Waw-Ba)",
            occurrence = "Appears 87 times in the Quran",
            significance = "At-Tawwaab means The Ever-Returning, The Acceptor of Repentance. This name represents Allah's mercy in repeatedly accepting repentance from His servants, turning towards them with forgiveness."
        ),
        80 to NameDetails(
            rootWord = "ن-ق-م (Nun-Qaf-Mim)",
            occurrence = "Appears 4 times in the Quran",
            significance = "Al-Muntaqim means The Avenger. This name represents Allah's justice in exacting retribution from those who persist in evil and oppression, while being tied to His wisdom and mercy."
        ),
        71 to NameDetails(
            rootWord = "أ-خ-ر (Alif-Kha-Ra)",
            occurrence = "Appears 163 times in the Quran",
            significance = "Al-Mu'akhkhir means The Postponer, The Delayer. This name represents Allah's wisdom in putting things back or delaying them according to His perfect knowledge and plan."
        ),
        72 to NameDetails(
            rootWord = "أ-و-ل (Alif-Waw-Lam)",
            occurrence = "Appears 40 times in the Quran",
            significance = "Al-Awwal means The First. This name indicates that Allah is the First with no beginning, existing before all creation and being the source of everything that exists."
        ),
        73 to NameDetails(
            rootWord = "أ-خ-ر (Alif-Kha-Ra)",
            occurrence = "Appears 40 times in the Quran",
            significance = "Al-Aakhir means The Last. This name indicates that Allah is the Last with no end, remaining after all creation has perished and being the ultimate end of all matters."
        ),
        74 to NameDetails(
            rootWord = "ظ-ه-ر (Zha-Ha-Ra)",
            occurrence = "Appears 61 times in the Quran",
            significance = "Az-Zaahir means The Manifest, The Apparent. This name indicates that Allah is apparent through His signs and creation, His existence being clear through His manifestations."
        ),
        75 to NameDetails(
            rootWord = "ب-ط-ن (Ba-Ta-Nun)",
            occurrence = "Appears 25 times in the Quran",
            significance = "Al-Baatin means The Hidden, The Imperceptible. This name indicates that Allah's essence cannot be perceived by human senses or comprehended fully by human minds."
        ),
        76 to NameDetails(
            rootWord = "و-ل-ي (Waw-Lam-Ya)",
            occurrence = "Appears 124 times in the Quran",
            significance = "Al-Waali means The Governor, The Patron. This name represents Allah's authority over all creation, managing and governing all affairs with perfect wisdom."
        ),
        77 to NameDetails(
            rootWord = "ع-ل-و (Ayn-Lam-Waw)",
            occurrence = "Appears 45 times in the Quran",
            significance = "Al-Muta'ali means The Most Exalted. This name represents Allah's absolute transcendence above creation, being far above any imperfection or limitation."
        ),
        78 to NameDetails(
            rootWord = "ب-ر-ر (Ba-Ra-Ra)",
            occurrence = "Appears 32 times in the Quran",
            significance = "Al-Barr means The Most Kind and Righteous. This name indicates Allah's perfect kindness and vast goodness towards His creation, fulfilling His promises and being generous in His giving."
        ),
        79 to NameDetails(
            rootWord = "ت-و-ب (Ta-Waw-Ba)",
            occurrence = "Appears 87 times in the Quran",
            significance = "At-Tawwaab means The Ever-Returning, The Acceptor of Repentance. This name represents Allah's mercy in repeatedly accepting repentance from His servants, turning towards them with forgiveness."
        ),
        80 to NameDetails(
            rootWord = "ن-ق-م (Nun-Qaf-Mim)",
            occurrence = "Appears 4 times in the Quran",
            significance = "Al-Muntaqim means The Avenger. This name represents Allah's justice in exacting retribution from those who persist in evil and oppression, while being tied to His wisdom and mercy."
        ),
        81 to NameDetails(
            rootWord = "ع-ف-و (Ayn-Fa-Waw)",
            occurrence = "Appears 35 times in the Quran",
            significance = "Al-'Afuw means The Pardoner. This name represents Allah's attribute of pardoning His servants, effacing their sins and removing their faults completely out of His grace."
        ),
        82 to NameDetails(
            rootWord = "ر-أ-ف (Ra-Alif-Fa)",
            occurrence = "Appears 13 times in the Quran",
            significance = "Ar-Ra'oof means The Most Kind. This name indicates Allah's extreme kindness and compassion towards His creation, showing a tenderness that goes beyond ordinary mercy."
        ),
        83 to NameDetails(
            rootWord = "م-ل-ك (Mim-Lam-Kaf)",
            occurrence = "Appears 206 times in the Quran",
            significance = "Maalik-ul-Mulk means The Owner of All Sovereignty. This name indicates Allah's absolute ownership and kingship over all creation, having complete authority over all dominions."
        ),
        84 to NameDetails(
            rootWord = "ج-ل-ل (Jim-Lam-Lam) and ك-ر-م (Kaf-Ra-Mim)",
            occurrence = "Combined form appears 3 times in the Quran",
            significance = "Dhul-Jalaali-wal-Ikraam means The Lord of Majesty and Bounty. This name combines Allah's attributes of magnificence and generosity, deserving both reverence and love."
        ),
        85 to NameDetails(
            rootWord = "ق-س-ط (Qaf-Sin-Ta)",
            occurrence = "Appears 15 times in the Quran",
            significance = "Al-Muqsit means The Equitable One. This name represents Allah's perfect justice and equity in dealing with His creation, establishing balance and fairness in all matters."
        ),
        86 to NameDetails(
            rootWord = "ج-م-ع (Jim-Mim-Ayn)",
            occurrence = "Appears 43 times in the Quran",
            significance = "Al-Jaami' means The Gatherer. This name indicates Allah's power to bring together what He wills, whether physically gathering creation on the Day of Judgment or uniting hearts in faith."
        ),
        87 to NameDetails(
            rootWord = "غ-ن-ي (Ghayn-Nun-Ya)",
            occurrence = "Appears 20 times in the Quran",
            significance = "Al-Ghaniyy means The Self-Sufficient, The Rich. This name represents Allah's absolute independence and self-sufficiency, needing nothing while all creation needs Him."
        ),
        88 to NameDetails(
            rootWord = "غ-ن-ي (Ghayn-Nun-Ya)",
            occurrence = "Appears 2 times in the Quran",
            significance = "Al-Mughni means The Enricher. This name indicates Allah's power to satisfy the needs of His creation and make them free from want, both materially and spiritually."
        ),
        89 to NameDetails(
            rootWord = "م-ن-ع (Mim-Nun-Ayn)",
            occurrence = "Appears 17 times in the Quran",
            significance = "Al-Maani' means The Preventer, The Withholder. This name represents Allah's wisdom in preventing things according to His will and knowledge, protecting through prevention."
        ),
        90 to NameDetails(
            rootWord = "ض-ر-ر (Dad-Ra-Ra)",
            occurrence = "Appears 75 times in the Quran",
            significance = "Ad-Daarr means The Creator of Harm. This name indicates Allah's power over adversity and hardship, allowing difficulty to occur according to His wisdom and as a test for His servants."
        ),
        91 to NameDetails(
            rootWord = "ن-ف-ع (Nun-Fa-Ayn)",
            occurrence = "Appears 50 times in the Quran",
            significance = "An-Naafi' means The Creator of Good. This name represents Allah's attribute of bringing about all benefit and good, being the source of all that profits and benefits His creation."
        ),
        92 to NameDetails(
            rootWord = "ن-و-ر (Nun-Waw-Ra)",
            occurrence = "Appears 49 times in the Quran",
            significance = "An-Noor means The Light. This name indicates that Allah is the Light of the heavens and earth, guiding creation through both physical and spiritual light, illuminating hearts with guidance."
        ),
        93 to NameDetails(
            rootWord = "ه-د-ي (Ha-Dal-Ya)",
            occurrence = "Appears 316 times in the Quran",
            significance = "Al-Haadi means The Guide. This name represents Allah's guidance of His creation to what is beneficial for them, both in worldly matters and, more importantly, to the straight path."
        ),
        94 to NameDetails(
            rootWord = "ب-د-ع (Ba-Dal-Ayn)",
            occurrence = "Appears 4 times in the Quran",
            significance = "Al-Badee' means The Originator, The Incomparable. This name indicates Allah's power to create entirely new and unprecedented things, creating in the most wonderful and unique way."
        ),
        95 to NameDetails(
            rootWord = "ب-ق-ي (Ba-Qaf-Ya)",
            occurrence = "Appears 96 times in the Quran",
            significance = "Al-Baaqi means The Everlasting. This name represents Allah's eternal existence without end, remaining after all creation has perished, being the only truly permanent existence."
        ),
        96 to NameDetails(
            rootWord = "و-ر-ث (Waw-Ra-Tha)",
            occurrence = "Appears 35 times in the Quran",
            significance = "Al-Waarith means The Inheritor, The Ultimate Inheritor. This name indicates that Allah is the ultimate heir of all creation, to whom everything returns after all creation ceases to exist."
        ),
        97 to NameDetails(
            rootWord = "ر-ش-د (Ra-Shin-Dal)",
            occurrence = "Appears 19 times in the Quran",
            significance = "Ar-Rasheed means The Guide to the Right Path. This name represents Allah's perfect guidance and direction, leading to what is right and proper with supreme wisdom."
        ),
        98 to NameDetails(
            rootWord = "ص-ب-ر (Sad-Ba-Ra)",
            occurrence = "Appears 103 times in the Quran",
            significance = "As-Saboor means The Most Patient. This name indicates Allah's perfect patience, never being hasty in punishment, giving time for repentance, and dealing with creation according to wisdom rather than immediate reaction."
        )
    )

    fun getNameDetails(index: Int): NameDetails {
        return nameDetails[index] ?: NameDetails(
            rootWord = "Root word information being researched",
            occurrence = "Occurrence information being verified",
            significance = "This blessed name represents one of Allah's perfect attributes, reflecting His divine nature and perfect qualities."
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NamesOfAllahRowPreview() {
    MaterialTheme {
        Column {
            NamesOfAllahRow(1, "Al 'Aleem", "العليم", "The All Knowing")
            NamesOfAllahRow(2, "Ar-Rahman", "الرحمن", "The Most Merciful")
        }
    }
}