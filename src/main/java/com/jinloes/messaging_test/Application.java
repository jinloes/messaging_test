package com.jinloes.messaging_test;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.redis.store.RedisChannelMessageStore;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * set spring.rabbitmq.host to change the rabbit host
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
    public static final String fileQueueName = "spring-boot-file";

    @Bean
    public ConnectionFactory rabbitConnectionFactory(RabbitProperties config) {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        String addresses = config.getAddresses();
        factory.setAddresses(addresses);
        if (config.getHost() != null) {
            factory.setHost(config.getHost());
            factory.setPort(config.getPort());
        }
        if (config.getUsername() != null) {
            factory.setUsername(config.getUsername());
        }
        if (config.getPassword() != null) {
            factory.setPassword(config.getPassword());
        }
        if (config.getVirtualHost() != null) {
            factory.setVirtualHost(config.getVirtualHost());
        }
        factory.setPublisherConfirms(true);
        return factory;
    }

    @Bean
    public DB mapDb() {
        return DBMaker.newFileDB(Paths.get("/tmp/map-db").toFile()).make();
    }

    @Bean
    public BlockingQueue<Object> failedMessageQueue() {
        return mapDb().createQueue("failed-message-queue", Serializer.JAVA, false);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(RabbitProperties config) {
        return new FailOverRabbitTemplate(rabbitConnectionFactory(config), failedMessageQueue());
    }

    @Bean
    public RedisChannelMessageStore redisMessageStore(RedisConnectionFactory connectionFactory) {
        return new RedisChannelMessageStore(connectionFactory);
    }

    @Bean
    public MessageChannel redisBackedQueue(RedisChannelMessageStore redisMessageStore) {
        Object group = "redis-backed-group";
        //return MessageChannels.queue(redisMessageStore, group).get();
        return MessageChannels.queue().get();
    }

    @Bean
    public Queue queue() {
        return new Queue(queueName, false);
    }

    @Bean
    public Queue fileQueue() {
        return new Queue(fileQueueName, false);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange("spring-boot-exchange");
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(queueName);
    }

    @Bean
    public Binding fileBinding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(fileQueueName);
    }

    @Bean
    public AmqpOutboundEndpoint outboundMessageAdapter(RabbitTemplate rabbitTemplate) {
        return Amqp.outboundAdapter(rabbitTemplate)
                .exchangeName("spring-boot-exchange")
                .routingKey("spring-boot")
                .confirmCorrelationExpression("payload")
                .confirmAckChannel(ackChannel())
                .confirmNackChannel(nackChannel())
                .get();
    }

    @Bean
    public MessageChannel ackChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public MessageChannel nackChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public IntegrationFlow ackFlow() {
        return IntegrationFlows.from(ackChannel())
                .handle(new MessageHandler() {
                    @Override
                    public void handleMessage(org.springframework.messaging.Message<?> message)
                            throws MessagingException {
                        System.out.println("message acked.");
                    }
                })
                .get();
    }

    @Bean
    public IntegrationFlow nackFlow() {
        return IntegrationFlows.from(nackChannel())
                .handle(new MessageHandler() {
                    @Override
                    public void handleMessage(org.springframework.messaging.Message<?> message)
                            throws MessagingException {
                        System.out.println("message nacked.");
                    }
                })
                .get();
    }


    @Bean
    public IntegrationFlow sendMigrationScheduleRequestFlow(final RabbitTemplate rabbitTemplate,
            MessageChannel redisBackedQueue) {
        return IntegrationFlows.from(redisBackedQueue)
                .handle(outboundMessageAdapter(rabbitTemplate))
                .get();
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller() {
        return Pollers.fixedDelay(500).get();
    }

    @Autowired
    private ConnectionFactory connectionFactory;

    @InboundChannelAdapter(value = "redisBackedQueue", poller = @Poller(fixedRate = "1000"))
    public Object message() {
        try {
            Connection conn = connectionFactory.createConnection();
            if (conn != null && conn.isOpen() && !failedMessageQueue().isEmpty()) {
                return failedMessageQueue().poll();
            }
        } catch (AmqpConnectException e) {
            // Failed to create connection queue won't be flushed
        }
        return null;
    }

}
