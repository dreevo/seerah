package com.seerah.publicapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The literal Arabic + English text of every ḥadīth the corpus cites, bundled at
 * {@code seed/hadith/texts.json} (fetched once from the open hadith dataset, whose
 * numbering matches sunnah.com for al-Bukhārī/Tirmidhī; Muslim was matched by
 * content). Keyed "collection:number". The BFF attaches the text to a citation so
 * a ḥadīth can be shown in full, exactly like a Qur'anic verse — not just cited.
 */
@Component
public class HadithTexts {

    private static final Pattern NUM = Pattern.compile("no\\.\\s*(\\d+)");
    private Map<String, Map<String, String>> texts = Map.of();

    @PostConstruct
    void load() throws Exception {
        try (var in = new ClassPathResource("seed/hadith/texts.json").getInputStream()) {
            texts = new ObjectMapper().readValue(in, new TypeReference<>() { });
        }
    }

    /** {@code [english, arabic]} for a citation, or null if it is not a bundled ḥadīth. */
    public String[] lookup(String workTitle, String locator) {
        String coll = collection(workTitle);
        if (coll == null || locator == null) return null;
        Matcher m = NUM.matcher(locator);
        if (!m.find()) return null;
        Map<String, String> t = texts.get(coll + ":" + m.group(1));
        return t == null ? null : new String[] { t.get("en"), t.get("ar") };
    }

    private static String collection(String workTitle) {
        if (workTitle == null) return null;
        String w = workTitle.toLowerCase();
        if (w.contains("bukh")) return "bukhari";
        if (w.contains("muslim")) return "muslim";
        if (w.contains("tirmidh")) return "tirmidhi";
        return null;
    }
}
