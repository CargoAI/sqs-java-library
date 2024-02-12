package co.cargoai.sqs.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

public class DefaultExceptionHandler implements ExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    @Override
    public ExceptionHandlerDecision handleException(Message message, Exception e) {
        logger.warn("error while processing message {} - message has not been deleted from SQS and will be retried:", message.messageId(), e);
        return ExceptionHandlerDecision.NOTHING;
    }
}
