package com.teleflow.orchestrator_server.dto;

import com.teleflow.orchestrator_server.model.enums.StepName;
import com.teleflow.orchestrator_server.model.enums.StepStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO representing a single step's state inside an OrderResponseDTO.
 * Nested in the 'steps' list of OrderResponseDTO.
 */
@Data
@Builder
public class StepExecutionDTO {

    private Long id;
    private StepName stepName;
    private StepStatus stepStatus;
    private int retryCount;
    private String failureReason;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
