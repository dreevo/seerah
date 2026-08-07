package com.seerah;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The boundaries the book draws in prose, enforced as tests (§22 — boundary
 * enforcement). These are the rules that keep an 11-module monolith from
 * quietly collapsing into a big ball of mud.
 */
@AnalyzeClasses(packages = "com.seerah", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /** §23.1 — the domain is framework-free: no Spring, no JPA, no Hibernate. */
    @ArchTest
    static final ArchRule domain_is_pure =
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..");

    /** §23.1 — the dependency rule: application never reaches out to adapters. */
    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter..");

    /** §23.1 — application talks to the database only through ports, never JPA. */
    @ArchTest
    static final ArchRule application_does_not_use_jpa =
            noClasses().that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    /** §23.4 — the web ring depends on ports, not on persistence adapters. */
    @ArchTest
    static final ArchRule web_does_not_touch_persistence =
            noClasses().that().resideInAPackage("..adapter.in.web..")
                    .should().dependOnClassesThat().resideInAPackage("..adapter.out.persistence..");

    /** §6.8.2 — content may reach provenance only through provenance.api. */
    @ArchTest
    static final ArchRule content_uses_provenance_only_via_api =
            noClasses().that().resideInAPackage("com.seerah.content..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.seerah.provenance.domain..",
                            "com.seerah.provenance.application..",
                            "com.seerah.provenance.adapter..");

    /** §6.8.2 — provenance is a supplier; it must not depend on content at all. */
    @ArchTest
    static final ArchRule provenance_does_not_depend_on_content =
            noClasses().that().resideInAPackage("com.seerah.provenance..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.content..");

    /** §6.8.2 — nobody outside provenance may reach past provenance.api into its internals. */
    @ArchTest
    static final ArchRule provenance_internals_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.provenance..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "com.seerah.provenance.domain..",
                            "com.seerah.provenance.application..",
                            "com.seerah.provenance.adapter..");

    /** §23.1 — no module's persistence/web adapters are reachable from another module. */
    @ArchTest
    static final ArchRule people_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.people..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.people.adapter..");

    @ArchTest
    static final ArchRule scripture_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.scripture..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.scripture.adapter..");

    @ArchTest
    static final ArchRule content_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.content..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.content.adapter..");

    @ArchTest
    static final ArchRule places_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.places..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.places.adapter..");

    @ArchTest
    static final ArchRule search_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.search..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.search.adapter..");

    @ArchTest
    static final ArchRule review_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.review..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.review.adapter..");

    @ArchTest
    static final ArchRule media_adapters_are_private =
            noClasses().that().resideOutsideOfPackage("com.seerah.media..")
                    .should().dependOnClassesThat().resideInAPackage("com.seerah.media.adapter..");

    /** §5.7 — the assistant retrieves and never asserts: it has no store of its own. */
    @ArchTest
    static final ArchRule assistant_has_no_persistence =
            noClasses().that().resideInAPackage("com.seerah.assistant..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "jakarta.persistence..",
                            "org.springframework.data..");
}
