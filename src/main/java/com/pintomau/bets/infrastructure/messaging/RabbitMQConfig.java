package com.pintomau.bets.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RabbitMQConfig {

  public static final String BET_SETTLEMENTS_EXCHANGE = "bet.settlements";
  public static final String BET_SETTLEMENTS_QUEUE = "bet-settlements-queue";
  public static final String BET_SETTLEMENTS_ROUTING_KEY = "bet.settlement";

  @Bean
  public DirectExchange betSettlementsExchange() {
    return ExchangeBuilder.directExchange(BET_SETTLEMENTS_EXCHANGE).durable(true).build();
  }

  @Bean
  public Queue betSettlementsQueue() {
    return QueueBuilder.durable(BET_SETTLEMENTS_QUEUE).build();
  }

  @Bean
  public Binding betSettlementsBinding(
      Queue betSettlementsQueue, DirectExchange betSettlementsExchange) {
    return BindingBuilder.bind(betSettlementsQueue)
        .to(betSettlementsExchange)
        .with(BET_SETTLEMENTS_ROUTING_KEY);
  }

  @Bean
  public MessageConverter messageConverter() {
    return new JacksonJsonMessageConverter();
  }
}
