package com.jinloes.messaging_test;

import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;

/**
 * Created by jinloes on 3/31/15.
 */
public class FailOverRabbitTemplate extends RabbitTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(FailOverRabbitTemplate.class);
    private final BlockingQueue<Object> failMessageQueue;

    public FailOverRabbitTemplate(ConnectionFactory connectionFactory,
            BlockingQueue<Object> failMessageQueue) {
        super(connectionFactory);
        this.failMessageQueue = failMessageQueue;
    }

    @Override
    public void send(final String exchange, final String routingKey,
            final Message message, final CorrelationData correlationData) {
        try {
            super.send(exchange, routingKey, message, correlationData);
        } catch (AmqpConnectException ex) {
            LOGGER.error("Failed to send message", ex);
            failMessageQueue.add(message);
        }
    }
}
