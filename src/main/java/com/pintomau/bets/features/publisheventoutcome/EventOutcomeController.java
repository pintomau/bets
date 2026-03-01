package com.pintomau.bets.features.publisheventoutcome;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventOutcomeController {

  private static final String TOPIC = "event-outcomes";

  private final KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate;

  public EventOutcomeController(KafkaTemplate<String, EventOutcomeMessage> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  @PostMapping("/outcome")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public void publishOutcome(@RequestBody EventOutcomeMessage message) {
    kafkaTemplate.send(TOPIC, message.eventId(), message);
  }
}
