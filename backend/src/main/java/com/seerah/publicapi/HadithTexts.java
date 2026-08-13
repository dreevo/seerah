package com.seerah.publicapi;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The literal Arabic + English text of every ḥadīth the corpus cites, bundled at
 * {@code seed/hadith/texts.json} (fetched once from the open hadith dataset, whose
 * numbering matches sunnah.com for al-Bukhārī/Tirmidhī; Muslim was matched by
 * content). Keyed "collection:number". The BFF attaches the text to a citation so
 * a ḥadīth can be shown in full, exactly like a Qur'anic verse — not just cited.
 * When present, {@code chain} carries the isnād's narrators (mechanically extracted
 * from the Arabic isnād, collector-ward first → Companion last) — only bundled when
 * the chain provably ends in a known Companion, so a shown chain is always sound.
 */
@Component
public class HadithTexts {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String en, String ar, List<String> chain) { }

    private static final Pattern NUM = Pattern.compile("no\\.\\s*(\\d+)");
    private Map<String, Entry> texts = Map.of();

    @PostConstruct
    void load() throws Exception {
        try (var in = new ClassPathResource("seed/hadith/texts.json").getInputStream()) {
            texts = new ObjectMapper().readValue(in, new TypeReference<>() { });
        }
    }

    /** The bundled text (+ isnād chain) for a citation, or null if it is not a bundled ḥadīth. */
    public Entry lookup(String workTitle, String locator) {
        String key = key(workTitle, locator);
        return key == null ? null : texts.get(key);
    }

    /** The "collection:number" key for a citation (e.g. "bukhari:3374"), or null if not a bundled ḥadīth. */
    public String key(String workTitle, String locator) {
        String coll = collection(workTitle);
        if (coll == null || locator == null) return null;
        Matcher m = NUM.matcher(locator);
        return m.find() ? coll + ":" + m.group(1) : null;
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
