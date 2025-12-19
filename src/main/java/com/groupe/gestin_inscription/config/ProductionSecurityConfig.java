package com.groupe.gestin_inscription.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuration de sécurité spécifique à la production
 * Désactive tous les endpoints de debug et test
 */
@Configuration
@Profile("prod")
public class ProductionSecurityConfig {

    /**
     * Filtre de sécurité qui bloque tous les endpoints de debug/test en production
     */
    @Bean
    @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
    public SecurityFilterChain productionDebugFilter(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/debug/**", "/api/test/**", "/api/auth-test/**", 
                           "/api/oauth2-test/**", "/api/dev/**")
            .authorizeHttpRequests(auth -> auth
                .anyRequest().denyAll()
            );
        return http.build();
    }
}
