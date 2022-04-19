package co.cargoai.sqs.api;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

/**
 * Creates an SQS queue on startup. Useful in tests.
 */
@RequiredArgsConstructor
public class SqsQueueInitializer implements InitializingBean {

    private final SqsClient sqsClient;
    private final String queueName;

    private void initializeQueue(SqsClient sqsClient, String queueName) {
        sqsClient.createQueue(CreateQueueRequest.builder().queueName(queueName).build());
    }

    @Override
    public void afterPropertiesSet() {
        initializeQueue(sqsClient, queueName);
    }
}
