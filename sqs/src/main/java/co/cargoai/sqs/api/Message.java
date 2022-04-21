package co.cargoai.sqs.api;

import lombok.Builder;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.util.Map;

@Builder
public class Message<T> {
    T body;
    Map<MessageSystemAttributeName, String> attributes;
}
