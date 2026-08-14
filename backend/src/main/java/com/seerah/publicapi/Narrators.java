package com.seerah.publicapi;

import java.util.HashMap;
import java.util.Map;

/**
 * Canonical identities for the Companions who anchor our ḥadīth chains — the last
 * (earliest) link of an isnād. The raw Arabic carries grammatical case endings and
 * honorifics ("أبي/أبو/أبا هريرة", "عائشة أم المؤمنين") that are the same person, so
 * we normalise before counting and pair each with a transliteration for the isnād
 * network. Only Companions actually present in the corpus are mapped; an unmapped
 * name falls back to its Arabic, never a guessed identity (§ sources-only).
 */
final class Narrators {

    private Narrators() { }

    record Companion(String ar, String en) { }

    private static final Map<String, Companion> MAP = new HashMap<>();

    private static void put(String key, String ar, String en) { MAP.put(key, new Companion(ar, en)); }

    static {
        put("أبو هريرة", "أبو هريرة", "Abū Hurayra");
        put("ابن عباس", "عبد الله بن عباس", "Ibn ʿAbbās");
        put("عبد الله بن عباس", "عبد الله بن عباس", "Ibn ʿAbbās");
        put("عائشة", "عائشة", "ʿĀʾisha");
        put("البراء", "البراء بن عازب", "al-Barāʾ ibn ʿĀzib");
        put("أبو سعيد", "أبو سعيد الخدري", "Abū Saʿīd al-Khudrī");
        put("المسور بن مخرمة ومروان", "المسور بن مخرمة ومروان", "al-Miswar ibn Makhrama & Marwān");
        put("أبو ذر", "أبو ذر الغفاري", "Abū Dharr al-Ghifārī");
        put("عبد الله بن زمعة", "عبد الله بن زمعة", "ʿAbdullāh ibn Zamʿa");
        put("ابن عمر", "عبد الله بن عمر", "Ibn ʿUmar");
        put("عبد الله بن مسعود", "عبد الله بن مسعود", "Ibn Masʿūd");
        put("عبادة", "عبادة بن الصامت", "ʿUbāda ibn al-Ṣāmit");
        put("أنس", "أنس بن مالك", "Anas ibn Mālik");
        put("أنس بن مالك", "أنس بن مالك", "Anas ibn Mālik");
        put("أبو بكر", "أبو بكر الصديق", "Abū Bakr al-Ṣiddīq");
        put("سهل بن سعد", "سهل بن سعد", "Sahl ibn Saʿd");
        put("كعب بن مالك", "كعب بن مالك", "Kaʿb ibn Mālik");
        put("الحارث الأشعري", "الحارث الأشعري", "al-Ḥārith al-Ashʿarī");
        put("جابر بن عبد الله", "جابر بن عبد الله", "Jābir ibn ʿAbdullāh");
        put("مالك بن صعصعة", "مالك بن صعصعة", "Mālik ibn Ṣaʿṣaʿa");
        put("عبد الله بن عمرو بن العاص", "عبد الله بن عمرو بن العاص", "ʿAbdullāh ibn ʿAmr ibn al-ʿĀṣ");
        put("عمر بن الخطاب", "عمر بن الخطاب", "ʿUmar ibn al-Khaṭṭāb");
        put("أبو موسى", "أبو موسى الأشعري", "Abū Mūsā al-Ashʿarī");
        put("صهيب", "صهيب الرومي", "Ṣuhayb al-Rūmī");
        put("زينب بنت جحش", "زينب بنت جحش", "Zaynab bint Jaḥsh");
    }

    /** Resolve the last isnād link to a canonical Companion, or fall back to its Arabic. */
    static Companion of(String lastLink) {
        if (lastLink == null) return new Companion("—", "");
        Companion c = MAP.get(normalize(lastLink));
        return c != null ? c : new Companion(lastLink.trim(), "");
    }

    /** Fold the grammatical case of the kunya and strip honorifics so variants unify. */
    private static String normalize(String s) {
        String n = s.trim().replace(" أم المؤمنين", "").trim();
        if (n.startsWith("أبي ") || n.startsWith("أبا ")) n = "أبو " + n.substring(4);
        return n;
    }
}
