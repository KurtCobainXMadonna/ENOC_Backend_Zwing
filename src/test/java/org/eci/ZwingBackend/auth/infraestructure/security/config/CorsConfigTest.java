package org.eci.ZwingBackend.auth.infraestructure.security.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void buildsCorsConfigurationFromCsv() throws Exception {
        CorsConfig config = new CorsConfig();
        Field field = CorsConfig.class.getDeclaredField("allowedOriginsCsv");
        field.setAccessible(true);
        field.set(config, "http://localhost:3000, https://app.example.com , ");

        var source = config.corsConfigurationSource();
        var cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/test"));

        assertThat(cors.getAllowedOrigins()).containsExactly("http://localhost:3000", "https://app.example.com");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        assertThat(cors.getAllowedHeaders()).contains("Authorization", "Content-Type", "X-Requested-With", "Accept", "Cookie");
        assertThat(cors.getAllowCredentials()).isTrue();
    }
}