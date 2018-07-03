package com.jinloes.messaging_test;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.InvalidStateException;
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorFactory;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.Record;

import java.util.List;
import java.util.UUID;

public class KinesisConsumerExample {

  public static void main(String[] args) {
    final KinesisClientLibConfiguration config = new KinesisClientLibConfiguration("app1", "jon-test",
        new AWSStaticCredentialsProvider(new BasicAWSCredentials(System.getenv("AWS_KEY"), System.getenv("AWS_SECRET"))),
        UUID.randomUUID().toString())
        .withRegionName(Regions.US_WEST_1.getName());

    final IRecordProcessorFactory recordProcessorFactory = new IRecordProcessorFactory() {
      @Override
      public IRecordProcessor createProcessor() {
        final String[] shard = new String[1];
        return new IRecordProcessor() {
          @Override
          public void initialize(String shardId) {
            shard[0] = shardId;
            System.out.println("shard id: " + shardId);
          }

          @Override
          public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
            records.forEach(System.out::println);
            try {
              checkpointer.checkpoint();
            } catch (InvalidStateException e) {
              e.printStackTrace();
            } catch (ShutdownException e) {
              e.printStackTrace();
            }
          }

          @Override
          public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {
            if(reason == ShutdownReason.TERMINATE) {
              try {
                checkpointer.checkpoint();
              } catch (InvalidStateException e) {
                e.printStackTrace();
              } catch (ShutdownException e) {
                e.printStackTrace();
              }
            }
          }
        };
      }
    };
    final Worker worker = new Worker.Builder()
        .recordProcessorFactory(recordProcessorFactory)
        .config(config)
        .build();
    worker.run();
  }
}
