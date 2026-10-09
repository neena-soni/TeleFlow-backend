package com.teleflow.network_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "network_provisions", indexes = {
    @Index(name = "idx_net_tracking_id", columnList = "trackingId"),
    @Index(name = "idx_net_vlan_tag", columnList = "vlanTag")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkProvision {

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

    @Column(nullable = false, length = 32)
    private String vlanTag;

    @Column(nullable = false)
    private Integer allocatedBandwidthMbps;

    @Column(length = 64)
    private String allocatedIp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NetworkStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    private LocalDateTime deactivatedAt;
}
