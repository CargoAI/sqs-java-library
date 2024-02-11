package co.cargoai.sqs.api;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.util.Map;

@Builder
@Getter
public class Message<T> {
    private T body;
    private String receiptHandle;
    private String messageId;
    private Map<MessageSystemAttributeName, String> attributes;
}
