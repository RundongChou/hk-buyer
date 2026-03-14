package com.hkbuyer.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID_WAIT_ACCEPT,
    BUYER_PROCUREMENT,
    PROOF_UNDER_REVIEW,
    WAIT_INBOUND,
    CUSTOMS_CLEARANCE,
    IN_TRANSIT,
    SIGNED,
    CANCELLED
}
