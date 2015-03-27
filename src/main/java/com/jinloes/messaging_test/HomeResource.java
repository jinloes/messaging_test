package com.jinloes.messaging_test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jinloes on 3/26/15.
 */
@RestController
public class HomeResource {
    @Autowired private RabbitTemplate rabbitTemplate;

    @RequestMapping(value = "/resource")
    public Map<String, Object> home() {
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("id", UUID.randomUUID().toString());
        model.put("content", "Hello World");
        return model;
    }

    @RequestMapping(value = "/message")
    public void sendMessage() {
        rabbitTemplate.convertAndSend(Application.queueName, "Hello rabbit!");
    }
}
