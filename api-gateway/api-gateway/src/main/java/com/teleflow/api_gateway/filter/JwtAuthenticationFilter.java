package com.teleflow.api_gateway.filter;

import com.teleflow.api_gateway.exception.UnauthorizedHandler;
import com.teleflow.api_gateway.util.JwtUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final JwtUtils jwtUtils;
    private final UnauthorizedHandler unauthorizedHandler;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UnauthorizedHandler unauthorizedHandler) {
        super(Config.class);
        this.jwtUtils = jwtUtils;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || authHeader.trim().isEmpty()) {
                return unauthorizedHandler.writeErrorResponse(exchange, "Missing Authorization header");
            }

            if (!authHeader.startsWith("Bearer ")) {
                return unauthorizedHandler.writeErrorResponse(exchange, "Invalid Authorization header format");
            }

            String token = authHeader.substring(7);

            if (!jwtUtils.validateToken(token)) {
                return unauthorizedHandler.writeErrorResponse(exchange, "Invalid or expired JWT token");
            }

            String username = jwtUtils.extractUsername(token);
            List<String> roles = jwtUtils.extractRoles(token);
            String rolesStr = String.join(",", roles);

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", username != null ? username : "")
                    .header("X-User-Roles", rolesStr)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
