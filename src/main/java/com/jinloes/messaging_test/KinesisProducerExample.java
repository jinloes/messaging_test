package com.jinloes.messaging_test;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordsRequest;
import com.amazonaws.services.kinesis.model.PutRecordsRequestEntry;
import com.amazonaws.services.kinesis.model.PutRecordsResult;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class KinesisProducerExample {

  public static void main(String[] args) {
    AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();
    ClientConfiguration clientConfiguration = new ClientConfiguration();
    clientBuilder.setRegion(Regions.US_WEST_1.getName());
    clientBuilder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(System.getenv("AWS_KEY"), System.getenv("AWS_SECRET"))));
    clientBuilder.setClientConfiguration(clientConfiguration);

    AmazonKinesis kinesisClient = clientBuilder.build();

    PutRecordsRequest putRecordsRequest  = new PutRecordsRequest();
    putRecordsRequest.setStreamName("jon-test");
    List<PutRecordsRequestEntry> putRecordsRequestEntryList  = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      PutRecordsRequestEntry putRecordsRequestEntry  = new PutRecordsRequestEntry();
      putRecordsRequestEntry.setData(ByteBuffer.wrap(String.valueOf(i).getBytes()));
      putRecordsRequestEntry.setPartitionKey(String.format("partitionKey-%d", i));
      putRecordsRequestEntryList.add(putRecordsRequestEntry);
    }

    putRecordsRequest.setRecords(putRecordsRequestEntryList);
    PutRecordsResult putRecordsResult  = kinesisClient.putRecords(putRecordsRequest);
    System.out.println("Put Result" + putRecordsResult);
  }
}
