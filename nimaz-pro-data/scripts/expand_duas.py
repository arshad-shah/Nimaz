#!/usr/bin/env python3
"""
Nimaz Pro - Dua Dataset Expansion
=================================

Expands json/duas.json and json/dua_categories.json with additional
authentic supplications (adhkar) sourced from the Qur'an and the major
hadith collections (Hisnul Muslim / Fortress of the Muslim compilation).

Design goals:
  * Existing categories (ids 1-15) and duas (ids 1-64) are preserved
    EXACTLY as-is. New content is appended only.
  * New categories continue from id 16; new duas continue from id 65.
  * display_order is computed per-category so additions to an existing
    category continue after its current maximum order.
  * dua_count on every category is recomputed from the final dataset.

Every entry cites a primary source (Qur'an reference or hadith
collection). Texts follow the widely published Hisnul Muslim wording.

Run from anywhere; paths are resolved relative to this file.
"""

import json
from pathlib import Path

JSON_DIR = Path(__file__).resolve().parent.parent / "json"
DUAS_PATH = JSON_DIR / "duas.json"
CATEGORIES_PATH = JSON_DIR / "dua_categories.json"


# ---------------------------------------------------------------------------
# New categories (appended after the existing 15). Order matters: ids and
# display_order are assigned sequentially in this list's order.
# ---------------------------------------------------------------------------
NEW_CATEGORIES = [
    {"name_english": "Ablution (Wudu)", "name_arabic": "أذكار الوضوء", "icon": "🚿"},
    {"name_english": "Toilet", "name_arabic": "أذكار الخلاء", "icon": "🚻"},
    {"name_english": "Adhan", "name_arabic": "أذكار الأذان", "icon": "📣"},
    {"name_english": "Clothing", "name_arabic": "دعاء اللباس", "icon": "👕"},
    {"name_english": "Istikharah", "name_arabic": "دعاء الاستخارة", "icon": "🌟"},
    {"name_english": "Sneezing", "name_arabic": "أذكار العطاس", "icon": "🤧"},
    {"name_english": "Knowledge & Guidance", "name_arabic": "دعاء العلم والهداية", "icon": "📖"},
    {"name_english": "Provision & Sustenance", "name_arabic": "دعاء الرزق", "icon": "💰"},
    {"name_english": "Sickness & Healing", "name_arabic": "دعاء الشفاء", "icon": "💊"},
    {"name_english": "Hardship & Calamity", "name_arabic": "دعاء المصيبة", "icon": "🕊️"},
    {"name_english": "Anger", "name_arabic": "دعاء الغضب", "icon": "😤"},
    {"name_english": "Debt", "name_arabic": "دعاء قضاء الدين", "icon": "💳"},
    {"name_english": "Quranic Supplications", "name_arabic": "أدعية قرآنية", "icon": "📜"},
    {"name_english": "Comprehensive Duas", "name_arabic": "أدعية جامعة", "icon": "🌿"},
    {"name_english": "Marriage & Children", "name_arabic": "دعاء الزواج والذرية", "icon": "👨‍👩‍👧"},
    {"name_english": "Hajj & Umrah", "name_arabic": "دعاء الحج والعمرة", "icon": "🐫"},
    {"name_english": "Salawat on the Prophet ﷺ", "name_arabic": "الصلاة على النبي ﷺ", "icon": "🌹"},
    {"name_english": "Protection & Refuge", "name_arabic": "التحصين والاستعاذة", "icon": "🛡️"},
]


