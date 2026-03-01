package com.pintomau.bets.shared.domain;

import com.pintomau.bets.infrastructure.hibernate.GeneratedUuidV7;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bets")
public class Bet {

  @Id @GeneratedUuidV7 private UUID id;

  private UUID userId;

  private String eventId;

  private String eventMarketId;

  private String eventWinnerId;

  private BigDecimal betAmount;

  @Enumerated(EnumType.STRING)
  private BetStatus status = BetStatus.PENDING;

  private Instant settledAt;

  protected Bet() {}

  public Bet(
      UUID userId,
      String eventId,
      String eventMarketId,
      String eventWinnerId,
      BigDecimal betAmount) {
    this.userId = userId;
    this.eventId = eventId;
    this.eventMarketId = eventMarketId;
    this.eventWinnerId = eventWinnerId;
    this.betAmount = betAmount;
    this.status = BetStatus.PENDING;
  }

  public UUID getId() {
    return id;
  }

  public UUID getUserId() {
    return userId;
  }

  public String getEventId() {
    return eventId;
  }

  public String getEventMarketId() {
    return eventMarketId;
  }

  public String getEventWinnerId() {
    return eventWinnerId;
  }

  public BigDecimal getBetAmount() {
    return betAmount;
  }

  public BetStatus getStatus() {
    return status;
  }

  public Instant getSettledAt() {
    return settledAt;
  }

  public void settle(BetStatus status) {
    if (this.status != BetStatus.PENDING) {
      throw new IllegalStateException("Bet already settled");
    }
    this.status = status;
    this.settledAt = Instant.now();
  }
}
