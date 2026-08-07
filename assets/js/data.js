/* ============================================================================
   SEERAH — Interactive Chronology of the Life of the Prophet Muhammad ﷺ
   DATA LAYER
   ----------------------------------------------------------------------------
   Every fact below carries a citation and a certainty tag, in keeping with the
   platform's founding principle: no claim ships without a named, gradeable
   source, and where scholars differ, the product says so rather than choosing
   a side silently.

   Certainty tags:
     'confirmed' — well-attested; multiple primary sources agree
     'differ'    — scholars differ on details (usually of dating or number)

   Source tiers:
     'a' — classical primary source / authenticated hadith
     'b' — respected modern scholarly synthesis

   Categories:
     revelation · battle · migration · treaty · life

   NOTE ON SCOPE: This is a design/education artifact, not a fatwa. All content
   assumes review by qualified scholars before publication. No depiction of any
   prophet or companion appears anywhere in the visual language.
   ========================================================================== */

/* ---------------------------------------------------------------------------
   ERAS — the three top-level periods of the chronology
   --------------------------------------------------------------------------- */
const ERAS = [
  { n: 'Before Prophethood', a: 565, b: 610, c: 'e0',
    blurb: 'The world into which he ﷺ was born — pre-Islamic Arabia, the custodianship of the Kaaba, and forty years of a life lived before any claim to revelation.' },
  { n: 'The Meccan Period', a: 610, b: 622, c: 'e1',
    blurb: 'Thirteen years from the first revelation in the Cave of Hira to the eve of the Hijrah — a period of patience, persecution, and the search for refuge.' },
  { n: 'The Medinan Period', a: 622, b: 634, c: 'e2',
    blurb: 'From a persecuted minority to a community with territory, a treaty framework, and the capacity to govern. The Islamic calendar is dated from this migration.' }
];

/* ---------------------------------------------------------------------------
   CATEGORIES — label + accent colour (colour is design-system, not devotional)
   --------------------------------------------------------------------------- */
const CAT = {
  revelation: 'Revelation',
  battle:     'Battle',
  migration:  'Migration',
  treaty:     'Treaty',
  life:       'Life Event'
};
const CAT_COLOR = {
  revelation: '#C8A44B',
  battle:     '#A6543C',
  migration:  '#4A6B4A',
  treaty:     '#608CAF',
  life:       '#7C8794'
};

/* ---------------------------------------------------------------------------
   EVENTS — the connected chronology, in strict chronological order.
   Chronological neighbours (before / after) are computed from array order in
   app.js, so the sequence here IS the timeline.
   --------------------------------------------------------------------------- */
