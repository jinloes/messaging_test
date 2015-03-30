package com.jinloes.messaging_test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by jinloes on 3/26/15.
 */
@RestController
public class HomeResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeResource.class);
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

    @RequestMapping(value = "/message-file")
    public void sendMessageFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("big.txt");
        InputStream is = resource.getInputStream();
        byte[] bytes = new byte[is.available()];
        IOUtils.readFully(is, bytes);
        LOGGER.info("Sending file of byte size: {}", bytes.length);
        rabbitTemplate.convertAndSend(Application.fileQueueName, bytes);
    }

    @RequestMapping(value = "/message-iso")
    public void sendIso() throws IOException {
        Path path = Paths.get("/Users/jinloes/ubuntu-14.04.2-desktop-amd64.iso");
        byte[] bytes = Files.readAllBytes(path);
        LOGGER.info("Sending file of byte size: {}", bytes.length);
        rabbitTemplate.convertAndSend(Application.fileQueueName, bytes);
    }
}
