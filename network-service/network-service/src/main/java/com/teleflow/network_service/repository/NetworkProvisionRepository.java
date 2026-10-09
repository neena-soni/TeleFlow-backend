package com.teleflow.network_service.repository;

import com.teleflow.network_service.model.NetworkProvision;
import com.teleflow.network_service.model.NetworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkProvisionRepository extends JpaRepository<NetworkProvision, Long> {

    Optional<NetworkProvision> findByTrackingId(String trackingId);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(NetworkStatus status);

    List<NetworkProvision> findTop50ByOrderByActivatedAtDesc();
}