const EVENTS = [

 {id:'elephant',y:570,hij:'—',cat:'life',t:'Year of the Elephant',ar:'عام الفيل',
  loc:'Makkah',cert:'differ',key:false,themes:['makkah','protection'],
  sum:'Abraha, the Abyssinian governor of Yemen, marched on Makkah with an army that included war elephants, intending to destroy the Kaaba. The campaign was destroyed before it reached the sanctuary. The Prophet ﷺ was born in this same year, which Arabs afterwards used as a calendar landmark.',
  why:'Abraha had built a grand cathedral in Sana\'a and sought to divert Arabian pilgrimage away from Makkah. Its failure dramatically raised the prestige of the Quraysh as custodians of the sanctuary.',
  verses:[{s:'Al-Fil',n:'105:1-5',ar:'أَلَمْ تَرَ كَيْفَ فَعَلَ رَبُّكَ بِأَصْحَابِ الْفِيلِ ۝ أَلَمْ يَجْعَلْ كَيْدَهُمْ فِي تَضْلِيلٍ',tr:'Have you not considered how your Lord dealt with the companions of the elephant? Did He not make their plan into misguidance?',note:'An entire surah is named for this event, addressed to a Quraysh generation that still remembered it.'}],
  people:[{n:'Abd al-Muttalib',ar:'عبد المطلب',r:'Grandfather of the Prophet ﷺ and custodian of the Kaaba; negotiated with Abraha.'}],
  lessons:[{t:'Divine Protection',x:'The sanctuary was defended without human armies — a lesson the Quraysh themselves narrated for generations.'},{t:'Historical Anchor',x:'Arabs dated events from this year, giving the Seerah a fixed chronological starting point.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Sana\'a',x:60,y:88}],routes:[[[60,88],[52,58]]],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a'],['Ar-Raheeq Al-Makhtum','Modern scholarly synthesis','b']]},

 {id:'birth',y:570,hij:'—',cat:'life',t:'Birth of the Prophet ﷺ',ar:'مولد النبي ﷺ',
  loc:'Makkah',cert:'confirmed',key:true,themes:['makkah','family'],
  sum:'Muhammad ibn Abdullah ﷺ was born in Makkah into the clan of Banu Hashim of the Quraysh. His father Abdullah had died before his birth. He was nursed in the desert by Halimah as-Sa\'diyyah, and lost his mother Aminah at age six and his grandfather at eight, after which his uncle Abu Talib raised him.',
  why:'Being orphaned early placed him outside the ordinary lines of inherited power, yet his lineage within Quraysh gave him standing — a combination that shaped how his later message was received.',
  verses:[{s:'Ad-Duha',n:'93:6',ar:'أَلَمْ يَجِدْكَ يَتِيمًا فَآوَىٰ',tr:'Did He not find you an orphan and give you refuge?',note:'Revealed much later, recalling this period of his life directly.'}],
  people:[{n:'Aminah bint Wahb',ar:'آمنة بنت وهب',r:'His mother; died when he was six years old.'},{n:'Abu Talib',ar:'أبو طالب',r:'Uncle and protector who shielded him through the hardest Meccan years.'},{n:'Halimah as-Sa\'diyyah',ar:'حليمة السعدية',r:'His wet-nurse among the Banu Sa\'d in the desert.'}],
  lessons:[{t:'Character Before Message',x:'Forty years of known honesty preceded any claim to prophethood — his own people called him al-Amin, the trustworthy.'},{t:'Formative Hardship',x:'Early loss and modest means shaped a leader unusually attentive to orphans, the poor, and the powerless.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Banu Sa\'d',x:44,y:48}],routes:[[[52,58],[44,48]]],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a'],['Ibn Kathir, Al-Sira al-Nabawiyya','Classical reference','a']]},

 {id:'khadijah',y:595,hij:'—',cat:'life',t:'Marriage to Khadijah',ar:'الزواج من خديجة',
  loc:'Makkah',cert:'confirmed',key:false,themes:['makkah','family'],
  sum:'At about twenty-five he married Khadijah bint Khuwaylid, a respected merchant of Makkah some years his senior who had employed him to lead her trade caravan to Syria and was impressed by his integrity. Their marriage lasted twenty-five years, until her death, and she bore all his children except Ibrahim.',
  why:'Khadijah\'s wealth and social standing gave him security and independence; her unwavering belief in him at the first revelation was decisive at the most vulnerable moment of his mission.',
  verses:[],
  people:[{n:'Khadijah bint Khuwaylid',ar:'خديجة بنت خويلد',r:'First wife and first person to accept Islam; supported the mission with her wealth and conviction.'},{n:'Waraqah ibn Nawfal',ar:'ورقة بن نوفل',r:'Khadijah\'s cousin, a Christian scholar who later confirmed the first revelation.'}],
  lessons:[{t:'Partnership',x:'The first believer was his wife — the mission began inside a household before it reached a city.'},{t:'Reputation as Capital',x:'He was entrusted with the caravan precisely because of a reputation built over years, not a claim made in a moment.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Syria (Sham)',x:40,y:18}],routes:[[[52,58],[40,18]]],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'revelation',y:610,hij:'—',cat:'revelation',t:'The First Revelation',ar:'بدء الوحي',
  loc:'Cave of Hira, Makkah',cert:'confirmed',key:true,themes:['makkah','revelation'],
  sum:'During a retreat in the Cave of Hira on Mount An-Nur, the angel Jibril appeared to the Prophet ﷺ and commanded him to read. The first five verses of Surah Al-Alaq were revealed. He returned home shaken; Khadijah comforted him and took him to Waraqah ibn Nawfal, who recognised the encounter as the same revelation given to Musa.',
  why:'He had withdrawn to Hira for years of solitary reflection amid a society of idolatry, tribal vengeance, and buried daughters. The first command — read, in the name of your Lord — set knowledge at the foundation of everything that followed.',
  verses:[{s:'Al-Alaq',n:'96:1-5',ar:'اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ ۝ خَلَقَ الْإِنسَانَ مِنْ عَلَقٍ ۝ اقْرَأْ وَرَبُّكَ الْأَكْرَمُ ۝ الَّذِي عَلَّمَ بِالْقَلَمِ ۝ عَلَّمَ الْإِنسَانَ مَا لَمْ يَعْلَمْ',tr:'Read in the name of your Lord who created — created man from a clinging substance. Read, and your Lord is the most Generous — who taught by the pen, taught man that which he knew not.',note:'The first verses revealed. Scholars note the significance of the very first command being to read, and of the pen being named as the instrument of divine teaching.'}],
  people:[{n:'Khadijah bint Khuwaylid',ar:'خديجة بنت خويلد',r:'Comforted him and became the first to believe.'},{n:'Waraqah ibn Nawfal',ar:'ورقة بن نوفل',r:'Confirmed the revelation and warned he would be driven out by his own people.'}],
  lessons:[{t:'Knowledge First',x:'The opening word of revelation is a command to read — placing learning at the centre of the faith from its first moment.'},{t:'Support in Crisis',x:'His response to overwhelming experience was to seek his wife\'s counsel; her steadiness carried him through it.'}],
  places:[{n:'Cave of Hira',x:55,y:52},{n:'Makkah',x:52,y:58}],routes:[[[52,58],[55,52]]],
  srcs:[['Sahih al-Bukhari, Book of Revelation','Authentic hadith — the primary narration','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'public',y:613,hij:'—',cat:'revelation',t:'Public Preaching Begins',ar:'الجهر بالدعوة',
  loc:'Makkah',cert:'confirmed',key:false,themes:['makkah','revelation','persecution'],
  sum:'After roughly three years of private teaching among family and close associates, the Prophet ﷺ was commanded to proclaim the message openly. He ascended Mount Safa and called the clans of Quraysh together. The response marked the beginning of open hostility, and persecution of the weaker and enslaved converts intensified sharply.',
  why:'Islam\'s call to abandon idols struck directly at the religious authority, pilgrimage economy, and tribal hierarchy on which Quraysh leadership rested — which is why opposition escalated from mockery to organised persecution.',
  verses:[{s:'Al-Hijr',n:'15:94',ar:'فَاصْدَعْ بِمَا تُؤْمَرُ وَأَعْرِضْ عَنِ الْمُشْرِكِينَ',tr:'Then declare openly what you are commanded and turn away from the polytheists.',note:'Understood by scholars as the command that ended the private phase of the call.'}],
  people:[{n:'Abu Bakr as-Siddiq',ar:'أبو بكر الصديق',r:'Among the first free men to believe; bought and freed persecuted enslaved converts including Bilal.'},{n:'Bilal ibn Rabah',ar:'بلال بن رباح',r:'Enslaved and tortured for his belief; later the first muadhdhin of Islam.'},{n:'Abu Lahab',ar:'أبو لهب',r:'The Prophet\'s uncle and among the most hostile opponents of the message.'}],
  lessons:[{t:'Cost of Conviction',x:'The earliest and most severe persecution fell on those with the least tribal protection — the enslaved and the poor.'},{t:'Gradual Method',x:'Three years of quiet foundation preceded public proclamation; the sequencing was deliberate, not hesitant.'}],
  places:[{n:'Mount Safa',x:53,y:57},{n:'Makkah',x:52,y:58}],routes:[],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ar-Raheeq Al-Makhtum','Modern scholarly synthesis','b']]},

 {id:'abyssinia',y:615,hij:'—',cat:'migration',t:'Migration to Abyssinia',ar:'الهجرة إلى الحبشة',
  loc:'Abyssinia (Ethiopia)',cert:'confirmed',key:false,themes:['persecution','migration'],
  sum:'As persecution intensified, the Prophet ﷺ directed a group of companions to seek refuge with the Negus (An-Najashi), the Christian king of Abyssinia, describing him as a ruler under whom no one is wronged. Quraysh sent a delegation to demand their return; Ja\'far ibn Abi Talib answered the king by reciting verses from Surah Maryam concerning Jesus and his mother, and the Negus refused to surrender them.',
  why:'This was the first organised migration in Islam and a deliberate political calculation — placing vulnerable believers beyond Quraysh\'s reach under a ruler whose own faith would make him sympathetic.',
  verses:[{s:'Maryam',n:'19:16-21',ar:'وَاذْكُرْ فِي الْكِتَابِ مَرْيَمَ إِذِ انتَبَذَتْ مِنْ أَهْلِهَا مَكَانًا شَرْقِيًّا',tr:'And mention, in the Book, Maryam — when she withdrew from her family to a place toward the east.',note:'Recited by Ja\'far before the Negus; the shared reverence for Jesus and Mary is what secured the refuge.'}],
  people:[{n:'Ja\'far ibn Abi Talib',ar:'جعفر بن أبي طالب',r:'Spokesman before the Negus; his address is among the most famous speeches of the Seerah.'},{n:'An-Najashi (the Negus)',ar:'النجاشي',r:'Christian king of Abyssinia who granted protection and refused Quraysh\'s demands.'},{n:'Umm Salamah',ar:'أم سلمة',r:'Among the migrants; later a wife of the Prophet ﷺ and a narrator of this episode.'}],
  lessons:[{t:'Strategic Retreat',x:'Withdrawal to preserve a vulnerable community was a legitimate and deliberate strategy, not a failure of resolve.'},{t:'Common Ground',x:'Ja\'far won protection by appealing to shared belief rather than by argument or confrontation.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Red Sea Port',x:44,y:70},{n:'Abyssinia',x:30,y:88}],routes:[[[52,58],[44,70],[30,88]]],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a'],['Musnad Ahmad','Authentic hadith collection','a']]},

 {id:'boycott',y:617,hij:'—',cat:'life',t:'Boycott of Banu Hashim',ar:'حصار الشعب',
  loc:'Shi\'b Abi Talib, Makkah',cert:'confirmed',key:false,themes:['makkah','persecution'],
  sum:'Quraysh drew up a written pact suspending all trade, intermarriage, and social dealings with Banu Hashim and Banu al-Muttalib until they surrendered the Prophet ﷺ. The clans were confined to a valley outside Makkah for roughly three years, enduring severe hunger. The boycott collapsed when sympathetic Qurayshis broke the pact, and the document was found consumed by termites.',
  why:'Unable to reach the Prophet ﷺ directly because of Abu Talib\'s protection, Quraysh applied collective pressure on his entire clan — an attempt to make tribal loyalty itself unbearable.',
  verses:[],
  people:[{n:'Abu Talib',ar:'أبو طالب',r:'Bore the boycott with his clan rather than surrender his nephew.'},{n:'Hisham ibn Amr',ar:'هشام بن عمرو',r:'Among the Qurayshis who worked to annul the pact.'}],
  lessons:[{t:'Endurance',x:'Roughly three years of deprivation preceded any relief — the Meccan period was largely one of patience without visible victory.'},{t:'Conscience Across Lines',x:'The boycott ended because people outside the community found it unjust; moral appeal reached beyond belief.'}],
  places:[{n:'Shi\'b Abi Talib',x:54,y:56},{n:'Makkah',x:52,y:58}],routes:[],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'sorrow',y:619,hij:'—',cat:'life',t:'The Year of Sorrow',ar:'عام الحزن',
  loc:'Makkah & Ta\'if',cert:'confirmed',key:false,themes:['makkah','persecution','family'],
  sum:'Within a short span the Prophet ﷺ lost both Abu Talib, his tribal protector, and Khadijah, his wife and first believer. Stripped of protection, he travelled to Ta\'if seeking support and was driven out and stoned by its people. He returned to Makkah under the protection of a non-Muslim, Mut\'im ibn Adi.',
  why:'Losing Abu Talib removed the tribal shield that had made him untouchable; losing Khadijah removed his closest personal support. Quraysh escalated immediately, and the Meccan phase reached its lowest point.',
  verses:[],
  people:[{n:'Mut\'im ibn Adi',ar:'مطعم بن عدي',r:'A non-Muslim of Quraysh who granted him protection to re-enter Makkah — remembered with gratitude afterwards.'},{n:'Zayd ibn Harithah',ar:'زيد بن حارثة',r:'Accompanied him to Ta\'if and shielded him during the stoning.'}],
  lessons:[{t:'Lowest Point',x:'The hardest year of his life came before the greatest openings — Ta\'if preceded the Isra and then the Hijrah.'},{t:'Mercy Under Injury',x:'Offered destruction of Ta\'if, he refused, hoping their descendants would one day believe.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Ta\'if',x:60,y:64}],routes:[[[52,58],[60,64]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'isra',y:621,hij:'—',cat:'revelation',t:'Al-Isra & Al-Mi\'raj',ar:'الإسراء والمعراج',
  loc:'Makkah → Jerusalem → the Heavens',cert:'confirmed',key:true,themes:['revelation','worship'],
  sum:'The Prophet ﷺ was taken by night from the Sacred Mosque in Makkah to Al-Aqsa in Jerusalem, and from there ascended through the heavens. During this journey the five daily prayers were prescribed. On his return, many in Makkah rejected the account outright; Abu Bakr affirmed it without hesitation, and it is said he earned the title as-Siddiq — the truthful one — for this.',
  why:'It came directly after the Year of Sorrow, at the point of deepest hardship, and it established the prayer — the single practice that would structure the daily life of the community from then on.',
  verses:[{s:'Al-Isra',n:'17:1',ar:'سُبْحَانَ الَّذِي أَسْرَىٰ بِعَبْدِهِ لَيْلًا مِّنَ الْمَسْجِدِ الْحَرَامِ إِلَى الْمَسْجِدِ الْأَقْصَى الَّذِي بَارَكْنَا حَوْلَهُ',tr:'Exalted is He who took His Servant by night from Al-Masjid al-Haram to Al-Masjid al-Aqsa, whose surroundings We have blessed.',note:'The opening verse of Surah Al-Isra, naming both the origin and destination of the night journey.'},{s:'An-Najm',n:'53:13-18',ar:'وَلَقَدْ رَآهُ نَزْلَةً أُخْرَىٰ ۝ عِندَ سِدْرَةِ الْمُنتَهَىٰ',tr:'And he certainly saw him in another descent, at the Lote Tree of the Utmost Boundary.',note:'Understood by many commentators as describing the ascent.'}],
  people:[{n:'Abu Bakr as-Siddiq',ar:'أبو بكر الصديق',r:'Affirmed the account immediately when others rejected it.'}],
  lessons:[{t:'Relief After Hardship',x:'The greatest honour of his life followed the hardest year of his life.'},{t:'Trust Built on Record',x:'Abu Bakr\'s certainty rested on decades of knowing the man — a lesson about how trust is actually earned.'}],
  places:[{n:'Makkah',x:52,y:58},{n:'Jerusalem',x:34,y:20}],routes:[[[52,58],[34,20]]],
  srcs:[['Sahih al-Bukhari & Sahih Muslim','Authentic hadith — multiple narrations','a'],['Quran 17:1, 53:13-18','Quranic text','a']]},

 {id:'aqaba',y:621,hij:'—',cat:'treaty',t:'The Pledges of Aqaba',ar:'بيعتا العقبة',
  loc:'Aqaba, near Mina',cert:'confirmed',key:false,themes:['madinah','treaty'],
  sum:'During the pilgrimage season, delegations from Yathrib (later Madinah) met the Prophet ﷺ at Aqaba. In the first pledge a small group committed to belief and basic moral obligations. A year later, in the second pledge, some seventy-three men and two women pledged to protect him as they would their own families — effectively an offer of political refuge and military protection.',
  why:'Yathrib was exhausted by the long civil conflict between the tribes of Aws and Khazraj and needed an authority both could accept. This convergence of need is what made the Hijrah possible.',
  verses:[],
  people:[{n:'Mus\'ab ibn Umayr',ar:'مصعب بن عمير',r:'Sent ahead to Yathrib as the first teacher of Islam there; his work prepared the city.'},{n:'Sa\'d ibn Mu\'adh',ar:'سعد بن معاذ',r:'Chief of the Aws whose acceptance brought his clan with him.'}],
  lessons:[{t:'Preparation Before Migration',x:'The Hijrah succeeded because groundwork had been laid in Yathrib for two years beforehand.'},{t:'The Right Envoy',x:'A single well-chosen teacher changed the standing of the message in an entire city.'}],
  places:[{n:'Aqaba/Mina',x:53,y:56},{n:'Yathrib',x:50,y:40}],routes:[[[50,40],[53,56]]],
  srcs:[['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'hijrah',y:622,hij:'1 AH',cat:'migration',t:'The Hijrah to Madinah',ar:'الهجرة إلى المدينة',
  loc:'Makkah → Madinah',cert:'confirmed',key:true,themes:['madinah','migration','community'],
  sum:'With Quraysh plotting to kill him, the Prophet ﷺ left Makkah with Abu Bakr. They sheltered three nights in the Cave of Thawr, south of the city, while pursuers searched, then took an indirect coastal route north to Yathrib — thereafter Madinah. On arrival he established the mosque, formally paired each Emigrant with a Helper, and drew up the Constitution of Madinah governing relations between Muslims, the Jewish tribes, and other groups of the city.',
  why:'This is the pivot of the entire Seerah: from a persecuted minority with no protection to a community with territory, a treaty framework, and the capacity to govern. The Islamic calendar is dated from this year for precisely this reason.',
  verses:[{s:'At-Tawbah',n:'9:40',ar:'إِذْ يَقُولُ لِصَاحِبِهِ لَا تَحْزَنْ إِنَّ اللَّهَ مَعَنَا',tr:'When he said to his companion, "Do not grieve; indeed Allah is with us."',note:'Refers directly to the Prophet ﷺ and Abu Bakr in the Cave of Thawr.'}],
  people:[{n:'Abu Bakr as-Siddiq',ar:'أبو بكر الصديق',r:'His sole companion on the journey and in the cave.'},{n:'Ali ibn Abi Talib',ar:'علي بن أبي طالب',r:'Slept in the Prophet\'s bed to mask his departure, then returned entrusted property to its owners.'},{n:'Asma bint Abi Bakr',ar:'أسماء بنت أبي بكر',r:'Carried provisions to the cave in secret.'},{n:'Suraqah ibn Malik',ar:'سراقة بن مالك',r:'Pursued them for the bounty and turned back; later accepted Islam.'}],
  lessons:[{t:'Planning and Trust',x:'A hidden cave, an indirect route, a decoy, and a guide — thorough preparation alongside complete reliance on God.'},{t:'Building Institutions',x:'His first acts in Madinah were a mosque, a brotherhood pact, and a written constitution — community before conquest.'},{t:'Returning Trusts',x:'Even fleeing for his life, he made sure the deposits of those persecuting him were returned.'}],
  places:[{n:'Makkah',x:52,y:62},{n:'Cave of Thawr',x:54,y:66},{n:'Quba',x:50,y:42},{n:'Madinah',x:49,y:38}],routes:[[[52,62],[54,66],[44,54],[46,46],[50,42],[49,38]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'qibla',y:624,hij:'2 AH',cat:'revelation',t:'The Change of the Qibla',ar:'تحويل القبلة',
  loc:'Madinah',cert:'confirmed',key:false,themes:['madinah','worship','revelation'],
  sum:'For roughly sixteen or seventeen months after the Hijrah the Muslims prayed toward Jerusalem. A revelation then directed the Prophet ﷺ to turn in prayer toward the Sacred Mosque in Makkah. The change came mid-prayer in one narration, and the community turned together — a moment remembered in the name "Mosque of the Two Qiblas."',
  why:'The redirection distinguished the young community\'s worship and re-centred it on the Kaaba, the sanctuary of Ibrahim — a claim of continuity with the primordial monotheism rather than dependence on any existing community.',
  verses:[{s:'Al-Baqarah',n:'2:144',ar:'قَدْ نَرَىٰ تَقَلُّبَ وَجْهِكَ فِي السَّمَاءِ ۖ فَلَنُوَلِّيَنَّكَ قِبْلَةً تَرْضَاهَا ۚ فَوَلِّ وَجْهَكَ شَطْرَ الْمَسْجِدِ الْحَرَامِ',tr:'We have certainly seen the turning of your face toward the heaven, and We will surely turn you to a qibla with which you will be pleased. So turn your face toward al-Masjid al-Haram.',note:'Revealed in response to the Prophet\'s longing, turning the direction of prayer to Makkah.'}],
  people:[{n:'Al-Bara ibn Azib',ar:'البراء بن عازب',r:'A companion whose narration preserves the account of the change of direction.'}],
  lessons:[{t:'Obedience Without Delay',x:'The community turned the moment the command came, before questioning its wisdom — trust expressed as immediate action.'},{t:'Identity in Worship',x:'A single change in orientation gave the community a distinct centre of gravity of its own.'}],
  places:[{n:'Madinah',x:50,y:36},{n:'Jerusalem',x:36,y:20},{n:'Makkah',x:52,y:66}],routes:[[[50,36],[52,66]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Quran 2:142-150','Quranic text','a']]},

 {id:'badr',y:624,hij:'2 AH',cat:'battle',t:'The Battle of Badr',ar:'غزوة بدر',
  loc:'Badr, west of Madinah',cert:'confirmed',key:true,themes:['madinah','battle'],
  sum:'Roughly three hundred and thirteen Muslims met a Quraysh force around three times their number at the wells of Badr. The Muslims reached the water first and held the superior ground. The battle ended in a decisive victory; several senior Quraysh leaders, including Abu Jahl, were killed. The Prophet ﷺ ruled that prisoners could purchase their freedom, and that a literate captive could ransom himself by teaching ten Muslim children to read.',
  why:'Quraysh had confiscated the property of the Emigrants, and the confrontation began as an interception of a returning caravan. Badr established the young community in Madinah as a real power in the region rather than a group of refugees.',
  verses:[{s:'Al-Anfal',n:'8:17',ar:'فَلَمْ تَقْتُلُوهُمْ وَلَـٰكِنَّ اللَّهَ قَتَلَهُمْ ۚ وَمَا رَمَيْتَ إِذْ رَمَيْتَ وَلَـٰكِنَّ اللَّهَ رَمَىٰ',tr:'You did not kill them, but it was Allah who killed them. And you threw not when you threw, but it was Allah who threw.',note:'Surah Al-Anfal deals extensively with Badr, its conduct, and the disposition of its spoils.'},{s:'Al Imran',n:'3:123',ar:'وَلَقَدْ نَصَرَكُمُ اللَّهُ بِبَدْرٍ وَأَنتُمْ أَذِلَّةٌ',tr:'And already had Allah given you victory at Badr while you were few in number.',note:'A direct reference to Badr by name, recalling the disparity in numbers.'}],
  people:[{n:'Hamza ibn Abd al-Muttalib',ar:'حمزة بن عبد المطلب',r:'The Prophet\'s uncle; fought at the front of the battle.'},{n:'Ali ibn Abi Talib',ar:'علي بن أبي طالب',r:'Among the champions in the opening single combat.'},{n:'Al-Hubab ibn al-Mundhir',ar:'الحباب بن المنذر',r:'Proposed repositioning the army to control the wells — a suggestion the Prophet ﷺ adopted.'},{n:'Abu Jahl',ar:'أبو جهل',r:'Commander of the Quraysh force; killed in the battle.'}],
  lessons:[{t:'Consultation',x:'The Prophet ﷺ changed the army\'s position on the advice of a companion — leadership that genuinely takes counsel.'},{t:'Preparation With Reliance',x:'Controlling the water supply and choosing the ground preceded the prayer for victory; both mattered.'},{t:'Knowledge as Ransom',x:'Literate captives bought their freedom by teaching children to read — a striking valuation of literacy.'}],
  places:[{n:'Madinah',x:52,y:32},{n:'Badr',x:38,y:44},{n:'Makkah',x:56,y:74}],routes:[[[52,32],[38,44]],[[56,74],[38,44]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a'],['Ar-Raheeq Al-Makhtum','Modern scholarly synthesis','b']]},

 {id:'uhud',y:625,hij:'3 AH',cat:'battle',t:'The Battle of Uhud',ar:'غزوة أحد',
  loc:'Mount Uhud, near Madinah',cert:'confirmed',key:true,themes:['madinah','battle'],
  sum:'Quraysh returned with around three thousand men to avenge Badr. The Prophet ﷺ positioned fifty archers on a hill with strict orders not to leave it under any circumstances. The Muslims gained the early advantage, but when most of the archers descended to collect spoils, Khalid ibn al-Walid — then still fighting for Quraysh — led cavalry around the exposed flank and reversed the battle. Hamza was killed, and the Prophet ﷺ himself was wounded.',
  why:'Uhud is the direct consequence of Badr: Quraysh could not accept the loss of prestige and of their leaders. The reversal came from a single lapse in discipline, and the Quran addresses the episode at length rather than passing over it.',
  verses:[{s:'Al Imran',n:'3:152',ar:'حَتَّىٰ إِذَا فَشِلْتُمْ وَتَنَازَعْتُمْ فِي الْأَمْرِ وَعَصَيْتُم مِّن بَعْدِ مَا أَرَاكُم مَّا تُحِبُّونَ',tr:'Until, when you lost courage and fell to disputing about the order and disobeyed after He had shown you that which you love.',note:'A direct and unsparing Quranic account of what went wrong at Uhud.'},{s:'Al Imran',n:'3:159',ar:'فَبِمَا رَحْمَةٍ مِّنَ اللَّهِ لِنتَ لَهُمْ ۖ وَلَوْ كُنتَ فَظًّا غَلِيظَ الْقَلْبِ لَانفَضُّوا مِنْ حَوْلِكَ',tr:'So by mercy from Allah you were lenient with them. And if you had been rude and harsh in heart, they would have disbanded from about you.',note:'Revealed after Uhud, instructing gentleness toward those whose error had just cost the battle.'}],
  people:[{n:'Hamza ibn Abd al-Muttalib',ar:'حمزة بن عبد المطلب',r:'Martyred at Uhud; the Prophet ﷺ grieved him deeply.'},{n:'Abdullah ibn Jubayr',ar:'عبد الله بن جبير',r:'Commander of the archers; held his post with a few others and was killed.'},{n:'Khalid ibn al-Walid',ar:'خالد بن الوليد',r:'Led the Quraysh cavalry manoeuvre; later accepted Islam and became a celebrated commander.'},{n:'Nusaybah bint Ka\'b',ar:'نسيبة بنت كعب',r:'Fought in defence of the Prophet ﷺ when the lines broke.'}],
  lessons:[{t:'Discipline',x:'A single order disregarded turned a winning position into a costly defeat.'},{t:'Leadership After Failure',x:'The response to catastrophic error was gentleness, not recrimination — as the revelation itself directed.'},{t:'Honest Record',x:'The Quran records the community\'s failure plainly; the Seerah does not sanitise its setbacks.'}],
  places:[{n:'Madinah',x:52,y:52},{n:'Mount Uhud',x:52,y:40},{n:'Archers\' Hill',x:57,y:44}],routes:[[[52,52],[52,40]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'trench',y:627,hij:'5 AH',cat:'battle',t:'The Battle of the Trench',ar:'غزوة الخندق',
  loc:'Madinah',cert:'confirmed',key:true,themes:['madinah','battle'],
  sum:'A coalition of Quraysh, Ghatafan, and allied tribes — some ten thousand men — advanced on Madinah. On the proposal of Salman al-Farisi, the Muslims dug a trench across the city\'s exposed northern approach, a tactic unfamiliar to Arabian warfare. The siege lasted around a month before cold, wind, dwindling supplies, and fractured alliances forced the coalition to withdraw without a general engagement.',
  why:'This was the largest attempt to destroy the Madinan community outright. Its failure ended the confederacy\'s capacity to take the offensive, and the initiative passed permanently to Madinah.',
  verses:[{s:'Al-Ahzab',n:'33:9',ar:'يَا أَيُّهَا الَّذِينَ آمَنُوا اذْكُرُوا نِعْمَةَ اللَّهِ عَلَيْكُمْ إِذْ جَاءَتْكُمْ جُنُودٌ فَأَرْسَلْنَا عَلَيْهِمْ رِيحًا وَجُنُودًا لَّمْ تَرَوْهَا',tr:'O you who have believed, remember the favour of Allah upon you when armies came to you and We sent upon them a wind and armies you did not see.',note:'Surah Al-Ahzab — named for the confederates — narrates the siege in detail, including the strain on the community.'}],
  people:[{n:'Salman al-Farisi',ar:'سلمان الفارسي',r:'Proposed the trench, drawing on Persian siege practice unknown in Arabia.'},{n:'Sa\'d ibn Mu\'adh',ar:'سعد بن معاذ',r:'Wounded during the siege; died of it shortly afterwards.'},{n:'Nu\'aym ibn Mas\'ud',ar:'نعيم بن مسعود',r:'Newly Muslim; worked to break the trust between the allied factions.'}],
  lessons:[{t:'Openness to Ideas',x:'A tactic from outside Arabian tradition, proposed by a Persian companion, saved the city.'},{t:'Unity in Labour',x:'The Prophet ﷺ dug alongside everyone else — leadership by shared hardship, not direction from a distance.'},{t:'Endurance',x:'Al-Ahzab records the fear and strain openly rather than presenting the siege as easily borne.'}],
  places:[{n:'Madinah',x:50,y:52},{n:'The Trench',x:50,y:42},{n:'Coalition Camp',x:50,y:30}],routes:[[[50,30],[50,42]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'hudaybiyyah',y:628,hij:'6 AH',cat:'treaty',t:'Treaty of Hudaybiyyah',ar:'صلح الحديبية',
  loc:'Hudaybiyyah, near Makkah',cert:'confirmed',key:true,themes:['treaty','makkah'],
  sum:'The Prophet ﷺ set out with around fourteen hundred companions to perform umrah, not to fight. Quraysh barred them at Hudaybiyyah. The negotiated treaty appeared one-sided: the Muslims would return without performing umrah that year, a ten-year truce was agreed, and anyone fleeing Quraysh to Madinah would be returned while the reverse did not apply. Many companions were distressed by the terms. Within two years the truce had allowed Islam to spread more widely than the preceding nineteen years of conflict.',
  why:'The treaty converted an armed standoff into open borders. Free movement and the pause in hostilities let the message travel through ordinary contact — which is why the Quran calls it a clear victory.',
  verses:[{s:'Al-Fath',n:'48:1',ar:'إِنَّا فَتَحْنَا لَكَ فَتْحًا مُّبِينًا',tr:'Indeed, We have given you a clear conquest.',note:'Revealed concerning Hudaybiyyah — naming as a clear victory what the companions had experienced as a painful concession.'},{s:'Al-Fath',n:'48:18',ar:'لَّقَدْ رَضِيَ اللَّهُ عَنِ الْمُؤْمِنِينَ إِذْ يُبَايِعُونَكَ تَحْتَ الشَّجَرَةِ',tr:'Certainly was Allah pleased with the believers when they pledged allegiance to you under the tree.',note:'The Pledge of Ridwan, given at Hudaybiyyah before the treaty was concluded.'}],
  people:[{n:'Uthman ibn Affan',ar:'عثمان بن عفان',r:'Sent as envoy into Makkah; rumours of his killing prompted the Pledge of Ridwan.'},{n:'Umar ibn al-Khattab',ar:'عمر بن الخطاب',r:'Openly questioned the terms, and later said he gave charity in regret for having doubted.'},{n:'Suhayl ibn Amr',ar:'سهيل بن عمرو',r:'Negotiated the treaty on behalf of Quraysh.'}],
  lessons:[{t:'Long View',x:'Terms that looked like defeat produced the greatest expansion of the community to that point.'},{t:'Room to Disagree',x:'Umar voiced his objection directly to the Prophet ﷺ — dissent was expressed, heard, and did not fracture the community.'},{t:'Peace as Strategy',x:'The decisive breakthrough of the Seerah came from a treaty, not a battle.'}],
  places:[{n:'Madinah',x:52,y:28},{n:'Hudaybiyyah',x:50,y:60},{n:'Makkah',x:53,y:66}],routes:[[[52,28],[50,60]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith — extensive narration','a'],['Ar-Raheeq Al-Makhtum','Modern scholarly synthesis','b']]},

 {id:'khaybar',y:628,hij:'7 AH',cat:'battle',t:'The Expedition of Khaybar',ar:'غزوة خيبر',
  loc:'Khaybar, north of Madinah',cert:'confirmed',key:false,themes:['madinah','battle'],
  sum:'Some months after Hudaybiyyah, the Prophet ﷺ led an expedition against the fortified oasis of Khaybar, whose leaders had been active in mobilising the confederacy against Madinah. After a series of engagements against its strongholds the oasis surrendered. Its inhabitants were permitted to remain and continue farming the land in exchange for a share of the produce — an arrangement later cited in the law of agricultural partnership.',
  why:'Securing the northern frontier removed the last organised base of hostility around Madinah and, with the Meccan front frozen by the truce, consolidated the community\'s position across the Hijaz.',
  verses:[{s:'Al-Fath',n:'48:20',ar:'وَعَدَكُمُ اللَّهُ مَغَانِمَ كَثِيرَةً تَأْخُذُونَهَا فَعَجَّلَ لَكُمْ هَـٰذِهِ',tr:'Allah has promised you much booty that you will take, and He has hastened for you this.',note:'Understood by commentators as referring to the gains of Khaybar, promised in the aftermath of Hudaybiyyah.'}],
  people:[{n:'Ali ibn Abi Talib',ar:'علي بن أبي طالب',r:'Given the banner and entrusted with the decisive assault on the strongholds.'},{n:'Ja\'far ibn Abi Talib',ar:'جعفر بن أبي طالب',r:'Returned from Abyssinia at this time; the Prophet ﷺ welcomed him with visible joy.'},{n:'Safiyyah bint Huyayy',ar:'صفية بنت حيي',r:'Of the people of Khaybar; later became a wife of the Prophet ﷺ.'}],
  lessons:[{t:'Coexistence by Contract',x:'The people of Khaybar kept their land and livelihood under a documented arrangement rather than being displaced.'},{t:'Sequencing',x:'Peace on one front made it possible to resolve another — Hudaybiyyah is what freed the hand that settled Khaybar.'}],
  places:[{n:'Madinah',x:50,y:60},{n:'Khaybar',x:48,y:30}],routes:[[[50,60],[48,30]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'mutah',y:629,hij:'8 AH',cat:'battle',t:'The Battle of Mu\'tah',ar:'غزوة مؤتة',
  loc:'Mu\'tah, southern Levant',cert:'confirmed',key:false,themes:['battle','frontier'],
  sum:'A force of some three thousand was sent north toward the borders of the Byzantine sphere after an envoy of the Prophet ﷺ was killed. The three named commanders — Zayd ibn Harithah, then Ja\'far ibn Abi Talib, then Abdullah ibn Rawahah — fell in succession. Khalid ibn al-Walid took the banner, held the army together, and withdrew it intact against far greater numbers.',
  why:'It was the first major encounter on the northern frontier and a signal that the message was now reaching beyond Arabia. The disciplined withdrawal, preserving the force, was itself remembered as an achievement.',
  verses:[],
  people:[{n:'Zayd ibn Harithah',ar:'زيد بن حارثة',r:'First commander; the Prophet\'s freedman and among the earliest believers, martyred here.'},{n:'Ja\'far ibn Abi Talib',ar:'جعفر بن أبي طالب',r:'Took the banner after Zayd and was martyred; remembered as Ja\'far of the Two Wings.'},{n:'Abdullah ibn Rawahah',ar:'عبد الله بن رواحة',r:'The third commander and a poet of the Ansar; martyred at Mu\'tah.'},{n:'Khalid ibn al-Walid',ar:'خالد بن الوليد',r:'Assumed command after the three fell and extracted the army; named the Sword of Allah.'}],
  lessons:[{t:'Succession Under Fire',x:'A clear line of command meant the army never broke even as its leaders fell one after another.'},{t:'Preservation Over Glory',x:'Bringing the force home intact was valued above a doomed advance — prudence counted as victory.'}],
  places:[{n:'Madinah',x:52,y:66},{n:'Mu\'tah',x:38,y:22}],routes:[[[52,66],[38,22]]],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'conquest',y:630,hij:'8 AH',cat:'migration',t:'The Conquest of Makkah',ar:'فتح مكة',
  loc:'Makkah',cert:'confirmed',key:true,themes:['makkah','community'],
  sum:'After Quraysh\'s allies violated the truce, the Prophet ﷺ advanced on Makkah with around ten thousand men. The city was taken with almost no fighting. He entered with his head lowered on his mount, declared a general amnesty — "Go, for you are free" — and cleared the idols from the Kaaba, reciting that truth has come and falsehood has departed. Those who had persecuted the community for twenty years, including Abu Sufyan and Hind, were pardoned.',
  why:'Hudaybiyyah had already shifted the balance decisively; the conquest confirmed it. The amnesty, at the moment of complete power over former persecutors, is among the most cited episodes of the Seerah.',
  verses:[{s:'Al-Isra',n:'17:81',ar:'وَقُلْ جَاءَ الْحَقُّ وَزَهَقَ الْبَاطِلُ ۚ إِنَّ الْبَاطِلَ كَانَ زَهُوقًا',tr:'And say, "Truth has come, and falsehood has departed. Indeed falsehood is bound to depart."',note:'Recited by the Prophet ﷺ as the idols were removed from the Kaaba.'},{s:'An-Nasr',n:'110:1-2',ar:'إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ ۝ وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا',tr:'When the victory of Allah has come and the conquest, and you see the people entering into the religion of Allah in multitudes.',note:'Among the last surahs revealed, understood as referring to this moment.'}],
  people:[{n:'Abu Sufyan',ar:'أبو سفيان',r:'Longtime leader of the opposition; accepted Islam before the entry and was granted safety.'},{n:'Bilal ibn Rabah',ar:'بلال بن رباح',r:'Once tortured in these streets, he gave the call to prayer from atop the Kaaba.'},{n:'Khalid ibn al-Walid',ar:'خالد بن الوليد',r:'Commanded a column of the advancing army.'}],
  lessons:[{t:'Amnesty Over Revenge',x:'At the point of total victory over those who had driven him out, he pardoned them.'},{t:'Humility in Triumph',x:'He entered the city with his head bowed low on his mount rather than in a victor\'s procession.'},{t:'Reversal',x:'The formerly enslaved Bilal calling the adhan from the Kaaba inverted the city\'s entire social order in a single act.'}],
  places:[{n:'Madinah',x:52,y:26},{n:'Marr az-Zahran',x:50,y:52},{n:'Makkah',x:52,y:62}],routes:[[[52,26],[50,52],[52,62]]],
  srcs:[['Sahih al-Bukhari & Sahih Muslim','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'hunayn',y:630,hij:'8 AH',cat:'battle',t:'The Battle of Hunayn',ar:'غزوة حنين',
  loc:'Valley of Hunayn, near Ta\'if',cert:'confirmed',key:false,themes:['battle','makkah'],
  sum:'Weeks after the conquest of Makkah, the tribes of Hawazin and Thaqif assembled to challenge the new order. In the narrow valley of Hunayn the Muslim army — now its largest ever, around twelve thousand — was ambushed and its front ranks scattered. The Prophet ﷺ held his ground and rallied the companions, and the day was recovered. The Quran points to the episode as a lesson that numbers alone secure nothing.',
  why:'Coming immediately after the euphoria of Makkah, Hunayn was a corrective: the largest force the community had ever fielded nearly lost because it leaned on its own strength. The reversal and recovery became a fixed reference point in the Quran.',
  verses:[{s:'At-Tawbah',n:'9:25',ar:'وَيَوْمَ حُنَيْنٍ إِذْ أَعْجَبَتْكُمْ كَثْرَتُكُمْ فَلَمْ تُغْنِ عَنكُمْ شَيْئًا',tr:'And on the day of Hunayn, when your great number pleased you, but it did not avail you at all.',note:'Hunayn is named directly in the Quran, recalling the danger of relying on numbers.'}],
  people:[{n:'Al-Abbas ibn Abd al-Muttalib',ar:'العباس بن عبد المطلب',r:'Called the companions back to the Prophet ﷺ with his powerful voice when the lines broke.'},{n:'Malik ibn Awf',ar:'مالك بن عوف',r:'Led the Hawazin coalition; later accepted Islam.'}],
  lessons:[{t:'Numbers Are Not Strength',x:'The largest army yet nearly lost — the Quran fixes this as a warning against self-reliance.'},{t:'Steadiness Under Panic',x:'The recovery began with a handful holding firm around their leader while the rest regrouped.'}],
  places:[{n:'Makkah',x:48,y:56},{n:'Hunayn',x:56,y:60},{n:'Ta\'if',x:60,y:66}],routes:[[[48,56],[56,60],[60,66]]],
  srcs:[['Sahih al-Bukhari & Sahih Muslim','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'tabuk',y:630,hij:'9 AH',cat:'battle',t:'The Expedition of Tabuk',ar:'غزوة تبوك',
  loc:'Tabuk, north-western Arabia',cert:'confirmed',key:false,themes:['frontier','community'],
  sum:'In severe heat and hardship, the Prophet ﷺ called for a large expedition north toward Tabuk in response to reports of a Byzantine mobilisation. The community equipped the army through voluntary giving despite drought and difficulty. No battle occurred; the force reached Tabuk, secured agreements with frontier communities, and returned. The episode is remembered for the test of sincerity it posed and for the three companions who stayed behind and were later forgiven.',
  why:'This was the last major expedition the Prophet ﷺ led. Its significance was less military than moral — a demonstration of the community\'s willingness to answer a costly call, and the setting of the celebrated account of the three who were left behind.',
  verses:[{s:'At-Tawbah',n:'9:118',ar:'وَعَلَى الثَّلَاثَةِ الَّذِينَ خُلِّفُوا حَتَّىٰ إِذَا ضَاقَتْ عَلَيْهِمُ الْأَرْضُ بِمَا رَحُبَتْ',tr:'And He turned in mercy to the three who were left behind, until the earth closed in on them despite its vastness.',note:'The story of Ka\'b ibn Malik and his two companions, forgiven after a period of testing, is drawn from this passage.'}],
  people:[{n:'Ka\'b ibn Malik',ar:'كعب بن مالك',r:'One of the three sincere believers who stayed behind, confessed truthfully, and was forgiven.'},{n:'Uthman ibn Affan',ar:'عثمان بن عفان',r:'Equipped a large portion of the army from his own wealth.'},{n:'Ali ibn Abi Talib',ar:'علي بن أبي طالب',r:'Left in charge of Madinah during the expedition.'}],
  lessons:[{t:'Costly Obedience',x:'The community answered a hard call in drought and heat — commitment measured by difficulty, not ease.'},{t:'Truth Over Excuse',x:'Ka\'b\'s honest admission, rather than a plausible excuse, is what ultimately earned his forgiveness.'}],
  places:[{n:'Madinah',x:50,y:70},{n:'Tabuk',x:46,y:22}],routes:[[[50,70],[46,22]]],
  srcs:[['Sahih al-Bukhari — Hadith of Ka\'b ibn Malik','Authentic hadith — the fullest account','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]},

 {id:'farewell',y:632,hij:'10 AH',cat:'revelation',t:'The Farewell Pilgrimage',ar:'حجة الوداع',
  loc:'Arafat, Makkah',cert:'confirmed',key:true,themes:['makkah','worship','community'],
  sum:'The Prophet ﷺ performed Hajj with well over a hundred thousand companions and delivered the Farewell Sermon at Arafat. He abolished the blood feuds and usury of the pre-Islamic era, affirmed the rights of women, declared that no Arab has superiority over a non-Arab except in piety, and declared the sanctity of every person\'s life, property, and honour. He died some three months later in Madinah.',
  why:'This was the summation of twenty-three years of prophethood — the principles of the message stated openly before the largest gathering of his lifetime, at the moment they could be most widely transmitted.',
  verses:[{s:'Al-Ma\'idah',n:'5:3',ar:'الْيَوْمَ أَكْمَلْتُ لَكُمْ دِينَكُمْ وَأَتْمَمْتُ عَلَيْكُمْ نِعْمَتِي وَرَضِيتُ لَكُمُ الْإِسْلَامَ دِينًا',tr:'This day I have perfected for you your religion and completed My favour upon you and have approved for you Islam as religion.',note:'Revealed at Arafat during this pilgrimage. Narrated that a Jewish man told Umar that had such a verse come to them, they would have made that day a festival.'}],
  people:[{n:'Abu Bakr as-Siddiq',ar:'أبو بكر الصديق',r:'Led the pilgrimage the previous year and led prayers in the Prophet\'s final illness.'},{n:'Aisha bint Abi Bakr',ar:'عائشة بنت أبي بكر',r:'Narrated a large portion of the accounts of his final months.'},{n:'Bilal ibn Rabah',ar:'بلال بن رباح',r:'Called the prayer during the pilgrimage.'}],
  lessons:[{t:'Universal Equality',x:'No superiority of Arab over non-Arab, nor white over black, except by piety — declared before the largest audience of his life.'},{t:'Rights and Obligations',x:'The sermon addressed debts, inheritance, the treatment of women, and blood feuds in concrete terms.'},{t:'Completion',x:'The chronology closes where it began — at the sanctuary in Makkah, twenty-three years after the first revelation.'}],
  places:[{n:'Makkah',x:50,y:62},{n:'Mina',x:55,y:58},{n:'Arafat',x:62,y:56},{n:'Madinah',x:48,y:26}],routes:[[[50,62],[55,58],[62,56]]],
  srcs:[['Sahih Muslim — Hadith of Jabir on the Hajj','Authentic hadith — the fullest account','a'],['Musnad Ahmad','Authentic hadith collection','a']]},

 {id:'passing',y:632,hij:'11 AH',cat:'life',t:'The Passing of the Prophet ﷺ',ar:'وفاة النبي ﷺ',
  loc:'Madinah',cert:'confirmed',key:true,themes:['madinah','community','family'],
  sum:'Some three months after the Farewell Pilgrimage, the Prophet ﷺ fell ill with a fever. In his final days he asked Abu Bakr to lead the prayers, and he passed away in the apartment of Aisha, in Madinah. The community was stunned; Umar could not accept it until Abu Bakr addressed the people, reminding them that Muhammad was a messenger who had passed as messengers before him had, while the One he worshipped does not die.',
  why:'His passing closed the age of revelation and opened the question of continuity — how a community built around a living prophet would carry his message forward. Abu Bakr\'s words that day set the terms on which it would.',
  verses:[{s:'Al Imran',n:'3:144',ar:'وَمَا مُحَمَّدٌ إِلَّا رَسُولٌ قَدْ خَلَتْ مِن قَبْلِهِ الرُّسُلُ ۚ أَفَإِن مَّاتَ أَوْ قُتِلَ انقَلَبْتُمْ عَلَىٰ أَعْقَابِكُمْ',tr:'Muhammad is not but a messenger. Messengers have passed on before him. So if he was to die or be killed, would you turn back on your heels?',note:'The verse Abu Bakr recited to the grieving community, steadying it at the moment of the Prophet\'s death.'}],
  people:[{n:'Abu Bakr as-Siddiq',ar:'أبو بكر الصديق',r:'Steadied the community with the Quran and was chosen as the first successor (Khalifah).'},{n:'Aisha bint Abi Bakr',ar:'عائشة بنت أبي بكر',r:'In whose apartment he spent his final days and passed away; a foremost narrator of these events.'},{n:'Umar ibn al-Khattab',ar:'عمر بن الخطاب',r:'Overcome with grief, unable to accept the news until Abu Bakr recalled the community to the Quran.'}],
  lessons:[{t:'The Message Outlasts the Man',x:'Abu Bakr turned grief back to principle: the worship was never of a person, and the mission did not end with him.'},{t:'Continuity',x:'The care taken over succession in those hours shaped how the community would hold together afterward.'}],
  places:[{n:'Madinah',x:50,y:50},{n:'The Prophet\'s Mosque',x:52,y:48}],routes:[],
  srcs:[['Sahih al-Bukhari','Authentic hadith','a'],['Ibn Hisham, As-Sirah an-Nabawiyyah','Classical primary source','a']]}
];

/* ---------------------------------------------------------------------------
   LEARNING PATHS — curated sequences, each an ordered list of event ids.
   These drive the Explore view and can be "played" as a guided journey.
   --------------------------------------------------------------------------- */
const PATHS = [
  { id:'start-here', t:'Start Here — The Pivotal Events', badge:'Guided introduction',
    d:'New to the Seerah? Begin with the turning points that give the whole chronology its shape — from birth to the completion of the message.',
    ids:['birth','revelation','isra','hijrah','badr','hudaybiyyah','conquest','farewell','passing'] },
  { id:'meccan', t:'The Meccan Period in Seven Events', badge:'~25 min',
    d:'From the first revelation in the Cave of Hira to the night before the Hijrah — persecution, patience, and the search for refuge.',
    ids:['revelation','public','abyssinia','boycott','sorrow','isra','aqaba'] },
  { id:'hijrah-after', t:'The Hijrah and Its Aftermath', badge:'~18 min',
    d:'Why the migration happened, how it was prepared for two years in advance, and what was built in Madinah in its first years.',
    ids:['aqaba','hijrah','qibla','badr','uhud'] },
  { id:'battles', t:'The Major Battles in Sequence', badge:'~22 min',
    d:'Badr, Uhud, the Trench, and Hunayn read as one connected chain of cause and consequence rather than separate stories.',
    ids:['badr','uhud','trench','conquest','hunayn'] },
  { id:'diplomacy', t:'Diplomacy Over the Sword', badge:'~20 min',
    d:'The pledges, the constitution, and the treaty — the moments where the Seerah turned on negotiation rather than battle.',
    ids:['aqaba','hijrah','hudaybiyyah','khaybar','conquest'] },
  { id:'revelation-path', t:'Revelation Alongside Events', badge:'~35 min',
    d:'Follow the Quran chronologically — which surahs came down around which moments, and why the context changes the reading.',
    ids:['revelation','public','isra','badr','uhud','trench','hudaybiyyah','conquest','farewell'] }
];

/* ---------------------------------------------------------------------------
   GLOSSARY — terms, tribes, and places, for depth and accessibility.
   --------------------------------------------------------------------------- */
const GLOSSARY = [
  { term:'Seerah', ar:'السيرة النبوية', def:'The recorded biography of the Prophet Muhammad ﷺ — his life, character, sayings, and the events of his mission.' },
  { term:'Hijrah', ar:'الهجرة', def:'The migration from Makkah to Madinah in 622 CE. The Islamic (Hijri) calendar is dated from this year.' },
  { term:'Quraysh', ar:'قريش', def:'The dominant tribe of Makkah and custodians of the Kaaba; the Prophet ﷺ was of its clan of Banu Hashim.' },
  { term:'Ansar', ar:'الأنصار', def:'The "Helpers" — the Muslims of Madinah (of the Aws and Khazraj) who received and supported the Emigrants.' },
  { term:'Muhajirun', ar:'المهاجرون', def:'The "Emigrants" — those who migrated from Makkah to Madinah for the sake of the faith.' },
  { term:'Kaaba', ar:'الكعبة', def:'The cube-shaped sanctuary at the heart of the Sacred Mosque in Makkah, and the direction (qibla) of prayer.' },
  { term:'Companion (Sahabi)', ar:'صحابي', def:'A person who met the Prophet ﷺ as a believer and died upon faith; the plural is Sahabah.' },
  { term:'Ghazwah', ar:'غزوة', def:'An expedition or battle in which the Prophet ﷺ personally took part; the plural is Ghazawat.' },
  { term:'Aws & Khazraj', ar:'الأوس والخزرج', def:'The two principal Arab tribes of Yathrib/Madinah, long in conflict before Islam united them as the Ansar.' },
  { term:'Umrah', ar:'العمرة', def:'The "lesser pilgrimage" to Makkah, which may be performed at any time of year.' },
  { term:'Tafsir', ar:'التفسير', def:'The scholarly interpretation and exegesis of the Quran.' },
  { term:'Adhan', ar:'الأذان', def:'The call to prayer; Bilal ibn Rabah was the first to give it in Islam.' }
];

/* ---------------------------------------------------------------------------
   SEARCH SUGGESTIONS — surfaced when the search box is empty.
   --------------------------------------------------------------------------- */
const SEARCH_SUGGESTIONS = [
  'What happened right before the Hijrah?',
  'Which verses were revealed about Badr?',
  'Why did the Muslims migrate to Abyssinia?',
  'What did the Treaty of Hudaybiyyah actually achieve?',
  'Show me every battle in order',
  'Who was Khadijah?'
];

/* Expose to the app (classic-script global scope). */
window.SEERAH = { ERAS, CAT, CAT_COLOR, EVENTS, PATHS, GLOSSARY, SEARCH_SUGGESTIONS };
