package com.teleflow.billing_service.repository;

import com.teleflow.billing_service.model.BillingCharge;
import com.teleflow.billing_service.model.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingChargeRepository extends JpaRepository<BillingCharge, Long> {

    Optional<BillingCharge> findByTrackingId(String trackingId);

    boolean existsByTrackingId(String trackingId);

    long countByStatus(PaymentStatus status);

    List<BillingCharge> findTop50ByOrderByCreatedAtDesc();
}
