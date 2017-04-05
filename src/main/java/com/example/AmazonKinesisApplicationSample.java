package com.example;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;

import java.net.InetAddress;
import java.util.UUID;

public class AmazonKinesisApplicationSample {
    public static final String SAMPLE_APPLICATION_STREAM_NAME = "myFirstStream";
    private static final String SAMPLE_APPLICATION_NAME = "SampleKinesisApplication";

    private static final InitialPositionInStream SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM =
            InitialPositionInStream.LATEST;

    private static AWSCredentialsProvider credentialsProvider;

    private static void init() {
        java.security.Security.setProperty("networkaddress.cache.ttl", "60");

        credentialsProvider = new ProfileCredentialsProvider();
        try {
            credentialsProvider.getCredentials();
        }
        catch (Exception e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. "
                    + "Please make sure that your credentials file is at the correct "
                    + "location (~/.aws/credentials), and is in valid format.", e);
        }
    }

    public static void main(String[] args) throws Exception {
        init();

        if (args.length == 1 && "delete-resources".equals(args[0])) {
            deleteResources();
            return;
        }

        String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(SAMPLE_APPLICATION_NAME,
                        SAMPLE_APPLICATION_STREAM_NAME,
                        credentialsProvider,
                        workerId);
        kinesisClientLibConfiguration.withInitialPositionInStream(SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM);

        IRecordProcessorFactory recordProcessorFactory = new AmazonKinesisApplicationRecordProcessorFactory();
        Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration);

        System.out.printf("Running %s to process stream %s as worker %s...\n",
                SAMPLE_APPLICATION_NAME,
                SAMPLE_APPLICATION_STREAM_NAME,
                workerId);

        int exitCode = 0;
        try {
            worker.run();
        }
        catch (Throwable t) {
            System.err.println("Caught throwable while processing data.");
            t.printStackTrace();
            exitCode = 1;
        }
        System.exit(exitCode);
    }

    public static void deleteResources() {
        AWSCredentials credentials = credentialsProvider.getCredentials();

        // Delete the stream
        AmazonKinesis kinesis = AmazonKinesisClientBuilder.standard().withCredentials(
                new AWSStaticCredentialsProvider(credentials)).build();
        System.out.printf("Deleting the Amazon Kinesis stream used by the sample. Stream Name = %s.\n",
                SAMPLE_APPLICATION_STREAM_NAME);
        try {
            kinesis.deleteStream(SAMPLE_APPLICATION_STREAM_NAME);
        }
        catch (Exception ex) {
            // The stream doesn't exist.
        }

        // Delete the table
        AmazonDynamoDBClient dynamoDB = new AmazonDynamoDBClient(credentialsProvider.getCredentials());
        System.out.printf("Deleting the Amazon DynamoDB table used by the Amazon Kinesis Client Library. " +
                        "Table Name = %s.\n", SAMPLE_APPLICATION_NAME);
        try {
            dynamoDB.deleteTable(SAMPLE_APPLICATION_NAME);
        }
        catch (Exception ex) {
            // The table doesn't exist.
        }
    }

}
