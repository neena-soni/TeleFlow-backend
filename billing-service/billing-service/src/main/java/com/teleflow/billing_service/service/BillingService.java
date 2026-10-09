package com.teleflow.billing_service.service;

import com.teleflow.billing_service.dto.BillingChargeRequest;
import com.teleflow.billing_service.dto.BillingChargeResponse;
import com.teleflow.billing_service.dto.BillingRefundRequest;
import com.teleflow.billing_service.dto.BillingStatsDTO;
import com.teleflow.billing_service.model.BillingCharge;
import com.teleflow.billing_service.model.PaymentStatus;
import com.teleflow.billing_service.repository.BillingChargeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingChargeRepository repository;
    private final AtomicBoolean chaosMode = new AtomicBoolean(false);

    public boolean toggleChaosMode() {
        boolean newState = !chaosMode.get();
        chaosMode.set(newState);
        log.warn("[BILLING-CHAOS] Global chaos mode toggled to: {}", newState);
        return newState;
    }

    public boolean isChaosMode() {
        return chaosMode.get();
    }

    @Transactional
    public BillingChargeResponse charge(BillingChargeRequest request) {
        log.info("[BILLING] Processing charge request: trackingId={}, customerId={}, amount=₹{}",
                request.getTrackingId(), request.getCustomerId(), request.getAmount());

        // ① Chaos Mode / Simulated Failure Injection
        if (chaosMode.get()
                || (request.getPlanName() != null && request.getPlanName().toUpperCase().contains("CHAOS_FAIL_BILL"))
                || (request.getCustomerId() != null && request.getCustomerId().toUpperCase().startsWith("CHAOS_BILL"))) {
            log.error("[BILLING-CHAOS] Simulating 500 Payment Gateway Timeout for trackingId={}", request.getTrackingId());
            throw new RuntimeException("Simulated Failure (Chaos Mode): Bank Payment Gateway Timeout [HTTP 500]. Downstream banking partner unreachable.");
        }

        // ② Idempotency Check: Already charged?
        Optional<BillingCharge> existing = repository.findByTrackingId(request.getTrackingId());
        if (existing.isPresent()) {
            BillingCharge bc = existing.get();
            log.info("[BILLING] Existing charge found for trackingId={}, status={}",
                    request.getTrackingId(), bc.getStatus());
            return BillingChargeResponse.builder()
                    .trackingId(bc.getTrackingId())
                    .paymentId(bc.getPaymentId())
                    .amount(bc.getAmount())
                    .status(bc.getStatus().name())
                    .build();
        }

        // ③ Generate Payment Transaction ID
        long txnNum = Math.abs(System.currentTimeMillis() % 900000L) + 100000L;
        String paymentId = "PAY-TXN-" + txnNum;

        // ④ Save Record
        BillingCharge charge = BillingCharge.builder()
                .trackingId(request.getTrackingId())
                .customerId(request.getCustomerId())
                .planName(request.getPlanName())
                .amount(request.getAmount())
                .paymentId(paymentId)
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();

        charge = repository.save(charge);
        log.info("[BILLING] Payment successful: paymentId={} amount=₹{} for trackingId={}",
                paymentId, request.getAmount(), request.getTrackingId());

        return BillingChargeResponse.builder()
                .trackingId(charge.getTrackingId())
                .paymentId(charge.getPaymentId())
                .amount(charge.getAmount())
                .status(charge.getStatus().name())
                .build();
    }

    @Transactional
    public void refund(BillingRefundRequest request) {
        log.info("[BILLING-COMPENSATION] Initiating refund for trackingId={}", request.getTrackingId());
        Optional<BillingCharge> opt = repository.findByTrackingId(request.getTrackingId());
        if (opt.isPresent()) {
            BillingCharge bc = opt.get();
            bc.setStatus(PaymentStatus.REFUNDED);
            bc.setRefundedAt(LocalDateTime.now());
            repository.save(bc);
            log.info("[BILLING-COMPENSATION] Refunded ₹{} for paymentId={} trackingId={}",
                    bc.getAmount(), bc.getPaymentId(), request.getTrackingId());
        } else {
            log.warn("[BILLING-COMPENSATION] No charge record found to refund for trackingId={}",
                    request.getTrackingId());
        }
    }

    @Transactional(readOnly = true)
    public Optional<BillingCharge> getCharge(String trackingId) {
        return repository.findByTrackingId(trackingId);
    }

    @Transactional(readOnly = true)
    public List<BillingCharge> getAllCharges() {
        return repository.findTop50ByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public BillingStatsDTO getStats() {
        long total = repository.count();
        long success = repository.countByStatus(PaymentStatus.SUCCESS);
        long refunded = repository.countByStatus(PaymentStatus.REFUNDED);

        List<BillingCharge> charges = repository.findAll();
        BigDecimal totalRev = charges.stream()
                .filter(c -> c.getStatus() == PaymentStatus.SUCCESS)
                .map(BillingCharge::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return BillingStatsDTO.builder()
                .totalTransactions(total)
                .successfulCharges(success)
                .refundedCharges(refunded)
                .totalRevenueCollected(totalRev)
                .chaosModeActive(chaosMode.get())
                .status("HEALTHY")
                .build();
    }
}
