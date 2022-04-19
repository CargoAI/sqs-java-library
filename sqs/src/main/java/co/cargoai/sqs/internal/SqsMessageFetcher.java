package co.cargoai.sqs.internal;

import co.cargoai.sqs.api.SqsMessagePollerProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.Collections;
import java.util.List;

/**
 * Fetches a batch of messages from SQS.
 */
@RequiredArgsConstructor
class SqsMessageFetcher {

  private static final Logger logger = LoggerFactory.getLogger(SqsMessageFetcher.class);
  private final SqsClient sqsClient;
  private final SqsMessagePollerProperties properties;

  List<Message> fetchMessages() {

    logger.debug("fetching messages from SQS queue {}", properties.getQueueUrl());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .maxNumberOfMessages(properties.getBatchSize())
            .queueUrl(properties.getQueueUrl())
            .waitTimeSeconds((int) properties.getWaitTime().getSeconds())
            .build();

    ReceiveMessageResponse result = sqsClient.receiveMessage(request);

    if (result.sdkHttpResponse() == null) {
      logger.error("cannot determine success from response for SQS queue {}: {}",
              properties.getQueueUrl(),
              result.sdkHttpResponse());
      return Collections.emptyList();
    }

    if (result.sdkHttpResponse().statusCode() != 200) {
      logger.error("got error response from SQS queue {}: {}",
          properties.getQueueUrl(),
          result.sdkHttpResponse());
      return Collections.emptyList();
    }

    logger.debug("polled {} messages from SQS queue {}",
        result.messages().size(),
        properties.getQueueUrl());

    return result.messages();
  }

}