# Map category name -> additions for EXISTING categories (keyed by english name).
# These duas extend categories that already exist (ids 1-15).
ADDITIONS_TO_EXISTING = {
    "Morning Adhkar": [
        {
            "title_english": "Hasbiyallah (Morning)",
            "title_arabic": "حسبي الله لا إله إلا هو",
            "text_arabic": "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ، عَلَيْهِ تَوَكَّلْتُ، وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            "transliteration": "Hasbiyallahu la ilaha illa Huwa, 'alayhi tawakkaltu, wa Huwa Rabbul-'Arshil-'Adhim.",
            "translation": "Allah is sufficient for me. None has the right to be worshipped except Him. Upon Him I rely, and He is the Lord of the Magnificent Throne.",
            "source": "Abu Dawud",
            "virtue": "Whoever says this seven times in the morning and evening, Allah will suffice him in whatever worries him.",
            "repeat_count": 7,
        },
        {
            "title_english": "Ya Hayyu Ya Qayyum (Morning)",
            "title_arabic": "يا حي يا قيوم",
            "text_arabic": "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ، أَصْلِحْ لِي شَأْنِي كُلَّهُ، وَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ",
            "transliteration": "Ya Hayyu ya Qayyumu birahmatika astaghith, aslih li sha'ni kullahu, wa la takilni ila nafsi tarfata 'ayn.",
            "translation": "O Ever-Living, O Sustainer, by Your mercy I seek help. Rectify for me all of my affairs and do not leave me to myself even for the blink of an eye.",
            "source": "An-Nasai (Amal al-Yawm wal-Laylah), Al-Hakim",
            "virtue": "A supplication seeking Allah's constant care, said morning and evening.",
            "repeat_count": 1,
        },
        {
            "title_english": "Seeking Wellbeing & Security",
            "title_arabic": "اللهم إني أسألك العفو والعافية",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي، اللَّهُمَّ اسْتُرْ عَوْرَاتِي، وَآمِنْ رَوْعَاتِي",
            "transliteration": "Allahumma inni as'alukal-'afwa wal-'afiyah fid-dunya wal-akhirah. Allahumma inni as'alukal-'afwa wal-'afiyah fi dini wa dunyaya wa ahli wa mali. Allahumma-stur 'awrati, wa amin raw'ati.",
            "translation": "O Allah, I ask You for pardon and wellbeing in this world and the next. O Allah, I ask You for pardon and wellbeing in my religion, my worldly affairs, my family and my wealth. O Allah, conceal my faults and keep me safe from what I fear.",
            "source": "Abu Dawud, Ibn Majah",
            "virtue": "The Prophet ﷺ never abandoned these words morning and evening.",
            "repeat_count": 1,
        },
        {
            "title_english": "Knowing Allah's Names of Protection",
            "title_arabic": "اللهم عالم الغيب والشهادة",
            "text_arabic": "اللَّهُمَّ عَالِمَ الْغَيْبِ وَالشَّهَادَةِ، فَاطِرَ السَّمَاوَاتِ وَالْأَرْضِ، رَبَّ كُلِّ شَيْءٍ وَمَلِيكَهُ، أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا أَنْتَ، أَعُوذُ بِكَ مِنْ شَرِّ نَفْسِي، وَمِنْ شَرِّ الشَّيْطَانِ وَشِرْكِهِ، وَأَنْ أَقْتَرِفَ عَلَى نَفْسِي سُوءًا، أَوْ أَجُرَّهُ إِلَى مُسْلِمٍ",
            "transliteration": "Allahumma 'Alimal-ghaybi wash-shahadah, Fatiras-samawati wal-ard, Rabba kulli shay'in wa malikah, ashhadu an la ilaha illa anta, a'udhu bika min sharri nafsi, wa min sharrish-shaytani wa shirkih, wa an aqtarifa 'ala nafsi su'an, aw ajurrahu ila Muslim.",
            "translation": "O Allah, Knower of the unseen and the seen, Creator of the heavens and the earth, Lord and Sovereign of all things, I bear witness that none has the right to be worshipped except You. I seek refuge in You from the evil of my soul, and from the evil of Satan and his incitement to associate partners with You, and lest I bring evil upon myself or drag it to a Muslim.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "The Prophet ﷺ taught this to be said morning, evening and before sleep.",
            "repeat_count": 1,
        },
    ],
    "Evening Adhkar": [
        {
            "title_english": "Sayyidul Istighfar (Evening)",
            "title_arabic": "سيد الاستغفار مساءً",
            "text_arabic": "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَٰهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            "transliteration": "Allahumma anta Rabbi la ilaha illa anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bini'matika 'alayya, wa abu'u bidhanbi faghfir li fa'innahu la yaghfirudh-dhunuba illa anta.",
            "translation": "O Allah, You are my Lord, none has the right to be worshipped except You. You created me and I am Your servant, and I abide by Your covenant and promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your favor upon me and I acknowledge my sin, so forgive me, for none can forgive sins except You.",
            "source": "Bukhari",
            "virtue": "Whoever says it in the evening with conviction and dies that night enters Paradise.",
            "repeat_count": 1,
        },
        {
            "title_english": "Protection from Evil (Evening)",
            "title_arabic": "أعوذ بكلمات الله مساءً",
            "text_arabic": "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "transliteration": "A'udhu bikalimatillahit-tammati min sharri ma khalaq.",
            "translation": "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            "source": "Muslim",
            "virtue": "Whoever says it three times in the evening will not be harmed by any sting that night.",
            "repeat_count": 3,
        },
        {
            "title_english": "Wellbeing & Security (Evening)",
            "title_arabic": "اللهم إني أسألك العافية مساءً",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي الدُّنْيَا وَالْآخِرَةِ، اللَّهُمَّ إِنِّي أَسْأَلُكَ الْعَفْوَ وَالْعَافِيَةَ فِي دِينِي وَدُنْيَايَ وَأَهْلِي وَمَالِي",
            "transliteration": "Allahumma inni as'alukal-'afwa wal-'afiyah fid-dunya wal-akhirah. Allahumma inni as'alukal-'afwa wal-'afiyah fi dini wa dunyaya wa ahli wa mali.",
            "translation": "O Allah, I ask You for pardon and wellbeing in this world and the next. O Allah, I ask You for pardon and wellbeing in my religion, my worldly affairs, my family and my wealth.",
            "source": "Abu Dawud, Ibn Majah",
            "virtue": "Said by the Prophet ﷺ each evening.",
            "repeat_count": 1,
        },
        {
            "title_english": "Hasbiyallah (Evening)",
            "title_arabic": "حسبي الله مساءً",
            "text_arabic": "حَسْبِيَ اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ، عَلَيْهِ تَوَكَّلْتُ، وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ",
            "transliteration": "Hasbiyallahu la ilaha illa Huwa, 'alayhi tawakkaltu, wa Huwa Rabbul-'Arshil-'Adhim.",
            "translation": "Allah is sufficient for me. None has the right to be worshipped except Him. Upon Him I rely, and He is the Lord of the Magnificent Throne.",
            "source": "Abu Dawud",
            "virtue": "Whoever says this seven times in the evening, Allah will suffice him in whatever worries him.",
            "repeat_count": 7,
        },
        {
            "title_english": "Reaching Evening upon Fitrah",
            "title_arabic": "أمسينا على فطرة الإسلام",
            "text_arabic": "أَمْسَيْنَا عَلَى فِطْرَةِ الْإِسْلَامِ، وَعَلَى كَلِمَةِ الْإِخْلَاصِ، وَعَلَى دِينِ نَبِيِّنَا مُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ، وَعَلَى مِلَّةِ أَبِينَا إِبْرَاهِيمَ حَنِيفًا مُسْلِمًا وَمَا كَانَ مِنَ الْمُشْرِكِينَ",
            "transliteration": "Amsayna 'ala fitratil-Islam, wa 'ala kalimatil-ikhlas, wa 'ala dini nabiyyina Muhammadin sallallahu 'alayhi wa sallam, wa 'ala millati abina Ibrahima hanifan musliman wa ma kana minal-mushrikin.",
            "translation": "We have reached the evening upon the natural religion of Islam, the word of pure faith, the religion of our Prophet Muhammad ﷺ, and the way of our father Ibrahim, who was upright, a Muslim, and not of those who associate partners with Allah.",
            "source": "Ahmad",
            "virtue": "A renewal of faith upon reaching the evening.",
            "repeat_count": 1,
        },
        {
            "title_english": "Contentment with Allah (Evening)",
            "title_arabic": "رضيت بالله ربا مساءً",
            "text_arabic": "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا",
            "transliteration": "Raditu billahi Rabba, wa bil-Islami dina, wa bi-Muhammadin sallallahu 'alayhi wa sallama nabiyya.",
            "translation": "I am pleased with Allah as my Lord, with Islam as my religion, and with Muhammad ﷺ as my Prophet.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "Allah has promised to please whoever says this three times morning and evening.",
            "repeat_count": 3,
        },
    ],
    "After Prayer": [
        {
            "title_english": "Help in Remembering Allah",
            "title_arabic": "اللهم أعني على ذكرك",
            "text_arabic": "اللَّهُمَّ أَعِنِّي عَلَى ذِكْرِكَ، وَشُكْرِكَ، وَحُسْنِ عِبَادَتِكَ",
            "transliteration": "Allahumma a'inni 'ala dhikrika, wa shukrika, wa husni 'ibadatik.",
            "translation": "O Allah, help me to remember You, to thank You, and to worship You in the best manner.",
            "source": "Abu Dawud, An-Nasai",
            "virtue": "The Prophet ﷺ advised Mu'adh to never omit this after every prayer.",
            "repeat_count": 1,
        },
        {
            "title_english": "Refuge after Prayer (Comprehensive)",
            "title_arabic": "اللهم إني أعوذ بك من الكفر",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ، وَالْفَقْرِ، وَعَذَابِ الْقَبْرِ",
            "transliteration": "Allahumma inni a'udhu bika minal-kufr, wal-faqr, wa 'adhabil-qabr.",
            "translation": "O Allah, I seek refuge in You from disbelief, poverty, and the punishment of the grave.",
            "source": "An-Nasai, Ahmad",
            "virtue": "Said seeking protection in this life and the next.",
            "repeat_count": 3,
        },
    ],
    "Before Sleep": [
        {
            "title_english": "Tasbih of Fatimah",
            "title_arabic": "تسبيح فاطمة",
            "text_arabic": "سُبْحَانَ اللَّهِ (٣٣)، وَالْحَمْدُ لِلَّهِ (٣٣)، وَاللَّهُ أَكْبَرُ (٣٤)",
            "transliteration": "SubhanAllah (33), wal-hamdulillah (33), wallahu Akbar (34).",
            "translation": "Glory be to Allah (33 times), all praise is for Allah (33 times), Allah is the Greatest (34 times).",
            "source": "Bukhari & Muslim",
            "virtue": "The Prophet ﷺ taught this to Fatimah and 'Ali as better for them than a servant.",
            "repeat_count": 1,
        },
        {
            "title_english": "The Three Quls and Wiping the Body",
            "title_arabic": "المعوذات والنفث",
            "text_arabic": "قُلْ هُوَ اللَّهُ أَحَدٌ، قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ، قُلْ أَعُوذُ بِرَبِّ النَّاسِ",
            "transliteration": "Qul Huwa Allahu Ahad, Qul a'udhu bi Rabbil-Falaq, Qul a'udhu bi Rabbin-Nas.",
            "translation": "Recite Surah Al-Ikhlas, Al-Falaq and An-Nas, then blow into the palms and wipe over the body, starting with the head, face and front, three times.",
            "source": "Bukhari",
            "virtue": "The Prophet ﷺ did this every night before sleeping.",
            "repeat_count": 3,
        },
        {
            "title_english": "Seeking Refuge from the Evil of Creation",
            "title_arabic": "أعوذ بكلمات الله التامات قبل النوم",
            "text_arabic": "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            "transliteration": "A'udhu bikalimatillahit-tammati min sharri ma khalaq.",
            "translation": "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            "source": "Muslim",
            "virtue": "Protection through the night from every harm.",
            "repeat_count": 3,
        },
    ],
    "Distress & Anxiety": [
        {
            "title_english": "Calling upon the Ever-Living",
            "title_arabic": "يا حي يا قيوم برحمتك أستغيث",
            "text_arabic": "يَا حَيُّ يَا قَيُّومُ بِرَحْمَتِكَ أَسْتَغِيثُ",
            "transliteration": "Ya Hayyu ya Qayyumu birahmatika astaghith.",
            "translation": "O Ever-Living, O Sustainer of all, by Your mercy I seek relief.",
            "source": "Tirmidhi, Al-Hakim",
            "virtue": "The Prophet ﷺ would say this in times of distress.",
            "repeat_count": 1,
        },
        {
            "title_english": "Hoping in Allah's Mercy",
            "title_arabic": "اللهم رحمتك أرجو",
            "text_arabic": "اللَّهُمَّ رَحْمَتَكَ أَرْجُو، فَلَا تَكِلْنِي إِلَى نَفْسِي طَرْفَةَ عَيْنٍ، وَأَصْلِحْ لِي شَأْنِي كُلَّهُ، لَا إِلَٰهَ إِلَّا أَنْتَ",
            "transliteration": "Allahumma rahmataka arju, fala takilni ila nafsi tarfata 'ayn, wa aslih li sha'ni kullah, la ilaha illa anta.",
            "translation": "O Allah, I hope for Your mercy, so do not leave me to myself even for the blink of an eye, and rectify all of my affairs. None has the right to be worshipped except You.",
            "source": "Abu Dawud",
            "virtue": "A supplication for relief at times of worry.",
            "repeat_count": 1,
        },
    ],
    "Forgiveness": [
        {
            "title_english": "The Repentance of Adam",
            "title_arabic": "دعاء آدم عليه السلام",
            "text_arabic": "رَبَّنَا ظَلَمْنَا أَنْفُسَنَا وَإِنْ لَمْ تَغْفِرْ لَنَا وَتَرْحَمْنَا لَنَكُونَنَّ مِنَ الْخَاسِرِينَ",
            "transliteration": "Rabbana zalamna anfusana wa il lam taghfir lana wa tarhamna lanakunanna minal-khasirin.",
            "translation": "Our Lord, we have wronged ourselves, and if You do not forgive us and have mercy upon us, we will surely be among the losers.",
            "source": "Quran 7:23",
            "virtue": "The supplication by which Adam and Hawwa turned to Allah in repentance.",
            "repeat_count": 1,
        },
        {
            "title_english": "Astaghfirullah al-Adhim",
            "title_arabic": "أستغفر الله العظيم",
            "text_arabic": "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ",
            "transliteration": "Astaghfirullahal-'Adhim alladhi la ilaha illa Huwal-Hayyul-Qayyumu wa atubu ilayh.",
            "translation": "I seek the forgiveness of Allah the Magnificent, whom none has the right to be worshipped except Him, the Ever-Living, the Sustainer, and I repent to Him.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "Whoever says it, Allah forgives him even if he had fled from battle.",
            "repeat_count": 1,
        },
    ],
}


