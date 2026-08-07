package com.seerah.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The editorial gate (§4.2 actors, §6.5). The read path is open to everyone; the
 * write path is not:
 * <ul>
 *   <li>anyone may {@code GET /api/public/**} — the reader app and the world;</li>
 *   <li>an <b>editor</b> may create and edit content ({@code POST/PUT/DELETE /api/**});</li>
 *   <li>a <b>scholar</b> alone may sign off ({@code /api/review/**}) — the human gate the
 *       stale-approval check (§13.6) then enforces at publish time.</li>
 * </ul>
 *
 * <p>Phase 1 uses HTTP Basic against in-memory accounts so the platform is secure
 * and demonstrable without an external IdP. The production design (§ security phase)
 * replaces this bean with an OAuth2 resource server validating Keycloak JWTs and
 * mapping realm roles to these same authorities — the authorization rules below do
 * not change.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // stateless API; no cookies to protect
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Scholarly sign-off is a scholar's act, and only a scholar's.
                .requestMatchers("/api/review/**").hasRole("SCHOLAR")
                // Everything else that writes requires an editor.
                .requestMatchers(HttpMethod.POST, "/api/**").hasRole("EDITOR")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("EDITOR")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("EDITOR")
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * Dev accounts. NEVER used in production — replaced by Keycloak-issued identities.
     * Passwords may be overridden via SEERAH_EDITOR_PASSWORD / SEERAH_SCHOLAR_PASSWORD.
     */
    @Bean
    UserDetailsService users() {
        String editorPw = env("SEERAH_EDITOR_PASSWORD", "editor-dev");
        String scholarPw = env("SEERAH_SCHOLAR_PASSWORD", "scholar-dev");
        return new InMemoryUserDetailsManager(
            User.withUsername("editor").password("{noop}" + editorPw).roles("EDITOR").build(),
            User.withUsername("scholar").password("{noop}" + scholarPw).roles("SCHOLAR").build(),
            User.withUsername("admin").password("{noop}" + env("SEERAH_ADMIN_PASSWORD", "admin-dev"))
                    .roles("EDITOR", "SCHOLAR").build());
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
