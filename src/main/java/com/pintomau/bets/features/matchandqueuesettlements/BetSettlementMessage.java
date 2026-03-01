package com.pintomau.bets.features.matchandqueuesettlements;

import com.pintomau.bets.shared.domain.BetResult;
import java.math.BigDecimal;
import java.util.UUID;

public record BetSettlementMessage(
    UUID betId, UUID userId, String eventId, BigDecimal betAmount, BetResult result) {}
