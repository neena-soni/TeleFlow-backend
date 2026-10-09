package com.teleflow.user_service.controller;

import com.teleflow.user_service.dto.PlaceOrderRequest;
import com.teleflow.user_service.dto.UserDTO;
import com.teleflow.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(required = false) String identifier) {

        String userKey = userIdHeader != null && !userIdHeader.isEmpty() ? userIdHeader : identifier;
        if (userKey == null || userKey.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(userService.getProfile(userKey));
    }

    @PostMapping("/orders")
    public ResponseEntity<Object> placeOrder(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-001") String customerId,
            @Valid @RequestBody PlaceOrderRequest request) {

        Object response = userService.placeOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders")
    public ResponseEntity<Object> getUserOrders(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "CUST-001") String customerId) {

        return ResponseEntity.ok(userService.getUserOrders(customerId));
    }
}
