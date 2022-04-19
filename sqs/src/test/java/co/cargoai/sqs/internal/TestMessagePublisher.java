package co.cargoai.sqs.internal;

import co.cargoai.sqs.api.SqsMessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;

class TestMessagePublisher extends SqsMessagePublisher<TestMessage> {

    TestMessagePublisher(SqsClient sqsClient, ObjectMapper objectMapper) {
        super("http://localhost:4576/queue/testMessages", sqsClient, objectMapper);
    }

}
