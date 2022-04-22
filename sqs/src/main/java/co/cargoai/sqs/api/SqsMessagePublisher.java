package co.cargoai.sqs.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
public abstract class SqsMessagePublisher<T> {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessagePublisher.class);
    private final String sqsQueueUrl;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final RetryRegistry retryRegistry;

    public SqsMessagePublisher(
            String sqsQueueUrl,
            SqsClient sqsClient,
            ObjectMapper objectMapper) {
        this.sqsQueueUrl = sqsQueueUrl;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.retryRegistry = defaultRetryRegistry();
    }

    public SqsMessagePublisher(
            String sqsQueueUrl,
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            RetryRegistry retryRegistry) {
        this.sqsQueueUrl = sqsQueueUrl;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.retryRegistry = retryRegistry;
    }

    private RetryRegistry defaultRetryRegistry() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff())
                .build();
        return RetryRegistry.of(retryConfig);
    }

    public void publish(T message) {
        publish(message, null);
    }

    /**
     * Publishes a message with a pre-configured {@link SendMessageRequest} which gives you all the options you may need
     * from the underlying SQS client. Note that the `queueUrl` and `messageBody` must not be set because they will be set
     * by the this publisher.
     */
    public void publish(T message, SendMessageRequest request) {

        Retry retry = retryRegistry.retry("publish");
        retry.getEventPublisher()
                .onError(event -> logger.warn("error publishing message to queue {}", this.sqsQueueUrl));
        retry.executeRunnable(() -> doPublish(message, request));
    }

    private void doPublish(T message, SendMessageRequest request) {
        try {
            logger.debug("sending message to SQS queue {}", sqsQueueUrl);
            if(request == null) {
                request = SendMessageRequest.builder()
                        .queueUrl(sqsQueueUrl)
                        .messageBody(objectMapper.writeValueAsString(message))
                        .build();
            } else {
                request = request.toBuilder()
                        .queueUrl(sqsQueueUrl)
                        .messageBody(objectMapper.writeValueAsString(message))
                        .build();
            }

            SendMessageResponse result = sqsClient.sendMessage(request);

            if (result.sdkHttpResponse().statusCode() != 200) {
                throw new RuntimeException(String.format("got error response from SQS queue %s: %s",
                        sqsQueueUrl,
                        result.sdkHttpResponse()));
            }

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("error sending message to SQS: ", e);
        }
    }
}
