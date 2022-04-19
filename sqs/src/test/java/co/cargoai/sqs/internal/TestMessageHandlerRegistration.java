package co.cargoai.sqs.internal;

import co.cargoai.sqs.api.SqsMessageHandler;
import co.cargoai.sqs.api.SqsMessageHandlerProperties;
import co.cargoai.sqs.api.SqsMessageHandlerRegistration;
import co.cargoai.sqs.api.SqsMessagePollerProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sqs.SqsClient;

class TestMessageHandlerRegistration implements SqsMessageHandlerRegistration<TestMessage> {

    private final SqsClient client;
    private final ObjectMapper objectMapper;
    private final TestMessageHandler messageHandler;

    public TestMessageHandlerRegistration(
            SqsClient client,
            ObjectMapper objectMapper,
            TestMessageHandler messageHandler) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.messageHandler = messageHandler;
    }

    @Override
    public SqsMessageHandler<TestMessage> messageHandler() {
        return this.messageHandler;
    }

    @Override
    public String name() {
        return "testMessageHandler";
    }

    @Override
    public SqsMessageHandlerProperties messageHandlerProperties() {
        return new SqsMessageHandlerProperties();
    }

    @Override
    public SqsMessagePollerProperties messagePollerProperties() {
        return new SqsMessagePollerProperties("http://localhost:4576/queue/testMessages");
    }

    @Override
    public SqsClient sqsClient() {
        return this.client;
    }

    @Override
    public ObjectMapper objectMapper() {
        return this.objectMapper;
    }
}
