package com.teleflow.network_service.service;

import com.teleflow.network_service.dto.NetworkActivateRequest;
import com.teleflow.network_service.dto.NetworkActivateResponse;
import com.teleflow.network_service.dto.NetworkDeactivateRequest;
import com.teleflow.network_service.dto.NetworkStatsDTO;
import com.teleflow.network_service.model.NetworkProvision;
import com.teleflow.network_service.model.NetworkStatus;
import com.teleflow.network_service.repository.NetworkProvisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkService {

    private final NetworkProvisionRepository repository;

    @Transactional
    public NetworkActivateResponse activate(NetworkActivateRequest request) {
        log.info("[NETWORK] Processing activate request: trackingId={}, customerId={}, plan={}",
                request.getTrackingId(), request.getCustomerId(), request.getPlanName());

        // ① Chaos Mode / Failure Injection Hook for Hackathon Testing
        if ((request.getPlanName() != null && request.getPlanName().toUpperCase().contains("CHAOS_FAIL_NET"))
                || (request.getCustomerId() != null && request.getCustomerId().toUpperCase().startsWith("CHAOS_NET"))) {
            log.error("[NETWORK-CHAOS] Simulated network slice activation failure for trackingId={}", request.getTrackingId());
            throw new RuntimeException("Simulated Failure: Optical/5G slice activation failed at sector controller.");
        }

        // ② Idempotency Check: Already provisioned?
        Optional<NetworkProvision> existing = repository.findByTrackingId(request.getTrackingId());
        if (existing.isPresent()) {
            NetworkProvision np = existing.get();
            log.info("[NETWORK] Existing provision found for trackingId={}, status={}",
                    request.getTrackingId(), np.getStatus());
            return NetworkActivateResponse.builder()
                    .trackingId(np.getTrackingId())
                    .vlanTag(np.getVlanTag())
                    .allocatedBandwidthMbps(np.getAllocatedBandwidthMbps())
                    .status(np.getStatus().name())
                    .build();
        }

        // ③ Determine Bandwidth profile
        int bandwidth;
        String planUpper = request.getPlanName() != null ? request.getPlanName().toUpperCase() : "";
        if (planUpper.contains("5G_UNLIMITED")) {
            bandwidth = 1000;
        } else if (planUpper.contains("FIBER_300MBPS")) {
            bandwidth = 300;
        } else if (planUpper.contains("5G_BASIC")) {
            bandwidth = 250;
        } else if (planUpper.contains("FIBER_100MBPS")) {
            bandwidth = 100;
        } else {
            bandwidth = 100;
        }

        // ④ Allocate VLAN & Private IP
        int vlanNum = Math.abs(request.getTrackingId().hashCode() % 900) + 100;
        String vlanTag = "VLAN-" + vlanNum;
        int ipOctet3 = Math.abs(request.getTrackingId().hashCode() % 250) + 1;
        int ipOctet4 = Math.abs(request.getCustomerId().hashCode() % 250) + 1;
        String allocatedIp = "10.240." + ipOctet3 + "." + ipOctet4;

        // ⑤ Persist Network Provisioning
        NetworkProvision provision = NetworkProvision.builder()
                .trackingId(request.getTrackingId())
                .customerId(request.getCustomerId())
                .planName(request.getPlanName())
                .portId(request.getPortId() != null ? request.getPortId() : "PORT-AUTO-01")
                .routerId(request.getRouterId() != null ? request.getRouterId() : "RTR-CORE-01")
                .vlanTag(vlanTag)
                .allocatedBandwidthMbps(bandwidth)
                .allocatedIp(allocatedIp)
                .status(NetworkStatus.ACTIVE)
                .activatedAt(LocalDateTime.now())
                .build();

        provision = repository.save(provision);
        log.info("[NETWORK] Provisioned VLAN={} BW={}Mbps IP={} for trackingId={}",
                vlanTag, bandwidth, allocatedIp, request.getTrackingId());

        return NetworkActivateResponse.builder()
                .trackingId(provision.getTrackingId())
                .vlanTag(provision.getVlanTag())
                .allocatedBandwidthMbps(provision.getAllocatedBandwidthMbps())
                .status(provision.getStatus().name())
                .build();
    }

    @Transactional
    public void deactivate(NetworkDeactivateRequest request) {
        log.info("[NETWORK-COMPENSATION] Deactivating network provision for trackingId={}", request.getTrackingId());
        Optional<NetworkProvision> opt = repository.findByTrackingId(request.getTrackingId());
        if (opt.isPresent()) {
            NetworkProvision np = opt.get();
            np.setStatus(NetworkStatus.DEACTIVATED);
            np.setDeactivatedAt(LocalDateTime.now());
            repository.save(np);
            log.info("[NETWORK-COMPENSATION] VLAN {} deactivated successfully for trackingId={}",
                    np.getVlanTag(), request.getTrackingId());
        } else {
            log.warn("[NETWORK-COMPENSATION] No provision found for trackingId={}", request.getTrackingId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<NetworkProvision> getProvision(String trackingId) {
        return repository.findByTrackingId(trackingId);
    }

    @Transactional(readOnly = true)
    public List<NetworkProvision> getAllProvisions() {
        return repository.findTop50ByOrderByActivatedAtDesc();
    }

    @Transactional(readOnly = true)
    public NetworkStatsDTO getStats() {
        long total = repository.count();
        long active = repository.countByStatus(NetworkStatus.ACTIVE);
        long deactivated = repository.countByStatus(NetworkStatus.DEACTIVATED);

        return NetworkStatsDTO.builder()
                .totalProvisions(total)
                .activeLines(active)
                .deactivatedLines(deactivated)
                .totalAllocatedBandwidthMbps(active * 300) // average estimate
                .status("HEALTHY")
                .build();
    }
}
