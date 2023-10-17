package co.cargoai.sqs.api;

import lombok.Data;

@Data
public class SqsMessageHandlerProperties {

    private int handlerThreadPoolSize = 1;

    private int handlerQueueSize = 1000;

    /**
     * The size of the thread pool of {@link SqsMessageHandler}s.
     */
    public SqsMessageHandlerProperties withHandlerThreadPoolSize(int handlerThreadPoolSize){
        this.handlerThreadPoolSize = handlerThreadPoolSize;
        return this;
    }

    /**
     * The size of the in-memory queue of the thread pool of {@link SqsMessageHandler}s.
     */
    public SqsMessageHandlerProperties withHandlerQueueSize(int handlerQueueSize){
        this.handlerQueueSize = handlerQueueSize;
        return this;
    }
}
