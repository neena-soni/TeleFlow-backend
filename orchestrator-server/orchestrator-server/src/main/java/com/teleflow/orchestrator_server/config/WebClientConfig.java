package com.teleflow.orchestrator_server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

/**
 * WebClient Configuration — one bean per downstream microservice.
 *
 * ── What is WebClient? ───────────────────────────────────────────────────
 * WebClient is Spring's non-blocking HTTP client (from Spring WebFlux).
 * Unlike RestTemplate (which blocks a thread per call), WebClient uses
 * Project Reactor's event loop — the calling thread is NOT held while
 * waiting for the HTTP response.
 *
 * Inside Temporal Activities, we use .block() to synchronously wait for
 * the response (Temporal activities are synchronous from Temporal's POV).
 * But the underlying I/O is still non-blocking at the OS level.
 *
 * ── Why separate beans? ──────────────────────────────────────────────────
 * Each downstream service has its own base URL and potentially different
 * timeout / retry settings. Named beans make injection unambiguous.
 */
@Configuration
public class WebClientConfig {

    @Value("${teleflow.services.inventory.base-url}")
    private String inventoryBaseUrl;

    @Value("${teleflow.services.network.base-url}")
    private String networkBaseUrl;

    @Value("${teleflow.services.billing.base-url}")
    private String billingBaseUrl;

    @Value("${teleflow.services.notification.base-url}")
    private String notificationBaseUrl;

    @Value("${teleflow.services.timeout-seconds}")
    private int timeoutSeconds;

    @Bean("inventoryWebClient")
    public WebClient inventoryWebClient() {
        return buildClient(inventoryBaseUrl);
    }

    @Bean("networkWebClient")
    public WebClient networkWebClient() {
        return buildClient(networkBaseUrl);
    }

    @Bean("billingWebClient")
    public WebClient billingWebClient() {
        return buildClient(billingBaseUrl);
    }

    @Bean("notificationWebClient")
    public WebClient notificationWebClient() {
        return buildClient(notificationBaseUrl);
    }

    private WebClient buildClient(String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer ->
                    // Increase response buffer size for large payloads (default: 256KB)
                    configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build();
    }
}
