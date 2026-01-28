package com.arshadshah.nimaz.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arshadshah.nimaz.data.local.database.NimazDatabase
import com.arshadshah.nimaz.data.local.database.dao.DuaDao
import com.arshadshah.nimaz.data.local.database.dao.FastingDao
import com.arshadshah.nimaz.data.local.database.dao.HadithDao
import com.arshadshah.nimaz.data.local.database.dao.IslamicEventDao
import com.arshadshah.nimaz.data.local.database.dao.LocationDao
import com.arshadshah.nimaz.data.local.database.dao.PrayerDao
import com.arshadshah.nimaz.data.local.database.dao.QuranDao
import com.arshadshah.nimaz.data.local.database.dao.TasbihDao
import com.arshadshah.nimaz.data.local.database.dao.ZakatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `surah_info` (`surahNumber` INTEGER NOT NULL, `description` TEXT NOT NULL, `themes` TEXT NOT NULL, PRIMARY KEY(`surahNumber`))")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (1, 'Al-Fatiha (The Opening) is the most recited surah, serving as the essence of the Quran. It includes praise of Allah, seeking guidance, and supplication for the straight path.', 'Praise,Guidance,Supplication,Mercy,Divine Attributes')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (2, 'Al-Baqarah (The Cow) is the longest surah, covering fundamental Islamic teachings including faith, law, and guidance. It addresses believers, disbelievers, and hypocrites, establishing rules for worship, family, and society.', 'Faith,Law,Guidance,Stories of Prophets,Social Justice')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (3, 'Al-Imran (The Family of Imran) discusses the stories of Mary and Jesus, the Battle of Uhud, and principles of faith. It emphasizes unity among believers and steadfastness in trials.', 'Prophets,Unity,Battle of Uhud,Patience,Divine Will')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (4, 'An-Nisa (The Women) focuses on women''s rights, family law, inheritance, and social justice. It establishes guidelines for marriage, orphans, and equitable treatment.', 'Women''s Rights,Family Law,Inheritance,Justice,Social Ethics')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (5, 'Al-Ma''idah (The Table Spread) covers dietary laws, legal rulings, and the covenant with Allah. It includes stories of Jesus'' disciples and the table spread from heaven.', 'Dietary Laws,Covenants,Legal Rulings,Jesus,Gratitude')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (6, 'Al-An''am (The Cattle) emphasizes monotheism and refutes idolatry through arguments about creation and divine signs. It discusses Allah''s sovereignty and the consequences of disbelief.', 'Monotheism,Creation,Divine Signs,Idolatry,Accountability')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (7, 'Al-A''raf (The Heights) narrates the stories of various prophets including Adam, Noah, Moses, and others. It warns against arrogance and emphasizes following divine guidance.', 'Prophets'' Stories,Adam,Moses,Guidance,Warning')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (8, 'Al-Anfal (The Spoils of War) addresses the Battle of Badr, rules of warfare, and distribution of war spoils. It emphasizes reliance on Allah and unity among believers.', 'Battle of Badr,Warfare Ethics,Unity,Divine Help,Victory')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (9, 'At-Tawbah (The Repentance) discusses treaties, warfare against aggressors, and hypocrisy. It calls for sincere repentance and striving in Allah''s cause.', 'Repentance,Warfare,Hypocrisy,Treaties,Striving')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (10, 'Yunus (Jonah) focuses on the story of Prophet Jonah and his people who repented. It emphasizes Allah''s mercy, the reality of revelation, and consequences of denial.', 'Prophet Jonah,Repentance,Mercy,Revelation,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (11, 'Hud tells the stories of Noah, Hud, Saleh, Abraham, Lot, and Shu''ayb. It warns against injustice and urges patience in conveying the message.', 'Prophets,Noah,Abraham,Patience,Divine Justice')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (12, 'Yusuf (Joseph) narrates the complete story of Prophet Joseph from his childhood dreams to becoming a ruler in Egypt. It demonstrates patience, trust in Allah, and eventual triumph.', 'Prophet Joseph,Patience,Dreams,Trials,Divine Plan')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (13, 'Ar-Ra''d (The Thunder) describes Allah''s power through natural phenomena and emphasizes the truth of revelation. It contrasts believers with disbelievers regarding their ultimate fate.', 'Divine Power,Thunder,Revelation,Nature,Afterlife')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (14, 'Ibrahim (Abraham) recounts Prophet Abraham''s supplication and message of monotheism. It warns against ingratitude and highlights Allah''s blessings.', 'Prophet Abraham,Monotheism,Gratitude,Blessings,Supplication')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (15, 'Al-Hijr discusses the destruction of past nations, creation of humanity, and protection of the Quran. It emphasizes Allah''s knowledge and power over all things.', 'Past Nations,Creation,Quran Preservation,Divine Knowledge,Warning')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (16, 'An-Nahl (The Bee) highlights Allah''s countless blessings in nature and creation. It encourages gratitude, patience, and following the straight path.', 'Blessings,Nature,Gratitude,Patience,Divine Favors')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (17, 'Al-Isra (The Night Journey) describes Prophet Muhammad''s miraculous journey and contains moral and ethical teachings. It emphasizes worship of Allah alone and respect for parents.', 'Night Journey,Ethics,Worship,Parents,Moral Guidance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (18, 'Al-Kahf (The Cave) contains four stories: the People of the Cave, the two gardens, Moses and Khidr, and Dhul-Qarnayn. It teaches lessons about faith, patience, and knowledge.', 'Cave Dwellers,Moses and Khidr,Patience,Knowledge,Trials')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (19, 'Maryam (Mary) narrates the stories of Mary, Jesus, Zechariah, Abraham, and other prophets. It emphasizes the mercy of Allah and the truth of prophethood.', 'Mary,Jesus,Prophets,Mercy,Monotheism')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (20, 'Ta-Ha recounts Moses'' story from his calling at the burning bush to leading the Israelites. It also mentions Adam''s story and emphasizes remembrance of Allah.', 'Prophet Moses,Pharaoh,Remembrance,Patience,Divine Mission')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (21, 'Al-Anbiya (The Prophets) mentions numerous prophets and their struggles against disbelief. It emphasizes the unity of the prophetic message and accountability on the Day of Judgment.', 'Prophets,Unity of Message,Judgment Day,Patience,Divine Support')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (22, 'Al-Hajj (The Pilgrimage) discusses the pilgrimage rituals, resurrection, and struggle in Allah''s cause. It describes signs of Allah''s power and the fate of believers versus disbelievers.', 'Pilgrimage,Resurrection,Struggle,Divine Power,Rituals')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (23, 'Al-Mu''minun (The Believers) describes the characteristics of successful believers and narrates stories of past prophets. It emphasizes prayer, humility, and moral excellence.', 'Believers'' Traits,Prophets,Prayer,Humility,Success')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (24, 'An-Nur (The Light) establishes laws regarding modesty, slander, and proper conduct. It includes the verse of Light and emphasizes moral purity in society.', 'Modesty,Social Ethics,Slander,Light,Purity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (25, 'Al-Furqan (The Criterion) describes the Quran as a criterion between truth and falsehood. It refutes disbelievers'' arguments and highlights the mercy of Allah.', 'Quran,Truth,Mercy,Servants of the Most Merciful,Guidance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (26, 'Ash-Shu''ara (The Poets) narrates stories of Moses, Abraham, Noah, Hud, Saleh, Lot, and Shu''ayb confronting their people. It emphasizes the consistent message of all prophets.', 'Prophets,Moses,Abraham,Rejection,Divine Message')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (27, 'An-Naml (The Ant) tells stories of Solomon, the Queen of Sheba, and other prophets. It highlights Allah''s signs in creation and the wisdom He grants to His servants.', 'Prophet Solomon,Queen of Sheba,Wisdom,Divine Signs,Gratitude')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (28, 'Al-Qasas (The Stories) provides detailed accounts of Moses'' life from infancy through prophethood. It discusses divine wisdom in apparent difficulties and ultimate triumph of truth.', 'Moses,Pharaoh,Divine Wisdom,Trials,Truth')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (29, 'Al-Ankabut (The Spider) uses the spider''s weak web as a metaphor for false reliance on anything besides Allah. It emphasizes trials as a test of faith and mentions various prophets.', 'Trials,Faith,Prophets,False Gods,Patience')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (30, 'Ar-Rum (The Romans) prophesies the Romans'' victory and discusses signs of Allah in creation. It emphasizes resurrection and the natural disposition toward monotheism.', 'Romans,Divine Signs,Resurrection,Creation,Natural Faith')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (31, 'Luqman recounts the wise advice of Luqman to his son, including monotheism, gratitude, and good conduct. It emphasizes wisdom and moral guidance.', 'Wisdom,Parental Advice,Gratitude,Monotheism,Moral Conduct')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (32, 'As-Sajdah (The Prostration) discusses creation, revelation, and resurrection. It emphasizes the reality of the hereafter and prostration to Allah alone.', 'Creation,Prostration,Resurrection,Revelation,Hereafter')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (33, 'Al-Ahzab (The Combined Forces) addresses the Battle of the Trench, rules for the Prophet''s household, and social conduct. It establishes important Islamic laws and etiquette.', 'Battle of the Trench,Prophet''s Wives,Social Conduct,Hijab,Ethics')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (34, 'Saba (Sheba) discusses the kingdom of Sheba, David, Solomon, and Allah''s signs in creation. It warns against arrogance and emphasizes gratitude for blessings.', 'Sheba,David,Solomon,Gratitude,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (35, 'Fatir (The Originator) emphasizes Allah as the creator and sustainer of all. It contrasts believers with disbelievers and highlights Satan as an enemy.', 'Creation,Divine Attributes,Satan,Believers vs Disbelievers,Sustenance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (36, 'Ya-Sin discusses resurrection, divine signs, and the story of a town that rejected messengers. It emphasizes the Quran''s role as a warning and reminder.', 'Resurrection,Divine Signs,Messengers,Warning,Hereafter')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (37, 'As-Saffat (Those Ranged in Ranks) describes angels praising Allah and mentions stories of Noah, Abraham, Moses, and Jonah. It emphasizes monotheism and the reality of judgment.', 'Angels,Prophets,Abraham,Monotheism,Judgment')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (38, 'Sad narrates stories of David, Solomon, and Job, highlighting patience and repentance. It discusses Satan''s arrogance and the creation of Adam.', 'David,Solomon,Job,Patience,Satan')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (39, 'Az-Zumar (The Groups) emphasizes sincere worship of Allah alone and describes the Day of Judgment when people will be grouped. It highlights repentance and Allah''s mercy.', 'Sincere Worship,Repentance,Judgment Day,Mercy,Monotheism')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (40, 'Ghafir (The Forgiver) describes Allah''s forgiveness and power, and the story of Moses versus Pharaoh. It mentions the believer from Pharaoh''s family who defended Moses.', 'Forgiveness,Moses,Pharaoh,Divine Power,Faith')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (41, 'Fussilat (Explained in Detail) discusses the Quran''s clarity, creation of heavens and earth, and warnings to past nations. It emphasizes Allah''s signs and the response of the righteous.', 'Quran,Creation,Divine Signs,Warning,Righteous')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (42, 'Ash-Shura (The Consultation) emphasizes consultation in affairs, unity of prophetic message, and Allah''s attributes. It discusses revelation and the balance between hope and fear.', 'Consultation,Unity,Revelation,Divine Attributes,Balance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (43, 'Az-Zukhruf (The Gold Adornments) warns against being deceived by worldly luxuries and refutes false beliefs about Allah. It mentions Abraham''s rejection of idolatry and Jesus'' true nature.', 'Worldly Luxuries,Abraham,Jesus,Idolatry,Truth')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (44, 'Ad-Dukhan (The Smoke) describes the smoke as a sign of the Day of Judgment and mentions Pharaoh''s destruction. It emphasizes the Quran as a blessed revelation.', 'Smoke,Judgment Day,Pharaoh,Quran,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (45, 'Al-Jathiyah (The Kneeling) describes how all nations will kneel on Judgment Day and emphasizes Allah''s signs in creation. It warns against following desires and denying resurrection.', 'Judgment Day,Creation,Divine Signs,Desires,Resurrection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (46, 'Al-Ahqaf (The Wind-Curved Sand Dunes) narrates the destruction of the people of Ad and emphasizes belief in resurrection. It mentions the jinn who heard the Quran and believed.', 'People of Ad,Jinn,Resurrection,Quran,Divine Punishment')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (47, 'Muhammad discusses rules of warfare, hypocrisy, and the characteristics of believers versus disbelievers. It emphasizes striving in Allah''s cause and sincerity.', 'Warfare,Hypocrisy,Believers,Striving,Sincerity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (48, 'Al-Fath (The Victory) celebrates the Treaty of Hudaybiyyah as a clear victory and prophesies the conquest of Makkah. It praises the companions'' loyalty and faith.', 'Treaty of Hudaybiyyah,Victory,Companions,Loyalty,Conquest')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (49, 'Al-Hujurat (The Rooms) establishes etiquette in dealing with the Prophet and fellow Muslims. It emphasizes unity, avoiding suspicion, and equality of all people.', 'Etiquette,Unity,Equality,Social Conduct,Brotherhood')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (50, 'Qaf discusses resurrection, creation, and the Day of Judgment using the letter Qaf. It describes the recording angels and emphasizes reflection on divine signs.', 'Resurrection,Creation,Judgment Day,Angels,Reflection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (51, 'Adh-Dhariyat (The Scattering Winds) swears by various natural phenomena and emphasizes the reality of resurrection. It mentions stories of Abraham and other prophets.', 'Natural Phenomena,Resurrection,Abraham,Divine Promise,Creation')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (52, 'At-Tur (The Mount) swears by Mount Sinai and describes the inevitable punishment for disbelievers. It details Paradise and encourages patience in worship.', 'Mount Sinai,Judgment,Paradise,Patience,Worship')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (53, 'An-Najm (The Star) describes the Prophet''s vision, refutes idol worship, and emphasizes that no soul bears another''s burden. It discusses divine knowledge and power.', 'Prophet''s Vision,Idolatry,Responsibility,Divine Knowledge,Mi''raj')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (54, 'Al-Qamar (The Moon) describes the splitting of the moon as a sign and narrates the destruction of past nations. It emphasizes the Quran as an easy reminder.', 'Moon Splitting,Past Nations,Warning,Quran,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (55, 'Ar-Rahman (The Most Merciful) repeatedly asks which of Allah''s favors we would deny while describing His blessings. It describes Paradise and Hell in vivid detail.', 'Mercy,Blessings,Paradise,Hell,Gratitude')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (56, 'Al-Waqi''ah (The Inevitable Event) describes three groups on Judgment Day and their respective fates. It emphasizes the Quran''s honor and the reality of resurrection.', 'Judgment Day,Three Groups,Paradise,Hell,Resurrection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (57, 'Al-Hadid (The Iron) emphasizes Allah''s power, the fleeting nature of worldly life, and the importance of charity. It mentions iron and encourages striving for the hereafter.', 'Divine Power,Worldly Life,Charity,Iron,Striving')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (58, 'Al-Mujadilah (The Pleading Woman) addresses the case of a woman pleading about her husband''s unjust oath. It discusses secret consultations and establishes rulings on oaths and proper conduct.', 'Women''s Rights,Oaths,Secret Consultations,Justice,Social Conduct')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (59, 'Al-Hashr (The Exile) discusses the exile of the Jewish tribe Banu Nadir and rules of war spoils. It emphasizes Allah''s beautiful names and the Quran''s impact.', 'Exile,War Spoils,Divine Names,Quran,Unity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (60, 'Al-Mumtahanah (The Examined One) establishes rules for dealing with disbelievers and accepting women who migrate. It emphasizes testing the faith of female emigrants.', 'Loyalty,Migration,Women,Testing Faith,Treaties')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (61, 'As-Saff (The Ranks) emphasizes unity and standing in firm ranks for Allah''s cause. It mentions Jesus'' prophecy of Prophet Muhammad and calls to sincere striving.', 'Unity,Striving,Jesus,Prophecy,Sincerity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (62, 'Al-Jumu''ah (The Friday) emphasizes the importance of Friday prayer and warns against being distracted by worldly affairs. It describes the purpose of the Prophet''s mission.', 'Friday Prayer,Worship,Worldly Distractions,Prophet''s Mission,Remembrance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (63, 'Al-Munafiqun (The Hypocrites) exposes the characteristics and dangers of hypocrites. It warns believers against being deceived by appearances and encourages spending in charity.', 'Hypocrisy,Warning,Charity,Deception,Sincerity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (64, 'At-Taghabun (The Mutual Loss and Gain) describes Judgment Day as a day of mutual loss and gain. It emphasizes trials in family and wealth, and encourages forgiveness and charity.', 'Judgment Day,Trials,Family,Charity,Forgiveness')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (65, 'At-Talaq (The Divorce) establishes detailed rules for divorce, waiting periods, and maintenance. It emphasizes taqwa (God-consciousness) and trust in Allah.', 'Divorce,Waiting Period,Maintenance,Taqwa,Divine Provision')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (66, 'At-Tahrim (The Prohibition) addresses an incident in the Prophet''s household and calls for repentance. It provides examples of disbelieving and believing women.', 'Repentance,Prophet''s Wives,Examples,Faith,Sincerity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (67, 'Al-Mulk (The Sovereignty) emphasizes Allah''s dominion over all and describes creation''s perfection. It warns of Hell''s punishment and encourages reflection on divine signs.', 'Divine Sovereignty,Creation,Hell,Reflection,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (68, 'Al-Qalam (The Pen) defends the Prophet against accusations and narrates the story of the owners of the garden. It emphasizes patience and moral excellence.', 'Prophet''s Character,Patience,Garden Owners,Pen,Moral Excellence')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (69, 'Al-Haqqah (The Inevitable Reality) describes the Day of Judgment and the destruction of past nations. It emphasizes the Quran''s truth and the Prophet''s honesty.', 'Judgment Day,Past Nations,Quran''s Truth,Accountability,Certainty')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (70, 'Al-Ma''arij (The Ascending Stairways) describes the Day of Judgment and characteristics of successful believers. It emphasizes prayer, patience, and moral conduct.', 'Judgment Day,Prayer,Patience,Moral Conduct,Believers'' Traits')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (71, 'Nuh (Noah) narrates Prophet Noah''s persistent call to his people over 950 years. It emphasizes patience in da''wah and the consequences of persistent rejection.', 'Prophet Noah,Patience,Da''wah,Rejection,Divine Punishment')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (72, 'Al-Jinn describes how a group of jinn listened to the Quran and believed. It discusses the jinn''s testimony and their diversity in belief.', 'Jinn,Quran,Testimony,Belief,Divine Message')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (73, 'Al-Muzzammil (The Enshrouded One) commands the Prophet to pray at night and recite the Quran. It later eases the command and emphasizes charity and good deeds.', 'Night Prayer,Quran Recitation,Worship,Charity,Ease')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (74, 'Al-Muddaththir (The Cloaked One) commands the Prophet to arise and warn, and describes Hell and its guardians. It emphasizes human stubbornness and divine power.', 'Warning,Hell,Divine Power,Stubbornness,Resurrection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (75, 'Al-Qiyamah (The Resurrection) emphasizes the certainty of resurrection and describes the Day of Judgment. It warns against being distracted by worldly life.', 'Resurrection,Judgment Day,Certainty,Accountability,Human Nature')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (76, 'Al-Insan (The Human) describes the creation of humans and rewards of Paradise for the righteous. It emphasizes patience, worship, and fulfilling vows.', 'Human Creation,Paradise,Patience,Worship,Righteousness')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (77, 'Al-Mursalat (The Emissaries) swears by various phenomena and repeatedly warns of Judgment Day. It describes the fate of deniers and the rewards of the righteous.', 'Judgment Day,Warning,Righteous,Deniers,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (78, 'An-Naba (The Great News) describes resurrection and Judgment Day in detail. It emphasizes divine signs in creation and the reality of accountability.', 'Resurrection,Judgment Day,Creation,Accountability,Divine Signs')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (79, 'An-Nazi''at (Those Who Pull Out) swears by angels and describes the Day of Judgment. It mentions Moses'' story with Pharaoh and emphasizes resurrection.', 'Angels,Judgment Day,Moses,Pharaoh,Resurrection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (80, 'Abasa (He Frowned) recounts when the Prophet frowned at a blind man and teaches prioritizing the sincere seeker. It describes human ingratitude and resurrection.', 'Prophet''s Lesson,Equality,Human Ingratitude,Resurrection,Priorities')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (81, 'At-Takwir (The Overthrowing) describes cosmic events of Judgment Day and affirms the Quran''s revelation through Gabriel. It emphasizes the inevitability of accountability.', 'Judgment Day,Cosmic Events,Gabriel,Quran,Accountability')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (82, 'Al-Infitar (The Cleaving) describes the splitting of the sky on Judgment Day and the recording angels. It warns of accountability and divine justice.', 'Judgment Day,Recording Angels,Accountability,Divine Justice,Cosmic Events')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (83, 'Al-Mutaffifin (The Defrauders) condemns cheating in weights and measures and describes the records of the wicked and righteous. It warns against mocking believers.', 'Honesty in Trade,Accountability,Records,Mockery,Justice')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (84, 'Al-Inshiqaq (The Splitting Open) describes the splitting of the heavens on Judgment Day. It emphasizes that humans are striving toward their Lord and will meet Him.', 'Judgment Day,Striving,Meeting Allah,Accountability,Cosmic Events')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (85, 'Al-Buruj (The Constellations) narrates the story of believers burned in ditches and emphasizes Allah''s protection of the Quran. It warns oppressors of divine punishment.', 'Believers'' Trials,Divine Protection,Quran Preservation,Oppression,Punishment')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (86, 'At-Tariq (The Night Comer) swears by the night star and describes human creation. It emphasizes that every soul has a protector and the Quran''s decisive nature.', 'Night Star,Human Creation,Protection,Quran,Divine Power')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (87, 'Al-A''la (The Most High) glorifies Allah and emphasizes ease in religion. It mentions Abraham and Moses, and stresses preference for the eternal life over the worldly.', 'Divine Glorification,Ease in Religion,Prophets,Hereafter,Remembrance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (88, 'Al-Ghashiyah (The Overwhelming Event) describes the contrasting conditions of people on Judgment Day. It encourages reflection on divine signs in creation.', 'Judgment Day,Paradise,Hell,Divine Signs,Reflection')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (89, 'Al-Fajr (The Dawn) swears by dawn and describes the destruction of past nations. It discusses human nature regarding wealth and emphasizes accountability.', 'Dawn,Past Nations,Wealth,Accountability,Human Nature')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (90, 'Al-Balad (The City) swears by Makkah and describes the difficult path to righteousness. It emphasizes freeing slaves, feeding the poor, and choosing the right path.', 'Makkah,Righteousness,Charity,Difficult Path,Social Justice')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (91, 'Ash-Shams (The Sun) swears by celestial and earthly phenomena and discusses the soul''s potential for good and evil. It mentions the people of Thamud and their destruction.', 'Sun,Soul,Good and Evil,Thamud,Self-Purification')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (92, 'Al-Layl (The Night) contrasts the night and day, and describes different paths for the generous and the miserly. It emphasizes accountability and divine guidance.', 'Night,Generosity,Miserliness,Accountability,Contrasts')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (93, 'Ad-Duha (The Morning Brightness) consoles the Prophet, reminding him of Allah''s past favors. It commands kindness to orphans and beggars, and proclaiming Allah''s blessings.', 'Consolation,Divine Favors,Orphans,Gratitude,Kindness')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (94, 'Ash-Sharh (The Relief) reassures the Prophet that with hardship comes ease. It mentions the opening of his chest and elevation of his mention.', 'Relief,Hardship and Ease,Prophet''s Status,Comfort,Hope')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (95, 'At-Tin (The Fig) swears by sacred places and discusses human creation in the best form. It emphasizes the consequences of straying from righteousness.', 'Human Creation,Sacred Places,Righteousness,Deeds,Best Form')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (96, 'Al-Alaq (The Clot) contains the first revelation to the Prophet, commanding him to read. It condemns arrogance and emphasizes seeking knowledge and prostration.', 'First Revelation,Knowledge,Reading,Arrogance,Prostration')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (97, 'Al-Qadr (The Power) describes the Night of Decree as better than a thousand months. It emphasizes peace and the descent of angels during this blessed night.', 'Laylat al-Qadr,Angels,Blessings,Peace,Worship')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (98, 'Al-Bayyinah (The Clear Evidence) describes the Prophet as clear evidence and discusses the People of the Book. It emphasizes sincere worship and describes the fate of believers and disbelievers.', 'Clear Evidence,People of the Book,Sincere Worship,Fate,Truth')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (99, 'Az-Zalzalah (The Earthquake) describes the earth''s violent shaking on Judgment Day. It emphasizes that even the smallest deeds will be accounted for.', 'Earthquake,Judgment Day,Accountability,Small Deeds,Records')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (100, 'Al-Adiyat (The Charging Horses) swears by war horses and describes human ingratitude. It warns of resurrection and exposure of secrets.', 'War Horses,Ingratitude,Resurrection,Accountability,Human Nature')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (101, 'Al-Qari''ah (The Striking Calamity) describes Judgment Day as a striking calamity when deeds are weighed. It contrasts the fate of those with heavy versus light scales.', 'Judgment Day,Deeds Weighed,Calamity,Fate,Balance')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (102, 'At-Takathur (The Rivalry in Worldly Increase) warns against being distracted by competition for worldly gains. It emphasizes the certainty of death and accountability.', 'Worldly Competition,Distraction,Death,Accountability,Hell')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (103, 'Al-Asr (The Time) states that humans are in loss except those who believe, do good deeds, and counsel each other in truth and patience. It emphasizes time''s value.', 'Time,Loss,Faith,Good Deeds,Mutual Counsel')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (104, 'Al-Humazah (The Slanderer) condemns those who slander, backbite, and hoard wealth. It describes the Crushing Fire as punishment for such behavior.', 'Slander,Backbiting,Wealth,Punishment,Hell')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (105, 'Al-Fil (The Elephant) narrates how Allah protected the Ka''bah from Abraha''s army with the elephant. It demonstrates divine power and protection of the sacred house.', 'Elephant Army,Divine Protection,Ka''bah,Miracle,Power')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (106, 'Quraysh mentions the Quraysh tribe''s trading journeys and calls them to worship Allah who provided them security and sustenance. It emphasizes gratitude for blessings.', 'Quraysh,Trade,Security,Sustenance,Gratitude')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (107, 'Al-Ma''un (The Small Kindnesses) condemns those who deny the Judgment, mistreat orphans, and neglect prayer while being ostentatious. It emphasizes practical faith and helping others.', 'Judgment Denial,Orphans,Prayer,Hypocrisy,Small Kindnesses')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (108, 'Al-Kawthar (The Abundance) promises the Prophet abundant good and commands prayer and sacrifice. It states that his enemies will be cut off from posterity.', 'Abundance,Prayer,Sacrifice,Prophet''s Enemies,Divine Gift')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (109, 'Al-Kafirun (The Disbelievers) establishes clear distinction between the worship of believers and disbelievers. It emphasizes religious freedom and mutual respect while maintaining distinct beliefs.', 'Disbelievers,Worship,Distinction,Religious Freedom,Clarity')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (110, 'An-Nasr (The Divine Support) announces the coming victory and people entering Islam in multitudes. It commands glorification of Allah and seeking forgiveness.', 'Victory,Islam''s Spread,Glorification,Forgiveness,Prophet''s Mission')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (111, 'Al-Masad (The Palm Fiber) condemns Abu Lahab and his wife for their hostility to Islam. It declares their punishment in the Fire with a rope of palm fiber around her neck.', 'Abu Lahab,Punishment,Enemies of Islam,Fire,Divine Justice')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (112, 'Al-Ikhlas (The Sincerity) defines pure monotheism, declaring Allah as One, Eternal, not begetting nor begotten. It is considered equivalent to one-third of the Quran in meaning.', 'Monotheism,Tawhid,Divine Attributes,Oneness,Purity of Faith')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (113, 'Al-Falaq (The Daybreak) is a prayer seeking refuge in Allah from the evil of creation, darkness, sorcery, and envy. It emphasizes divine protection.', 'Seeking Refuge,Protection,Evil,Sorcery,Envy')")
            db.execSQL("INSERT INTO surah_info (surahNumber, description, themes) VALUES (114, 'An-Nas (Mankind) is a prayer seeking refuge in Allah from the whisperings of Satan and evil suggestions. It emphasizes Allah as Lord, King, and God of mankind.', 'Seeking Refuge,Satan,Whisperings,Divine Lordship,Protection')")
        }
    }

    @Provides
    @Singleton
    fun provideNimazDatabase(
        @ApplicationContext context: Context
    ): NimazDatabase {
        return Room.databaseBuilder(
            context,
            NimazDatabase::class.java,
            NimazDatabase.DATABASE_NAME
        )
            .createFromAsset("database/nimaz_prepopulated.db")
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    @Singleton
    fun provideQuranDao(database: NimazDatabase): QuranDao = database.quranDao()

    @Provides
    @Singleton
    fun provideHadithDao(database: NimazDatabase): HadithDao = database.hadithDao()

    @Provides
    @Singleton
    fun provideDuaDao(database: NimazDatabase): DuaDao = database.duaDao()

    @Provides
    @Singleton
    fun providePrayerDao(database: NimazDatabase): PrayerDao = database.prayerDao()

    @Provides
    @Singleton
    fun provideFastingDao(database: NimazDatabase): FastingDao = database.fastingDao()

    @Provides
    @Singleton
    fun provideTasbihDao(database: NimazDatabase): TasbihDao = database.tasbihDao()

    @Provides
    @Singleton
    fun provideLocationDao(database: NimazDatabase): LocationDao = database.locationDao()

    @Provides
    @Singleton
    fun provideIslamicEventDao(database: NimazDatabase): IslamicEventDao = database.islamicEventDao()

    @Provides
    @Singleton
    fun provideZakatDao(database: NimazDatabase): ZakatDao = database.zakatDao()
}
