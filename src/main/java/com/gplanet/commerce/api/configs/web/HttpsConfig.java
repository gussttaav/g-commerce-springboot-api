package com.gplanet.commerce.api.configs.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for HTTPS support.
 * Only activates when a keystore.p12 file exists in the classpath.
 * 
 * @author Gustavo
 * @version 1.0
 */
@Configuration
@ConditionalOnResource(resources = "classpath:keystore.p12")
public class HttpsConfig {

    /**
     * Configures and provides the Tomcat web server factory.
     * 
     * @return ServletWebServerFactory configured for Tomcat
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        return new TomcatServletWebServerFactory();
    }
}
