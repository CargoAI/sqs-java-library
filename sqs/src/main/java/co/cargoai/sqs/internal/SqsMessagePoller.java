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
                    .attributes(sqsMessage.attributes())
                    .build();
            handlerThreadPool.submit(() -> {
                try {
                    messageHandler.onBeforeHandle(message);
                    messageHandler.handle(message);
                    acknowledgeMessage(sqsMessage);
                    logger.debug("message {} processed successfully - message has been deleted from SQS", sqsMessage.messageId());
                } catch (Exception e) {
                    ExceptionHandler.ExceptionHandlerDecision result = exceptionHandler.handleException(sqsMessage, e);
                    switch (result) {
                        case RETRY:
                            // do nothing ... the message hasn't been deleted from SQS yet, so it will be retried
                            break;
                        case DELETE:
                            acknowledgeMessage(sqsMessage);
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

    private void acknowledgeMessage(Message message) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(pollingProperties.getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
    }

}
