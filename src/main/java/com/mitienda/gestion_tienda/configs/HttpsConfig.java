package com.mitienda.gestion_tienda.configs;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnResource(resources = "classpath:keystore.p12")
public class HttpsConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        return new TomcatServletWebServerFactory();
    }
}