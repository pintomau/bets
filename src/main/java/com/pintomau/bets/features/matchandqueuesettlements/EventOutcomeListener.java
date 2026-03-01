package com.pintomau.bets.features.matchandqueuesettlements;

import com.pintomau.bets.features.publisheventoutcome.EventOutcomeMessage;
import com.pintomau.bets.infrastructure.messaging.RabbitMQConfig;
import com.pintomau.bets.shared.domain.Bet;
import com.pintomau.bets.shared.domain.BetRepository;
import com.pintomau.bets.shared.domain.BetResult;
import com.pintomau.bets.shared.domain.BetStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EventOutcomeListener {

  private static final Logger log = LoggerFactory.getLogger(EventOutcomeListener.class);

  private final BetRepository betRepository;
  private final RabbitTemplate rabbitTemplate;

  public EventOutcomeListener(BetRepository betRepository, RabbitTemplate rabbitTemplate) {
    this.betRepository = betRepository;
    this.rabbitTemplate = rabbitTemplate;
  }

  @KafkaListener(topics = "event-outcomes", groupId = "bets-consumer-group")
  public void onEventOutcome(EventOutcomeMessage outcome) {
    log.info(
        "Received event outcome: eventId={}, winner={}",
        outcome.eventId(),
        outcome.eventWinnerId());

    List<Bet> pendingBets =
        betRepository.findByEventIdAndStatus(outcome.eventId(), BetStatus.PENDING);

    log.info("Found {} pending bets for event {}", pendingBets.size(), outcome.eventId());

    pendingBets.forEach(
        bet -> {
          BetResult result =
              bet.getEventWinnerId().equals(outcome.eventWinnerId())
                  ? BetResult.WIN
                  : BetResult.LOSS;
          BetSettlementMessage message =
              new BetSettlementMessage(
                  bet.getId(), bet.getUserId(), bet.getEventId(), bet.getBetAmount(), result);

          rabbitTemplate.convertAndSend(
              RabbitMQConfig.BET_SETTLEMENTS_EXCHANGE,
              RabbitMQConfig.BET_SETTLEMENTS_ROUTING_KEY,
              message);
        });
  }
}