# Map new category english name -> its list of duas.
NEW_DUAS = {
    "Ablution (Wudu)": [
        {
            "title_english": "Before Ablution",
            "title_arabic": "دعاء قبل الوضوء",
            "text_arabic": "بِسْمِ اللَّهِ",
            "transliteration": "Bismillah.",
            "translation": "In the name of Allah.",
            "source": "Abu Dawud, Ibn Majah",
            "virtue": "There is no ablution for the one who does not mention Allah's name over it.",
            "repeat_count": 1,
        },
        {
            "title_english": "After Ablution",
            "title_arabic": "دعاء بعد الوضوء",
            "text_arabic": "أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَشْهَدُ أَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، اللَّهُمَّ اجْعَلْنِي مِنَ التَّوَّابِينَ، وَاجْعَلْنِي مِنَ الْمُتَطَهِّرِينَ",
            "transliteration": "Ashhadu an la ilaha illallahu wahdahu la sharika lah, wa ashhadu anna Muhammadan 'abduhu wa rasuluh. Allahumma-j'alni minat-tawwabin, waj'alni minal-mutatahhirin.",
            "translation": "I bear witness that none has the right to be worshipped except Allah alone, without partner, and I bear witness that Muhammad is His servant and Messenger. O Allah, make me of those who turn to You in repentance and make me of those who purify themselves.",
            "source": "Muslim, Tirmidhi",
            "virtue": "The eight gates of Paradise are opened for whoever says this after ablution, to enter by whichever he wishes.",
            "repeat_count": 1,
        },
    ],
    "Toilet": [
        {
            "title_english": "Entering the Toilet",
            "title_arabic": "دعاء دخول الخلاء",
            "text_arabic": "بِسْمِ اللَّهِ، اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبُثِ وَالْخَبَائِثِ",
            "transliteration": "Bismillah, Allahumma inni a'udhu bika minal-khubuthi wal-khaba'ith.",
            "translation": "In the name of Allah. O Allah, I seek refuge in You from the male and female devils.",
            "source": "Bukhari & Muslim",
            "virtue": "A screen between the eyes of the jinn and the private parts of the children of Adam.",
            "repeat_count": 1,
        },
        {
            "title_english": "Leaving the Toilet",
            "title_arabic": "دعاء الخروج من الخلاء",
            "text_arabic": "غُفْرَانَكَ",
            "transliteration": "Ghufranak.",
            "translation": "I seek Your forgiveness.",
            "source": "Abu Dawud, Tirmidhi, Ibn Majah",
            "virtue": "Said by the Prophet ﷺ upon leaving the toilet.",
            "repeat_count": 1,
        },
    ],
    "Adhan": [
        {
            "title_english": "Repeating after the Muezzin",
            "title_arabic": "ترديد الأذان",
            "text_arabic": "يُرَدِّدُ مِثْلَ مَا يَقُولُ الْمُؤَذِّنُ، إِلَّا فِي الْحَيْعَلَتَيْنِ فَيَقُولُ: لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            "transliteration": "Repeat what the muezzin says, except at 'Hayya 'alas-salah' and 'Hayya 'alal-falah', where you say: La hawla wa la quwwata illa billah.",
            "translation": "Repeat after the muezzin word for word; but when he says 'Come to prayer' and 'Come to success', say: 'There is no might nor power except with Allah.'",
            "source": "Bukhari & Muslim",
            "virtue": "Whoever repeats after the muezzin from his heart enters Paradise.",
            "repeat_count": 1,
        },
        {
            "title_english": "Testimony upon Hearing the Adhan",
            "title_arabic": "دعاء بعد الشهادتين في الأذان",
            "text_arabic": "وَأَنَا أَشْهَدُ أَنْ لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، وَأَنَّ مُحَمَّدًا عَبْدُهُ وَرَسُولُهُ، رَضِيتُ بِاللَّهِ رَبًّا، وَبِمُحَمَّدٍ رَسُولًا، وَبِالْإِسْلَامِ دِينًا",
            "transliteration": "Wa ana ashhadu an la ilaha illallahu wahdahu la sharika lah, wa anna Muhammadan 'abduhu wa rasuluh, raditu billahi Rabba, wa bi-Muhammadin rasula, wa bil-Islami dina.",
            "translation": "And I too bear witness that none has the right to be worshipped except Allah alone, without partner, and that Muhammad is His servant and Messenger. I am pleased with Allah as my Lord, with Muhammad as Messenger, and with Islam as my religion.",
            "source": "Muslim",
            "virtue": "Whoever says this upon hearing the testimony in the adhan, his sins are forgiven.",
            "repeat_count": 1,
        },
        {
            "title_english": "Dua after the Adhan (Al-Wasilah)",
            "title_arabic": "دعاء بعد الأذان",
            "text_arabic": "اللَّهُمَّ رَبَّ هَٰذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            "transliteration": "Allahumma Rabba hadhihid-da'watit-tammah, wassalatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah, wab'athhu maqaman mahmudanil-ladhi wa'adtah.",
            "translation": "O Allah, Lord of this perfect call and established prayer, grant Muhammad the intercession and favor, and raise him to the praised station which You have promised him.",
            "source": "Bukhari",
            "virtue": "Whoever says this after the adhan, the Prophet's intercession becomes due for him on the Day of Resurrection.",
            "repeat_count": 1,
        },
    ],
    "Clothing": [
        {
            "title_english": "When Getting Dressed",
            "title_arabic": "دعاء لبس الثوب",
            "text_arabic": "الْحَمْدُ لِلَّهِ الَّذِي كَسَانِي هَٰذَا وَرَزَقَنِيهِ مِنْ غَيْرِ حَوْلٍ مِنِّي وَلَا قُوَّةٍ",
            "transliteration": "Alhamdulillahil-ladhi kasani hadha wa razaqanihi min ghayri hawlin minni wa la quwwah.",
            "translation": "All praise is for Allah who has clothed me with this and provided it for me without any might or power on my part.",
            "source": "Abu Dawud, Tirmidhi, Ibn Majah",
            "virtue": "Whoever says this when wearing a garment, his past sins are forgiven.",
            "repeat_count": 1,
        },
        {
            "title_english": "When Wearing New Clothes",
            "title_arabic": "دعاء لبس الثوب الجديد",
            "text_arabic": "اللَّهُمَّ لَكَ الْحَمْدُ أَنْتَ كَسَوْتَنِيهِ، أَسْأَلُكَ مِنْ خَيْرِهِ وَخَيْرِ مَا صُنِعَ لَهُ، وَأَعُوذُ بِكَ مِنْ شَرِّهِ وَشَرِّ مَا صُنِعَ لَهُ",
            "transliteration": "Allahumma lakal-hamdu anta kasawtanih, as'aluka min khayrihi wa khayri ma suni'a lah, wa a'udhu bika min sharrihi wa sharri ma suni'a lah.",
            "translation": "O Allah, all praise is for You; You have clothed me with it. I ask You for its good and the good for which it was made, and I seek refuge in You from its evil and the evil for which it was made.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "Said when putting on a new garment.",
            "repeat_count": 1,
        },
    ],
    "Istikharah": [
        {
            "title_english": "Prayer of Seeking Guidance (Istikharah)",
            "title_arabic": "دعاء صلاة الاستخارة",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْتَخِيرُكَ بِعِلْمِكَ، وَأَسْتَقْدِرُكَ بِقُدْرَتِكَ، وَأَسْأَلُكَ مِنْ فَضْلِكَ الْعَظِيمِ، فَإِنَّكَ تَقْدِرُ وَلَا أَقْدِرُ، وَتَعْلَمُ وَلَا أَعْلَمُ، وَأَنْتَ عَلَّامُ الْغُيُوبِ. اللَّهُمَّ إِنْ كُنْتَ تَعْلَمُ أَنَّ هَٰذَا الْأَمْرَ خَيْرٌ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي فَاقْدُرْهُ لِي وَيَسِّرْهُ لِي ثُمَّ بَارِكْ لِي فِيهِ، وَإِنْ كُنْتَ تَعْلَمُ أَنَّ هَٰذَا الْأَمْرَ شَرٌّ لِي فِي دِينِي وَمَعَاشِي وَعَاقِبَةِ أَمْرِي فَاصْرِفْهُ عَنِّي وَاصْرِفْنِي عَنْهُ، وَاقْدُرْ لِيَ الْخَيْرَ حَيْثُ كَانَ ثُمَّ أَرْضِنِي بِهِ",
            "transliteration": "Allahumma inni astakhiruka bi'ilmik, wa astaqdiruka biqudratik, wa as'aluka min fadlikal-'adhim, fa innaka taqdiru wa la aqdir, wa ta'lamu wa la a'lam, wa anta 'allamul-ghuyub. Allahumma in kunta ta'lamu anna hadhal-amra khayrun li fi dini wa ma'ashi wa 'aqibati amri faqdurhu li wa yassirhu li thumma barik li fih, wa in kunta ta'lamu anna hadhal-amra sharrun li fi dini wa ma'ashi wa 'aqibati amri fasrifhu 'anni wasrifni 'anhu, waqdur liyal-khayra haythu kana thumma ardini bih.",
            "translation": "O Allah, I seek Your guidance by Your knowledge, and I seek ability by Your power, and I ask You of Your great bounty. You have power, I have none; You know, I know not; You are the Knower of the unseen. O Allah, if You know that this matter is good for me in my religion, my livelihood and the outcome of my affairs, then decree it for me, make it easy for me, and bless it for me. And if You know that this matter is bad for me in my religion, my livelihood and the outcome of my affairs, then turn it away from me and turn me away from it, and decree for me what is good wherever it may be, and make me content with it.",
            "source": "Bukhari",
            "virtue": "The Prophet ﷺ taught Istikharah for all matters as he taught a chapter of the Qur'an; mention your need where the dua refers to 'this matter'.",
            "repeat_count": 1,
        },
    ],
    "Sneezing": [
        {
            "title_english": "When You Sneeze",
            "title_arabic": "دعاء العاطس",
            "text_arabic": "الْحَمْدُ لِلَّهِ",
            "transliteration": "Alhamdulillah.",
            "translation": "All praise is for Allah.",
            "source": "Bukhari",
            "virtue": "When one sneezes he should praise Allah, and his Muslim brother should respond.",
            "repeat_count": 1,
        },
        {
            "title_english": "Response of the One Who Sneezed",
            "title_arabic": "رد العاطس على من شمته",
            "text_arabic": "يَهْدِيكُمُ اللَّهُ وَيُصْلِحُ بَالَكُمْ",
            "transliteration": "Yahdikumullahu wa yuslihu balakum.",
            "translation": "May Allah guide you and rectify your affairs.",
            "source": "Bukhari",
            "virtue": "When the one who sneezed praises Allah and is told 'YarhamukAllah' (may Allah have mercy on you), he replies with this.",
            "repeat_count": 1,
        },
    ],
    "Knowledge & Guidance": [
        {
            "title_english": "Asking for Increase in Knowledge",
            "title_arabic": "رب زدني علما",
            "text_arabic": "رَبِّ زِدْنِي عِلْمًا",
            "transliteration": "Rabbi zidni 'ilma.",
            "translation": "My Lord, increase me in knowledge.",
            "source": "Quran 20:114",
            "virtue": "A concise Qur'anic supplication for beneficial knowledge.",
            "repeat_count": 1,
        },
        {
            "title_english": "Benefiting from Knowledge",
            "title_arabic": "اللهم انفعني بما علمتني",
            "text_arabic": "اللَّهُمَّ انْفَعْنِي بِمَا عَلَّمْتَنِي، وَعَلِّمْنِي مَا يَنْفَعُنِي، وَزِدْنِي عِلْمًا",
            "transliteration": "Allahumma-nfa'ni bima 'allamtani, wa 'allimni ma yanfa'uni, wa zidni 'ilma.",
            "translation": "O Allah, benefit me by what You have taught me, teach me what will benefit me, and increase me in knowledge.",
            "source": "Tirmidhi, Ibn Majah",
            "virtue": "A supplication of the Prophet ﷺ for beneficial knowledge.",
            "repeat_count": 1,
        },
        {
            "title_english": "Beneficial Knowledge & Good Provision",
            "title_arabic": "اللهم إني أسألك علما نافعا",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا، وَرِزْقًا طَيِّبًا، وَعَمَلًا مُتَقَبَّلًا",
            "transliteration": "Allahumma inni as'aluka 'ilman nafi'an, wa rizqan tayyiban, wa 'amalan mutaqabbala.",
            "translation": "O Allah, I ask You for beneficial knowledge, good provision, and deeds that are accepted.",
            "source": "Ibn Majah",
            "virtue": "The Prophet ﷺ would say this after the Fajr prayer.",
            "repeat_count": 1,
        },
        {
            "title_english": "Steadfastness of the Heart",
            "title_arabic": "يا مقلب القلوب ثبت قلبي",
            "text_arabic": "يَا مُقَلِّبَ الْقُلُوبِ ثَبِّتْ قَلْبِي عَلَى دِينِكَ",
            "transliteration": "Ya muqallibal-qulubi thabbit qalbi 'ala dinik.",
            "translation": "O Turner of the hearts, keep my heart firm upon Your religion.",
            "source": "Tirmidhi",
            "virtue": "The Prophet ﷺ frequently supplicated with these words.",
            "repeat_count": 1,
        },
    ],
    "Provision & Sustenance": [
        {
            "title_english": "Asking Allah for Provision",
            "title_arabic": "رب إني لما أنزلت إلي من خير فقير",
            "text_arabic": "رَبِّ إِنِّي لِمَا أَنْزَلْتَ إِلَيَّ مِنْ خَيْرٍ فَقِيرٌ",
            "transliteration": "Rabbi inni lima anzalta ilayya min khayrin faqir.",
            "translation": "My Lord, indeed I am, for whatever good You would send down to me, in need.",
            "source": "Quran 28:24",
            "virtue": "The supplication of Musa عليه السلام when he was in need.",
            "repeat_count": 1,
        },
        {
            "title_english": "Provision from the Heavens",
            "title_arabic": "ربنا أنزل علينا مائدة من السماء",
            "text_arabic": "رَبَّنَا أَنْزِلْ عَلَيْنَا مَائِدَةً مِنَ السَّمَاءِ تَكُونُ لَنَا عِيدًا لِأَوَّلِنَا وَآخِرِنَا وَآيَةً مِنْكَ، وَارْزُقْنَا وَأَنْتَ خَيْرُ الرَّازِقِينَ",
            "transliteration": "Rabbana anzil 'alayna ma'idatan minas-sama'i takunu lana 'idan li'awwalina wa akhirina wa ayatan mink, warzuqna wa anta khayrur-raziqin.",
            "translation": "Our Lord, send down to us a table spread with food from the heaven to be for us a festival for the first of us and the last of us and a sign from You. And provide for us, for You are the best of providers.",
            "source": "Quran 5:114",
            "virtue": "The supplication of Isa عليه السلام asking Allah for provision.",
            "repeat_count": 1,
        },
    ],
    "Sickness & Healing": [
        {
            "title_english": "Placing the Hand on the Pain",
            "title_arabic": "دعاء وضع اليد على موضع الألم",
            "text_arabic": "بِسْمِ اللَّهِ (ثَلَاثًا) أَعُوذُ بِاللَّهِ وَقُدْرَتِهِ مِنْ شَرِّ مَا أَجِدُ وَأُحَاذِرُ (سَبْعًا)",
            "transliteration": "Bismillah (three times). A'udhu billahi wa qudratihi min sharri ma ajidu wa uhadhir (seven times).",
            "translation": "In the name of Allah (three times). I seek refuge in Allah and His power from the evil of what I feel and fear (seven times).",
            "source": "Muslim",
            "virtue": "Place the hand on the place of pain and say it; it removes the ailment.",
            "repeat_count": 1,
        },
        {
            "title_english": "Supplication for the Sick (Ruqyah)",
            "title_arabic": "دعاء المريض",
            "text_arabic": "اللَّهُمَّ رَبَّ النَّاسِ، أَذْهِبِ الْبَأْسَ، اشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            "transliteration": "Allahumma Rabban-nas, adhhibil-ba's, ishfi antash-Shafi, la shifa'a illa shifa'uk, shifa'an la yughadiru saqama.",
            "translation": "O Allah, Lord of mankind, remove the harm and heal; You are the Healer. There is no healing except Your healing, a healing that leaves behind no ailment.",
            "source": "Bukhari & Muslim",
            "virtue": "The Prophet ﷺ used this to seek healing for the sick.",
            "repeat_count": 1,
        },
        {
            "title_english": "Visiting the Sick",
            "title_arabic": "دعاء زيارة المريض",
            "text_arabic": "لَا بَأْسَ طَهُورٌ إِنْ شَاءَ اللَّهُ",
            "transliteration": "La ba'sa tahurun in sha'Allah.",
            "translation": "No harm, it is a purification, Allah willing.",
            "source": "Bukhari",
            "virtue": "The Prophet ﷺ would say this when visiting a sick person.",
            "repeat_count": 1,
        },
        {
            "title_english": "Asking Allah Seven Times for Healing",
            "title_arabic": "أسأل الله العظيم أن يشفيك",
            "text_arabic": "أَسْأَلُ اللَّهَ الْعَظِيمَ رَبَّ الْعَرْشِ الْعَظِيمِ أَنْ يَشْفِيَكَ",
            "transliteration": "As'alullahal-'Adhima Rabbal-'Arshil-'Adhimi an yashfiyak.",
            "translation": "I ask Allah the Magnificent, Lord of the Magnificent Throne, to heal you.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "Whoever visits a sick person whose time has not come and says this seven times, Allah will cure him of that illness.",
            "repeat_count": 7,
        },
    ],
    "Hardship & Calamity": [
        {
            "title_english": "Upon Being Struck by Calamity",
            "title_arabic": "دعاء المصيبة",
            "text_arabic": "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي، وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
            "transliteration": "Inna lillahi wa inna ilayhi raji'un. Allahumma'-jurni fi musibati, wa akhlif li khayran minha.",
            "translation": "Indeed we belong to Allah, and indeed to Him we shall return. O Allah, reward me for my affliction and give me something better than it in its place.",
            "source": "Muslim",
            "virtue": "Whoever says this when afflicted, Allah rewards him and replaces his loss with something better.",
            "repeat_count": 1,
        },
        {
            "title_english": "Trust during Difficult Affairs",
            "title_arabic": "اللهم لا سهل إلا ما جعلته سهلا",
            "text_arabic": "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا، وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
            "transliteration": "Allahumma la sahla illa ma ja'altahu sahla, wa anta taj'alul-hazna idha shi'ta sahla.",
            "translation": "O Allah, there is no ease except in what You make easy, and You make the difficult, if You will, easy.",
            "source": "Ibn Hibban",
            "virtue": "Said when facing a hard matter so that Allah makes it easy.",
            "repeat_count": 1,
        },
    ],
    "Anger": [
        {
            "title_english": "When Overcome by Anger",
            "title_arabic": "دعاء الغضب",
            "text_arabic": "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ",
            "transliteration": "A'udhu billahi minash-shaytanir-rajim.",
            "translation": "I seek refuge in Allah from the accursed devil.",
            "source": "Bukhari & Muslim",
            "virtue": "The Prophet ﷺ said that whoever says this when angry, his anger will leave him.",
            "repeat_count": 1,
        },
    ],
    "Debt": [
        {
            "title_english": "Relief from Debt",
            "title_arabic": "دعاء قضاء الدين",
            "text_arabic": "اللَّهُمَّ اكْفِنِي بِحَلَالِكَ عَنْ حَرَامِكَ، وَأَغْنِنِي بِفَضْلِكَ عَمَّنْ سِوَاكَ",
            "transliteration": "Allahumma-kfini bihalalika 'an haramik, wa aghnini bifadlika 'amman siwak.",
            "translation": "O Allah, suffice me with what You have made lawful instead of what You have made unlawful, and make me independent of all others besides You by Your bounty.",
            "source": "Tirmidhi",
            "virtue": "The Prophet ﷺ taught this to relieve a debt as great as a mountain.",
            "repeat_count": 1,
        },
        {
            "title_english": "Refuge from Debt & Sin",
            "title_arabic": "اللهم إني أعوذ بك من المأثم والمغرم",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْمَأْثَمِ وَالْمَغْرَمِ",
            "transliteration": "Allahumma inni a'udhu bika minal-ma'thami wal-maghram.",
            "translation": "O Allah, I seek refuge in You from sin and from being in debt.",
            "source": "Bukhari & Muslim",
            "virtue": "The Prophet ﷺ would seek refuge from debt, for one in debt may lie and break promises.",
            "repeat_count": 1,
        },
    ],
    "Quranic Supplications": [
        {
            "title_english": "Good in Both Worlds",
            "title_arabic": "ربنا آتنا في الدنيا حسنة",
            "text_arabic": "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            "transliteration": "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar.",
            "translation": "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            "source": "Quran 2:201",
            "virtue": "The supplication the Prophet ﷺ made most often.",
            "repeat_count": 1,
        },
        {
            "title_english": "Burden and Forgiveness",
            "title_arabic": "ربنا لا تؤاخذنا",
            "text_arabic": "رَبَّنَا لَا تُؤَاخِذْنَا إِنْ نَسِينَا أَوْ أَخْطَأْنَا، رَبَّنَا وَلَا تَحْمِلْ عَلَيْنَا إِصْرًا كَمَا حَمَلْتَهُ عَلَى الَّذِينَ مِنْ قَبْلِنَا، رَبَّنَا وَلَا تُحَمِّلْنَا مَا لَا طَاقَةَ لَنَا بِهِ، وَاعْفُ عَنَّا وَاغْفِرْ لَنَا وَارْحَمْنَا، أَنْتَ مَوْلَانَا فَانْصُرْنَا عَلَى الْقَوْمِ الْكَافِرِينَ",
            "transliteration": "Rabbana la tu'akhidhna in nasina aw akhta'na, Rabbana wa la tahmil 'alayna isran kama hamaltahu 'alal-ladhina min qablina, Rabbana wa la tuhammilna ma la taqata lana bih, wa'fu 'anna waghfir lana warhamna, anta mawlana fansurna 'alal-qawmil-kafirin.",
            "translation": "Our Lord, do not take us to task if we forget or err. Our Lord, do not lay upon us a burden like that which You laid upon those before us. Our Lord, do not burden us with what we have no power to bear. Pardon us, forgive us, and have mercy on us. You are our Protector, so grant us victory over the disbelieving people.",
            "source": "Quran 2:286",
            "virtue": "The closing verses of Surah Al-Baqarah; whoever recites them at night, they suffice him.",
            "repeat_count": 1,
        },
        {
            "title_english": "Firmness upon Guidance",
            "title_arabic": "ربنا لا تزغ قلوبنا",
            "text_arabic": "رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا وَهَبْ لَنَا مِنْ لَدُنْكَ رَحْمَةً، إِنَّكَ أَنْتَ الْوَهَّابُ",
            "transliteration": "Rabbana la tuzigh qulubana ba'da idh hadaytana wa hab lana min ladunka rahmah, innaka antal-Wahhab.",
            "translation": "Our Lord, do not let our hearts deviate after You have guided us, and grant us mercy from Yourself. Indeed, You are the Bestower.",
            "source": "Quran 3:8",
            "virtue": "A plea for steadfastness upon guidance.",
            "repeat_count": 1,
        },
        {
            "title_english": "Light upon Light",
            "title_arabic": "رب اجعلني مقيم الصلاة",
            "text_arabic": "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي، رَبَّنَا وَتَقَبَّلْ دُعَاءِ، رَبَّنَا اغْفِرْ لِي وَلِوَالِدَيَّ وَلِلْمُؤْمِنِينَ يَوْمَ يَقُومُ الْحِسَابُ",
            "transliteration": "Rabbi-j'alni muqimas-salati wa min dhurriyyati, Rabbana wa taqabbal du'a. Rabbana-ghfir li wa liwalidayya wa lil-mu'minina yawma yaqumul-hisab.",
            "translation": "My Lord, make me an establisher of prayer, and many from my descendants. Our Lord, accept my supplication. Our Lord, forgive me and my parents and the believers on the Day the account is established.",
            "source": "Quran 14:40-41",
            "virtue": "The supplication of Ibrahim عليه السلام for prayer and forgiveness.",
            "repeat_count": 1,
        },
        {
            "title_english": "Coolness of the Eyes",
            "title_arabic": "ربنا هب لنا من أزواجنا",
            "text_arabic": "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
            "transliteration": "Rabbana hab lana min azwajina wa dhurriyyatina qurrata a'yunin waj'alna lil-muttaqina imama.",
            "translation": "Our Lord, grant us from among our spouses and offspring comfort to our eyes, and make us leaders for the righteous.",
            "source": "Quran 25:74",
            "virtue": "Among the qualities of the servants of the Most Merciful.",
            "repeat_count": 1,
        },
        {
            "title_english": "Ease of Affairs (Musa)",
            "title_arabic": "رب اشرح لي صدري",
            "text_arabic": "رَبِّ اشْرَحْ لِي صَدْرِي، وَيَسِّرْ لِي أَمْرِي، وَاحْلُلْ عُقْدَةً مِنْ لِسَانِي، يَفْقَهُوا قَوْلِي",
            "transliteration": "Rabbi-shrah li sadri, wa yassir li amri, wahlul 'uqdatan min lisani, yafqahu qawli.",
            "translation": "My Lord, expand for me my chest, ease for me my task, and untie the knot from my tongue that they may understand my speech.",
            "source": "Quran 20:25-28",
            "virtue": "The supplication of Musa عليه السلام for confidence and clarity.",
            "repeat_count": 1,
        },
        {
            "title_english": "Mercy and Forgiveness",
            "title_arabic": "رب اغفر وارحم",
            "text_arabic": "رَبِّ اغْفِرْ وَارْحَمْ وَأَنْتَ خَيْرُ الرَّاحِمِينَ",
            "transliteration": "Rabbi-ghfir warham wa anta khayrur-rahimin.",
            "translation": "My Lord, forgive and have mercy, for You are the best of the merciful.",
            "source": "Quran 23:118",
            "virtue": "A concise plea for Allah's forgiveness and mercy.",
            "repeat_count": 1,
        },
        {
            "title_english": "Truthful Entrance and Exit",
            "title_arabic": "رب أدخلني مدخل صدق",
            "text_arabic": "رَبِّ أَدْخِلْنِي مُدْخَلَ صِدْقٍ وَأَخْرِجْنِي مُخْرَجَ صِدْقٍ وَاجْعَلْ لِي مِنْ لَدُنْكَ سُلْطَانًا نَصِيرًا",
            "transliteration": "Rabbi adkhilni mudkhala sidqin wa akhrijni mukhraja sidqin waj'al li min ladunka sultanan nasira.",
            "translation": "My Lord, cause me to enter a sound entrance and to exit a sound exit, and grant me from Yourself a supporting authority.",
            "source": "Quran 17:80",
            "virtue": "A supplication for sincerity and divine support in all affairs.",
            "repeat_count": 1,
        },
    ],
    "Comprehensive Duas": [
        {
            "title_english": "Guidance, Piety, Chastity & Sufficiency",
            "title_arabic": "اللهم إني أسألك الهدى والتقى",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ الْهُدَى، وَالتُّقَى، وَالْعَفَافَ، وَالْغِنَى",
            "transliteration": "Allahumma inni as'alukal-huda, wat-tuqa, wal-'afafa, wal-ghina.",
            "translation": "O Allah, I ask You for guidance, piety, chastity and self-sufficiency.",
            "source": "Muslim",
            "virtue": "A comprehensive supplication taught by the Prophet ﷺ.",
            "repeat_count": 1,
        },
        {
            "title_english": "Setting Right All Affairs",
            "title_arabic": "اللهم أصلح لي ديني",
            "text_arabic": "اللَّهُمَّ أَصْلِحْ لِي دِينِي الَّذِي هُوَ عِصْمَةُ أَمْرِي، وَأَصْلِحْ لِي دُنْيَايَ الَّتِي فِيهَا مَعَاشِي، وَأَصْلِحْ لِي آخِرَتِي الَّتِي فِيهَا مَعَادِي، وَاجْعَلِ الْحَيَاةَ زِيَادَةً لِي فِي كُلِّ خَيْرٍ، وَاجْعَلِ الْمَوْتَ رَاحَةً لِي مِنْ كُلِّ شَرٍّ",
            "transliteration": "Allahumma aslih li dinil-ladhi huwa 'ismatu amri, wa aslih li dunyayal-lati fiha ma'ashi, wa aslih li akhiratil-lati fiha ma'adi, waj'alil-hayata ziyadatan li fi kulli khayr, waj'alil-mawta rahatan li min kulli sharr.",
            "translation": "O Allah, set right for me my religion which is the safeguard of my affairs; set right for me my worldly life in which is my living; set right for me my Hereafter to which is my return; make life for me an increase in every good, and make death a relief for me from every evil.",
            "source": "Muslim",
            "virtue": "A complete supplication covering religion, this world and the next.",
            "repeat_count": 1,
        },
        {
            "title_english": "Every Good, Near and Far",
            "title_arabic": "اللهم إني أسألك من الخير كله",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنَ الْخَيْرِ كُلِّهِ عَاجِلِهِ وَآجِلِهِ مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ، وَأَعُوذُ بِكَ مِنَ الشَّرِّ كُلِّهِ عَاجِلِهِ وَآجِلِهِ مَا عَلِمْتُ مِنْهُ وَمَا لَمْ أَعْلَمْ",
            "transliteration": "Allahumma inni as'aluka minal-khayri kullihi 'ajilihi wa ajilihi ma 'alimtu minhu wa ma lam a'lam, wa a'udhu bika minash-sharri kullihi 'ajilihi wa ajilihi ma 'alimtu minhu wa ma lam a'lam.",
            "translation": "O Allah, I ask You for all good, in this world and the next, what I know of it and what I do not; and I seek refuge in You from all evil, in this world and the next, what I know of it and what I do not.",
            "source": "Ibn Majah, Ahmad",
            "virtue": "An all-encompassing request for good and refuge from evil.",
            "repeat_count": 1,
        },
        {
            "title_english": "Love of Allah and Righteous Deeds",
            "title_arabic": "اللهم إني أسألك فعل الخيرات",
            "text_arabic": "اللَّهُمَّ إِنِّي أَسْأَلُكَ فِعْلَ الْخَيْرَاتِ، وَتَرْكَ الْمُنْكَرَاتِ، وَحُبَّ الْمَسَاكِينِ، وَأَنْ تَغْفِرَ لِي وَتَرْحَمَنِي، وَإِذَا أَرَدْتَ فِتْنَةً فِي قَوْمٍ فَتَوَفَّنِي غَيْرَ مَفْتُونٍ، وَأَسْأَلُكَ حُبَّكَ، وَحُبَّ مَنْ يُحِبُّكَ، وَحُبَّ عَمَلٍ يُقَرِّبُ إِلَى حُبِّكَ",
            "transliteration": "Allahumma inni as'aluka fi'lal-khayrat, wa tarkal-munkarat, wa hubbal-masakin, wa an taghfira li wa tarhamani, wa idha aradta fitnatan fi qawmin fatawaffani ghayra maftun, wa as'aluka hubbaka, wa hubba man yuhibbuk, wa hubba 'amalin yuqarribu ila hubbik.",
            "translation": "O Allah, I ask You for the doing of good deeds, the avoidance of evil deeds, and love of the poor. Forgive me and have mercy on me, and when You intend a trial for a people, take me to You without being tried. I ask You for Your love, the love of those who love You, and the love of every deed that brings me closer to Your love.",
            "source": "Tirmidhi (graded Sahih)",
            "virtue": "From the hadith of the Prophet's dream; he commanded that it be learned and recited.",
            "repeat_count": 1,
        },
        {
            "title_english": "Refuge from the Removal of Blessings",
            "title_arabic": "اللهم إني أعوذ بك من زوال نعمتك",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ زَوَالِ نِعْمَتِكَ، وَتَحَوُّلِ عَافِيَتِكَ، وَفُجَاءَةِ نِقْمَتِكَ، وَجَمِيعِ سَخَطِكَ",
            "transliteration": "Allahumma inni a'udhu bika min zawali ni'matik, wa tahawwuli 'afiyatik, wa fuja'ati niqmatik, wa jami'i sakhatik.",
            "translation": "O Allah, I seek refuge in You from the removal of Your blessing, the change of the wellbeing You have granted, the suddenness of Your retribution, and from all that angers You.",
            "source": "Muslim",
            "virtue": "Seeking protection for the continuation of Allah's favours.",
            "repeat_count": 1,
        },
        {
            "title_english": "Refuge from Four Evils",
            "title_arabic": "اللهم إني أعوذ بك من علم لا ينفع",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ عِلْمٍ لَا يَنْفَعُ، وَمِنْ قَلْبٍ لَا يَخْشَعُ، وَمِنْ نَفْسٍ لَا تَشْبَعُ، وَمِنْ دَعْوَةٍ لَا يُسْتَجَابُ لَهَا",
            "transliteration": "Allahumma inni a'udhu bika min 'ilmin la yanfa', wa min qalbin la yakhsha', wa min nafsin la tashba', wa min da'watin la yustajabu laha.",
            "translation": "O Allah, I seek refuge in You from knowledge that does not benefit, a heart that does not humble itself, a soul that is never satisfied, and a supplication that is not answered.",
            "source": "Muslim",
            "virtue": "The Prophet ﷺ regularly sought refuge with these words.",
            "repeat_count": 1,
        },
    ],
    "Marriage & Children": [
        {
            "title_english": "Congratulating the Newly Married",
            "title_arabic": "دعاء التهنئة بالزواج",
            "text_arabic": "بَارَكَ اللَّهُ لَكَ، وَبَارَكَ عَلَيْكَ، وَجَمَعَ بَيْنَكُمَا فِي خَيْرٍ",
            "transliteration": "Barakallahu lak, wa baraka 'alayk, wa jama'a baynakuma fi khayr.",
            "translation": "May Allah bless you, and shower His blessings upon you, and join you both together in goodness.",
            "source": "Abu Dawud, Tirmidhi",
            "virtue": "The Prophet ﷺ would say this to congratulate someone on marriage.",
            "repeat_count": 1,
        },
        {
            "title_english": "Before Intimacy",
            "title_arabic": "دعاء ما قبل الجماع",
            "text_arabic": "بِسْمِ اللَّهِ، اللَّهُمَّ جَنِّبْنَا الشَّيْطَانَ، وَجَنِّبِ الشَّيْطَانَ مَا رَزَقْتَنَا",
            "transliteration": "Bismillah, Allahumma jannibnash-shaytan, wa jannibish-shaytana ma razaqtana.",
            "translation": "In the name of Allah. O Allah, keep Satan away from us, and keep Satan away from what You provide us.",
            "source": "Bukhari & Muslim",
            "virtue": "If a child is decreed between them, Satan will never harm it.",
            "repeat_count": 1,
        },
        {
            "title_english": "Seeking Refuge for Children",
            "title_arabic": "دعاء حفظ الأطفال",
            "text_arabic": "أُعِيذُكُمَا بِكَلِمَاتِ اللَّهِ التَّامَّةِ، مِنْ كُلِّ شَيْطَانٍ وَهَامَّةٍ، وَمِنْ كُلِّ عَيْنٍ لَامَّةٍ",
            "transliteration": "U'idhukuma bikalimatillahit-tammah, min kulli shaytanin wa hammah, wa min kulli 'aynin lammah.",
            "translation": "I seek protection for you both in the perfect words of Allah from every devil and every poisonous creature, and from every evil, envious eye.",
            "source": "Bukhari",
            "virtue": "The Prophet ﷺ would seek refuge for Hasan and Husayn with these words.",
            "repeat_count": 1,
        },
        {
            "title_english": "Asking for Righteous Offspring",
            "title_arabic": "رب هب لي من لدنك ذرية طيبة",
            "text_arabic": "رَبِّ هَبْ لِي مِنْ لَدُنْكَ ذُرِّيَّةً طَيِّبَةً، إِنَّكَ سَمِيعُ الدُّعَاءِ",
            "transliteration": "Rabbi hab li min ladunka dhurriyyatan tayyibah, innaka Sami'ud-du'a.",
            "translation": "My Lord, grant me from Yourself good offspring. Indeed, You are the Hearer of supplication.",
            "source": "Quran 3:38",
            "virtue": "The supplication of Zakariyya عليه السلام for righteous children.",
            "repeat_count": 1,
        },
    ],
    "Hajj & Umrah": [
        {
            "title_english": "The Talbiyah",
            "title_arabic": "التلبية",
            "text_arabic": "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
            "transliteration": "Labbayka Allahumma labbayk, labbayka la sharika laka labbayk, innal-hamda wan-ni'mata laka wal-mulk, la sharika lak.",
            "translation": "Here I am, O Allah, here I am. Here I am, You have no partner, here I am. Indeed all praise, grace and sovereignty belong to You. You have no partner.",
            "source": "Bukhari & Muslim",
            "virtue": "The call of the pilgrim during Hajj and Umrah until the stoning of Jamratul-'Aqabah.",
            "repeat_count": 1,
        },
        {
            "title_english": "The Best Supplication (Day of Arafah)",
            "title_arabic": "خير الدعاء يوم عرفة",
            "text_arabic": "لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ، وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            "transliteration": "La ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa Huwa 'ala kulli shay'in Qadir.",
            "translation": "None has the right to be worshipped except Allah alone, without partner. To Him belongs the dominion and to Him belongs all praise, and He is over all things competent.",
            "source": "Tirmidhi",
            "virtue": "The best supplication is that of the Day of Arafah, and the best that the Prophets and he said was this.",
            "repeat_count": 1,
        },
        {
            "title_english": "Between the Two Corners (Tawaf)",
            "title_arabic": "دعاء بين الركنين في الطواف",
            "text_arabic": "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            "transliteration": "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar.",
            "translation": "Our Lord, give us good in this world and good in the Hereafter, and protect us from the punishment of the Fire.",
            "source": "Abu Dawud",
            "virtue": "The Prophet ﷺ would recite this between the Yemeni corner and the Black Stone during Tawaf.",
            "repeat_count": 1,
        },
    ],
    "Salawat on the Prophet ﷺ": [
        {
            "title_english": "The Ibrahimi Salawat",
            "title_arabic": "الصلاة الإبراهيمية",
            "text_arabic": "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا صَلَّيْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ، اللَّهُمَّ بَارِكْ عَلَى مُحَمَّدٍ وَعَلَى آلِ مُحَمَّدٍ، كَمَا بَارَكْتَ عَلَى إِبْرَاهِيمَ وَعَلَى آلِ إِبْرَاهِيمَ، إِنَّكَ حَمِيدٌ مَجِيدٌ",
            "transliteration": "Allahumma salli 'ala Muhammadin wa 'ala ali Muhammad, kama sallayta 'ala Ibrahima wa 'ala ali Ibrahim, innaka Hamidun Majid. Allahumma barik 'ala Muhammadin wa 'ala ali Muhammad, kama barakta 'ala Ibrahima wa 'ala ali Ibrahim, innaka Hamidun Majid.",
            "translation": "O Allah, send prayers upon Muhammad and the family of Muhammad, as You sent prayers upon Ibrahim and the family of Ibrahim; You are indeed Praiseworthy, Glorious. O Allah, send blessings upon Muhammad and the family of Muhammad, as You blessed Ibrahim and the family of Ibrahim; You are indeed Praiseworthy, Glorious.",
            "source": "Bukhari & Muslim",
            "virtue": "The complete salawat the Prophet ﷺ taught when asked how to send prayers upon him.",
            "repeat_count": 1,
        },
        {
            "title_english": "Concise Salawat",
            "title_arabic": "الصلاة على النبي",
            "text_arabic": "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ",
            "transliteration": "Allahumma salli wa sallim 'ala nabiyyina Muhammad.",
            "translation": "O Allah, send prayers and peace upon our Prophet Muhammad.",
            "source": "Muslim",
            "virtue": "Whoever sends one prayer upon the Prophet ﷺ, Allah sends ten upon him.",
            "repeat_count": 10,
        },
    ],
    "Protection & Refuge": [
        {
            "title_english": "Refuge from Trials",
            "title_arabic": "أعوذ بالله من جهد البلاء",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ جَهْدِ الْبَلَاءِ، وَدَرَكِ الشَّقَاءِ، وَسُوءِ الْقَضَاءِ، وَشَمَاتَةِ الْأَعْدَاءِ",
            "transliteration": "Allahumma inni a'udhu bika min jahdil-bala', wa darakish-shaqa', wa su'il-qada', wa shamatatil-a'da'.",
            "translation": "O Allah, I seek refuge in You from the hardship of affliction, from being overtaken by wretchedness, from an evil decree, and from the gloating of enemies.",
            "source": "Bukhari & Muslim",
            "virtue": "The Prophet ﷺ would seek refuge from these four things.",
            "repeat_count": 1,
        },
        {
            "title_english": "Refuge from Evil Character & Deeds",
            "title_arabic": "اللهم إني أعوذ بك من منكرات الأخلاق",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنْ مُنْكَرَاتِ الْأَخْلَاقِ، وَالْأَعْمَالِ، وَالْأَهْوَاءِ",
            "transliteration": "Allahumma inni a'udhu bika min munkaratil-akhlaq, wal-a'mal, wal-ahwa'.",
            "translation": "O Allah, I seek refuge in You from evil character, evil deeds, and evil desires.",
            "source": "Tirmidhi",
            "virtue": "A supplication seeking protection from all forms of corruption of the self.",
            "repeat_count": 1,
        },
        {
            "title_english": "Wellbeing in Body, Hearing & Sight",
            "title_arabic": "اللهم عافني في بدني",
            "text_arabic": "اللَّهُمَّ عَافِنِي فِي بَدَنِي، اللَّهُمَّ عَافِنِي فِي سَمْعِي، اللَّهُمَّ عَافِنِي فِي بَصَرِي، لَا إِلَٰهَ إِلَّا أَنْتَ",
            "transliteration": "Allahumma 'afini fi badani, Allahumma 'afini fi sam'i, Allahumma 'afini fi basari, la ilaha illa anta.",
            "translation": "O Allah, grant me wellbeing in my body. O Allah, grant me wellbeing in my hearing. O Allah, grant me wellbeing in my sight. None has the right to be worshipped except You.",
            "source": "Abu Dawud",
            "virtue": "Said three times morning and evening, seeking health and protection.",
            "repeat_count": 3,
        },
        {
            "title_english": "Refuge from Disbelief and Poverty",
            "title_arabic": "أعوذ بك من الكفر والفقر",
            "text_arabic": "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْكُفْرِ وَالْفَقْرِ، وَأَعُوذُ بِكَ مِنْ عَذَابِ الْقَبْرِ، لَا إِلَٰهَ إِلَّا أَنْتَ",
            "transliteration": "Allahumma inni a'udhu bika minal-kufri wal-faqr, wa a'udhu bika min 'adhabil-qabr, la ilaha illa anta.",
            "translation": "O Allah, I seek refuge in You from disbelief and poverty, and I seek refuge in You from the punishment of the grave. None has the right to be worshipped except You.",
            "source": "Abu Dawud, An-Nasai",
            "virtue": "Said morning and evening for protection of one's faith and condition.",
            "repeat_count": 3,
        },
    ],
}


