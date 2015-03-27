package com.jinloes.messaging_test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jinloes on 3/26/15.
 */
@SpringBootApplication
@RestController
public class Application {
    public static final List<Message> MESSAGES = new ArrayList<Message>();
    static {
        MESSAGES.add(new Message("foo"));
        MESSAGES.add(new Message("baz"));
    }
    @RequestMapping("/messages")
    public Map<String, Object> getMessages() {
        return new HashMap<String, Object>() {{
            put("messages", MESSAGES);
        }};
    }

    public static class Message {
        String text;

        public Message(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    public static final String queueName = "spring-boot";

    @Autowired private RabbitTemplate rabbitTemplate;

    @Bean
    public Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueName);
    }
}
