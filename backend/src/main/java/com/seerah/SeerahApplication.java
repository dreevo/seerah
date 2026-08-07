package com.seerah;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single deployable. One Spring Boot process, bounded contexts as packages
 * beneath {@code com.seerah}, boundaries enforced by the ArchUnit suite rather
 * than by build modules (§21 — the modular monolith).
 */
@SpringBootApplication
public class SeerahApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeerahApplication.class, args);
    }
}
