package com.teleflow.orchestrator_server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (Cross-Origin Resource Sharing) Configuration.
 *
 * ── What is CORS? ─────────────────────────────────────────────────────────
 * Browsers block HTTP requests from one origin (e.g., localhost:3000)
 * to a different origin (e.g., localhost:8081) by default.
 * CORS headers tell the browser "yes, this server allows requests from X".
 *
 * ── Why this matters for the hackathon demo ───────────────────────────────
 * The React/Angular dashboard runs on a different port than the Spring server.
 * Without this config, every REST and WebSocket call would be blocked by the
 * browser with a CORS error — the dashboard wouldn't work at all.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "http://localhost:3000",  // React (CRA / Vite)
                    "http://localhost:4200",  // Angular
                    "http://localhost:5173"   // Vite dev server
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600); // Cache preflight for 1 hour
    }
}
