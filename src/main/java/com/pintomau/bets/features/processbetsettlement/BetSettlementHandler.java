package com.pintomau.bets.features.processbetsettlement;

import com.pintomau.bets.features.matchandqueuesettlements.BetSettlementMessage;
import com.pintomau.bets.infrastructure.messaging.RabbitMQConfig;
import com.pintomau.bets.shared.domain.BetRepository;
import com.pintomau.bets.shared.domain.BetResult;
import com.pintomau.bets.shared.domain.BetStatus;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BetSettlementHandler {

  private static final Logger log = LoggerFactory.getLogger(BetSettlementHandler.class);

  private final BetRepository betRepository;

  public BetSettlementHandler(BetRepository betRepository) {
    this.betRepository = betRepository;
  }

  @RabbitListener(queues = RabbitMQConfig.BET_SETTLEMENTS_QUEUE)
  @Transactional
  public void handle(BetSettlementMessage message) {
    BetStatus newStatus = message.result() == BetResult.WIN ? BetStatus.WON : BetStatus.LOST;

    int updated = betRepository.settleIfPending(message.betId(), newStatus, Instant.now());

    if (updated == 1) {
      log.info("Settled bet {} as {}", message.betId(), newStatus);
    } else {
      log.info("Bet {} already settled or not found (idempotent)", message.betId());
    }
  }
}
