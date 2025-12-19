package com.groupe.gestin_inscription.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration HTTPS/SSL pour la production
 * Active automatiquement la redirection HTTP vers HTTPS
 */
@Configuration
@Profile("prod")
public class SSLConfig {

    @Value("${server.port:8086}")
    private int httpsPort;

    @Value("${server.http.port:8080}")
    private int httpPort;

    /**
     * Configure la redirection automatique de HTTP vers HTTPS
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return server -> {
            // Ajouter un connecteur HTTP qui redirige vers HTTPS
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(httpPort);
            connector.setSecure(false);
            connector.setRedirectPort(httpsPort);
            
            server.addAdditionalTomcatConnectors(connector);
        };
    }
}
