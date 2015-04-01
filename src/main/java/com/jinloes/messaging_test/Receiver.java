package com.jinloes.messaging_test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Created by jinloes on 3/26/15.
 */
@Component
public class Receiver {
    private static final Logger LOGGER = LoggerFactory.getLogger(Receiver.class);

    @RabbitListener(queues = Application.queueName)
    public void receiveMessage(String message) {
        Application.MESSAGES.add(new Application.Message(message));
        LOGGER.info("Received <" + message + ">");
    }

    @RabbitListener(queues = Application.fileQueueName)
    public void receiveMessage(byte[] message) {
        Application.MESSAGES.add(new Application.Message("I just received a file of "
                + message.length + " bytes"));
        LOGGER.info("Received file of size: " + message.length);
    }
}
