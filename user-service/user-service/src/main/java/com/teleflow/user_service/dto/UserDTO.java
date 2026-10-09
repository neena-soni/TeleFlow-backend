package com.teleflow.user_service.dto;

import java.time.LocalDateTime;

public class UserDTO {
    private Long id;
    private String customerId;
    private String email;
    private String fullName;
    private String role;
    private LocalDateTime createdAt;

    public UserDTO() {}

    public UserDTO(Long id, String customerId, String email, String fullName, String role, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static UserDTOBuilder builder() { return new UserDTOBuilder(); }

    public static class UserDTOBuilder {
        private Long id;
        private String customerId;
        private String email;
        private String fullName;
        private String role;
        private LocalDateTime createdAt;

        public UserDTOBuilder id(Long id) { this.id = id; return this; }
        public UserDTOBuilder customerId(String customerId) { this.customerId = customerId; return this; }
        public UserDTOBuilder email(String email) { this.email = email; return this; }
        public UserDTOBuilder fullName(String fullName) { this.fullName = fullName; return this; }
        public UserDTOBuilder role(String role) { this.role = role; return this; }
        public UserDTOBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public UserDTO build() {
            return new UserDTO(id, customerId, email, fullName, role, createdAt);
        }
    }
}