def main():
    duas = json.loads(DUAS_PATH.read_text(encoding="utf-8"))
    categories = json.loads(CATEGORIES_PATH.read_text(encoding="utf-8"))

    # Lookups -------------------------------------------------------------
    cat_by_name = {c["name_english"]: c for c in categories}
    next_dua_id = max(d["id"] for d in duas) + 1
    next_cat_id = max(c["id"] for c in categories) + 1
    next_cat_order = max(c["display_order"] for c in categories) + 1

    # Track the running display_order per category id.
    max_order_in_cat = {}
    for d in duas:
        cid = d["category_id"]
        max_order_in_cat[cid] = max(max_order_in_cat.get(cid, 0), d["display_order"])

    def append_dua(category_id, dua):
        nonlocal next_dua_id
        order = max_order_in_cat.get(category_id, 0) + 1
        max_order_in_cat[category_id] = order
        duas.append({
            "id": next_dua_id,
            "category_id": category_id,
            "title_english": dua["title_english"],
            "title_arabic": dua["title_arabic"],
            "text_arabic": dua["text_arabic"],
            "transliteration": dua["transliteration"],
            "translation": dua["translation"],
            "source": dua["source"],
            "virtue": dua.get("virtue"),
            "repeat_count": dua.get("repeat_count", 1),
            "audio_file": None,
            "display_order": order,
        })
        next_dua_id += 1

    # 1) Additions to existing categories --------------------------------
    for cat_name, items in ADDITIONS_TO_EXISTING.items():
        if cat_name not in cat_by_name:
            raise SystemExit(f"Unknown existing category: {cat_name!r}")
        cid = cat_by_name[cat_name]["id"]
        for item in items:
            append_dua(cid, item)

    # 2) New categories + their duas -------------------------------------
    for cat in NEW_CATEGORIES:
        category = {
            "id": next_cat_id,
            "name_english": cat["name_english"],
            "name_arabic": cat["name_arabic"],
            "icon": cat["icon"],
            "display_order": next_cat_order,
            "dua_count": 0,  # recomputed below
        }
        categories.append(category)
        cat_by_name[cat["name_english"]] = category
        next_cat_id += 1
        next_cat_order += 1

        for item in NEW_DUAS.get(cat["name_english"], []):
            append_dua(category["id"], item)

    # 3) Recompute dua_count for every category --------------------------
    counts = {}
    for d in duas:
        counts[d["category_id"]] = counts.get(d["category_id"], 0) + 1
    for c in categories:
        c["dua_count"] = counts.get(c["id"], 0)

    # Write back ----------------------------------------------------------
    DUAS_PATH.write_text(
        json.dumps(duas, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    CATEGORIES_PATH.write_text(
        json.dumps(categories, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )

    print(f"Categories: {len(categories)}")
    print(f"Duas: {len(duas)}")
    print("Per-category counts:")
    for c in categories:
        print(f"  {c['id']:>2} {c['name_english']:<28} {c['dua_count']}")


if __name__ == "__main__":
    main()
