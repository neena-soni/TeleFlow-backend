package com.teleflow.inventory_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_reservations", indexes = {
    @Index(name = "idx_inv_tracking_id", columnList = "trackingId"),
    @Index(name = "idx_inv_port_id", columnList = "portId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String trackingId;

    @Column(nullable = false, length = 64)
    private String customerId;

    @Column(nullable = false, length = 100)
    private String planName;

    @Column(nullable = false, length = 64)
    private String portId;

    @Column(nullable = false, length = 64)
    private String routerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReservationStatus status;

    @Column(length = 64)
    private String portType; // e.g. "5G_NR_CELL", "FIBER_GPON", "BROADBAND_VDSL"

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime releasedAt;
}
