package com.teleflow.user_service.dto;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private String customerId;
    private String email;
    private String fullName;
    private String role;

    public AuthResponse() {}

    public AuthResponse(String token, String tokenType, String customerId, String email, String fullName, String role) {
        this.token = token;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.customerId = customerId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenType() { return tokenType; }
    public void setTokenType(String tokenType) { this.tokenType = tokenType; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public static AuthResponseBuilder builder() { return new AuthResponseBuilder(); }

    public static class AuthResponseBuilder {
        private String token;
        private String tokenType = "Bearer";
        private String customerId;
        private String email;
        private String fullName;
        private String role;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder tokenType(String tokenType) { this.tokenType = tokenType; return this; }
        public AuthResponseBuilder customerId(String customerId) { this.customerId = customerId; return this; }
        public AuthResponseBuilder email(String email) { this.email = email; return this; }
        public AuthResponseBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public AuthResponseBuilder role(String role) { this.role = role; return this; }

        public AuthResponse build() {
            return new AuthResponse(token, tokenType, customerId, email, fullName, role);
        }
    }
}
