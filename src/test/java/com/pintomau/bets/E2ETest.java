package com.pintomau.bets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.pintomau.bets.shared.domain.Bet;
import com.pintomau.bets.shared.domain.BetRepository;
import com.pintomau.bets.shared.domain.BetStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestRestTemplate
class E2ETest {

  @Container @ServiceConnection
  static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.2.0");

  @Container @ServiceConnection
  static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:4.2.4-management-alpine");

  @Autowired TestRestTemplate restTemplate;

  @Autowired BetRepository betRepository;

  @Test
  void settlementFlow_teamAWins_correctBetsSettled() {
    // Use unique event ID to avoid conflicts with seeded data
    String testEventId = "test-event-" + UUID.randomUUID();

    // Create test bets
    Bet betOnTeamA1 =
        betRepository.save(
            new Bet(
                UUID.randomUUID(), testEventId, "market-001", "team-a", new BigDecimal("100.00")));

    Bet betOnTeamB =
        betRepository.save(
            new Bet(
                UUID.randomUUID(), testEventId, "market-001", "team-b", new BigDecimal("50.00")));

    Bet betOnTeamA2 =
        betRepository.save(
            new Bet(
                UUID.randomUUID(), testEventId, "market-001", "team-a", new BigDecimal("75.00")));

    // When: publish event outcome with team-a as winner
    var outcome = new EventOutcomeRequest(testEventId, "Test Match", "team-a");
    var response = restTemplate.postForEntity("/api/events/outcome", outcome, Void.class);

    // Then: response is 202 Accepted
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

    // And: bets are settled correctly (async, so we wait)
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var bet1 = betRepository.findById(betOnTeamA1.getId());
              var bet2 = betRepository.findById(betOnTeamB.getId());
              var bet3 = betRepository.findById(betOnTeamA2.getId());

              assertThat(bet1)
                  .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BetStatus.WON));
              assertThat(bet2)
                  .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BetStatus.LOST));
              assertThat(bet3)
                  .hasValueSatisfying(b -> assertThat(b.getStatus()).isEqualTo(BetStatus.WON));
            });
  }

  record EventOutcomeRequest(String eventId, String eventName, String eventWinnerId) {}
}
