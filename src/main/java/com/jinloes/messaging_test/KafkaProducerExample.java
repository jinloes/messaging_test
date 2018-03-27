package com.jinloes.messaging_test;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Random;

public class KafkaProducerExample {

  public static void main(String[] args) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "ec2-52-53-241-105.us-west-1.compute.amazonaws.com:9092");
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.RETRIES_CONFIG, 0);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

    Producer<String, String> producer = new KafkaProducer<>(props);
    TestCallback callback = new TestCallback();
    Random rnd = new Random();
    for (long i = 0; i < 100; i++) {
      ProducerRecord<String, String> data = new ProducerRecord<>("test-topic", "key-" + i, "message-" + i);
      producer.send(data, callback);
    }

    producer.close();
  }

  private static class TestCallback implements Callback {
    @Override
    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
      if (e != null) {
        System.out.println("Error while producing message to topic :" + recordMetadata);
        e.printStackTrace();
      } else {
        String message = String.format("sent message to topic:%s partition:%s  offset:%s", recordMetadata.topic(),
            recordMetadata.partition(), recordMetadata.offset());
        System.out.println(message);
      }
    }
  }
}
