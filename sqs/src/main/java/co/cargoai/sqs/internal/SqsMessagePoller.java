package co.cargoai.sqs.internal;

import co.cargoai.sqs.api.ExceptionHandler;
import co.cargoai.sqs.api.SqsMessageHandler;
import co.cargoai.sqs.api.SqsMessagePollerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Polls messages from an SQS queue in potentially multiple threads at regular intervals.
 *
 * @param <T> the type of message.
 */
@RequiredArgsConstructor
class SqsMessagePoller<T> {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessagePoller.class);
    private final String name;
    private final SqsMessageHandler<T> messageHandler;
    private final SqsMessageFetcher messageFetcher;
    private final SqsMessagePollerProperties pollingProperties;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final ScheduledThreadPoolExecutor pollerThreadPool;
    private final ThreadPoolExecutor handlerThreadPool;
    private final ExceptionHandler exceptionHandler;


    void start() {
        logger.info("starting SqsMessagePoller");
        for (int i = 0; i < pollerThreadPool.getCorePoolSize(); i++) {
            logger.info("starting SqsMessagePoller ({}) - thread {}", this.name, i);
            pollerThreadPool.scheduleWithFixedDelay(
                    this::pollMessages,
                    pollingProperties.getPollDelay().getSeconds(),
                    pollingProperties.getPollDelay().getSeconds(),
                    TimeUnit.SECONDS);
        }
    }

    void stop() {
        logger.info("stopping SqsMessagePoller");
        pollerThreadPool.shutdownNow();
        handlerThreadPool.shutdownNow();
    }

    void pollMessages() {
        try {
            List<Message> messages = messageFetcher.fetchMessages();
            for (Message sqsMessage : messages) {
                handleMessage(sqsMessage);
            }
        } catch (Exception e) {
            logger.error("error fetching messages from queue {}:", pollingProperties.getQueueUrl(), e);
        }
    }

    private void handleMessage(Message sqsMessage) {
        try {
            final T body = objectMapper.readValue(sqsMessage.body(), messageHandler.messageType());
            co.cargoai.sqs.api.Message<T> message = co.cargoai.sqs.api.Message.<T>builder()
                    .body(body)
                    .receiptHandle(sqsMessage.receiptHandle())
                    .messageId(sqsMessage.messageId())
                    .attributes(sqsMessage.attributes())
                    .build();
            acknowledgeMessage(sqsMessage);
            handlerThreadPool.submit(() -> {
                try {
                    long startTime = System.nanoTime();
                    messageHandler.onBeforeHandle(message);
                    messageHandler.handle(message);
                    long endTime = System.nanoTime();
                    long durationInNano = endTime - startTime;
                    double durationInMilliseconds = (double) durationInNano / 1_000_000.0;
                    logger.info("message {} processed successfully in {}", sqsMessage.messageId(), durationInMilliseconds);
                    if (durationInMilliseconds > 10000) {
                        logger.info("message {} took more than 10 sec", sqsMessage.messageId());
                        logger.info(message.getBody().toString());
                    }
                    if (durationInMilliseconds > 20000) {
                        logger.error("error: message {} took more than 20sec", sqsMessage.messageId());
                    }
                } catch (Exception e) {
                    ExceptionHandler.ExceptionHandlerDecision result = exceptionHandler.handleException(sqsMessage, e);
                    switch (result) {
                        case NOTHING:
                            // do nothing ... message is deleted
                            break;
                        case DLQ:
                            transferToDLQ(sqsMessage);
                            break;
                    }
                } finally {
                    messageHandler.onAfterHandle(message);
                }

            });
        } catch (JsonProcessingException e) {
            logger.warn("error handling message {}: {}", sqsMessage.messageId(), e.getMessage());
        }
    }

    private void transferToDLQ(Message message) {
        try {
            if (pollingProperties.getDlqUrl() == null || pollingProperties.getDlqUrl().isEmpty()) {
                logger.error("cannot write to non existing DLQ");
            }
            var request = SendMessageRequest.builder()
                    .queueUrl(pollingProperties.getDlqUrl())
                    .messageBody(objectMapper.writeValueAsString(message.body()))
                    .build();

            SendMessageResponse result = sqsClient.sendMessage(request);

            if (result.sdkHttpResponse().statusCode() != 200) {
                throw new RuntimeException(String.format("got error response from SQS queue %s: %s",
                        pollingProperties.getDlqUrl(),
                        result.sdkHttpResponse()));
            }

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("error sending message to SQS: ", e);
        }
    }

    private void acknowledgeMessage(Message message) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(pollingProperties.getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
    }

}
