package com.teleflow.user_service.service;

import com.teleflow.user_service.dto.*;
import com.teleflow.user_service.model.Role;
import com.teleflow.user_service.model.User;
import com.teleflow.user_service.repository.UserRepository;
import com.teleflow.user_service.util.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final WebClient orchestratorWebClient;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider, WebClient orchestratorWebClient) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.orchestratorWebClient = orchestratorWebClient;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered: " + request.getEmail());
        }

        String generatedCustomerId = "CUST-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        User user = User.builder()
                .customerId(generatedCustomerId)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.ROLE_USER)
                .build();

        userRepository.save(user);
        log.info("Registered new user: customerId={} email={}", generatedCustomerId, user.getEmail());

        String token = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        log.info("User logged in successfully: customerId={}", user.getCustomerId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public UserDTO getProfile(String identifier) {
        User user = userRepository.findByCustomerId(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + identifier)));

        return UserDTO.builder()
                .id(user.getId())
                .customerId(user.getCustomerId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public Object placeOrder(String customerId, PlaceOrderRequest request) {
        User user = userRepository.findByCustomerId(customerId)
                .orElseGet(() -> userRepository.findByEmail(customerId)
                        .orElse(null));

        String email = user != null ? user.getEmail() : customerId + "@example.com";

        log.info("User placing order via orchestrator: customerId={} plan={}", customerId, request.getPlanName());

        Map<String, Object> payload = Map.of(
                "customerId", customerId,
                "planName", request.getPlanName(),
                "customerEmail", email
        );

        return orchestratorWebClient.post()
                .uri("/api/orders/activate")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }

    @Override
    public Object getUserOrders(String customerId) {
        log.info("Fetching user orders from orchestrator: customerId={}", customerId);
        return orchestratorWebClient.get()
                .uri("/api/orders")
                .retrieve()
                .bodyToMono(Object.class)
                .block();
    }
}
