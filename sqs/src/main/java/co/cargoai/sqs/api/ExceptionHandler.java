package co.cargoai.sqs.api;

import software.amazon.awssdk.services.sqs.model.Message;

public interface ExceptionHandler {

    enum ExceptionHandlerDecision {

        /**
         * Delete the message from SQS. It will not be retried.
         */
        NOTHING,

        /**
         * Do not delete the message from SQS. In one of the next iterations, it will be polled by the poller again.
         */
        DLQ;

    }

    /**
     * Handles any exception that is thrown during message processing by an {@link SqsMessageHandler}.
     */
    ExceptionHandlerDecision handleException(Message message, Exception e);

    static ExceptionHandler defaultExceptionHandler() {
        return new DefaultExceptionHandler();
    }
}
