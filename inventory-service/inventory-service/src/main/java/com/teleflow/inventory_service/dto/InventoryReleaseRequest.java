package com.teleflow.inventory_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleaseRequest {

    @NotBlank(message = "trackingId is required")
    private String trackingId;
}
