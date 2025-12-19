package com.groupe.gestin_inscription.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration du rate limiting pour protéger les endpoints publics
 * Utilise l'algorithme Token Bucket pour limiter le nombre de requêtes
 */
@Configuration
public class RateLimitingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingConfig.class);

    /**
     * Filtre de rate limiting pour les endpoints publics
     */
    @Bean
    public Filter rateLimitingFilter() {
        return new RateLimitingFilter();
    }

    /**
     * Filtre qui applique le rate limiting basé sur l'IP
     */
    public static class RateLimitingFilter implements Filter {
        
        // Cache des buckets par IP
        private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
        
        // Endpoints publics à protéger
        private static final String[] RATE_LIMITED_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/users",
            "/oauth2/authorization"
        };

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String requestURI = httpRequest.getRequestURI();
            
            // Vérifier si l'endpoint nécessite un rate limiting
            if (shouldApplyRateLimit(requestURI)) {
                String clientIp = getClientIP(httpRequest);
                Bucket bucket = resolveBucket(clientIp);
                
                if (bucket.tryConsume(1)) {
                    // Requête autorisée
                    chain.doFilter(request, response);
                } else {
                    // Limite dépassée
                    logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestURI);
                    httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(
                        "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}"
                    );
                }
            } else {
                chain.doFilter(request, response);
            }
        }

        /**
         * Vérifie si l'endpoint nécessite un rate limiting
         */
        private boolean shouldApplyRateLimit(String requestURI) {
            for (String endpoint : RATE_LIMITED_ENDPOINTS) {
                if (requestURI.startsWith(endpoint)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Résout ou crée un bucket pour une IP donnée
         * Limite: 20 requêtes par minute
         */
        private Bucket resolveBucket(String clientIp) {
            return cache.computeIfAbsent(clientIp, k -> createNewBucket());
        }

        /**
         * Crée un nouveau bucket avec les limites configurées
         * 20 requêtes par minute avec refill de 20 tokens par minute
         */
        private Bucket createNewBucket() {
            Bandwidth limit = Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
            return Bucket.builder()
                .addLimit(limit)
                .build();
        }

        /**
         * Récupère l'IP du client en tenant compte des proxies
         */
        private String getClientIP(HttpServletRequest request) {
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader == null) {
                return request.getRemoteAddr();
            }
            return xfHeader.split(",")[0];
        }
    }
}
