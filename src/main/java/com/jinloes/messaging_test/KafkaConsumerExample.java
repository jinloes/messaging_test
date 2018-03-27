package com.jinloes.messaging_test;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerExample {

  public static void main(String[] args) {
    Properties consumerConfig = new Properties();
    consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "ec2-13-56-178-227.us-west-1.compute.amazonaws.com:9092");
    consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "my-group");
    consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<>(consumerConfig);
    TestConsumerRebalanceListener rebalanceListener = new TestConsumerRebalanceListener();
    consumer.subscribe(Collections.singletonList("test-topic"));

    try {
      while (true) {
        ConsumerRecords<byte[], byte[]> records = consumer.poll(1000);
        for (ConsumerRecord<byte[], byte[]> record : records) {
          System.out.printf("Received Message topic =%s, partition =%s, offset = %d, key = %s, value = %s\n", record.topic(), record.partition(), record.offset(), record.key(), record.value());
        }

        consumer.commitSync();
      }
    } finally {
      consumer.close();
    }

  }

  private static class TestConsumerRebalanceListener implements ConsumerRebalanceListener {
    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
      System.out.println("Called onPartitionsRevoked with partitions:" + partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
      System.out.println("Called onPartitionsAssigned with partitions:" + partitions);
    }
  }
}
