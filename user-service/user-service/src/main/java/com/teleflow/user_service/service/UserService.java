package com.teleflow.user_service.service;

import com.teleflow.user_service.dto.*;

import java.util.List;

public interface UserService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    UserDTO getProfile(String identifier);
    Object placeOrder(String customerId, PlaceOrderRequest request);
    Object getUserOrders(String customerId);
}
