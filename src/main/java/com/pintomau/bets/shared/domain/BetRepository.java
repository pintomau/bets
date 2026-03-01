package com.pintomau.bets.shared.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BetRepository extends JpaRepository<Bet, UUID> {

  List<Bet> findByEventIdAndStatus(String eventId, BetStatus status);

  @Modifying
  @Query(
      "UPDATE Bet b SET b.status = :status, b.settledAt = :settledAt "
          + "WHERE b.id = :id AND b.status = 'PENDING'")
  int settleIfPending(
      @Param("id") UUID id,
      @Param("status") BetStatus status,
      @Param("settledAt") Instant settledAt);
}
