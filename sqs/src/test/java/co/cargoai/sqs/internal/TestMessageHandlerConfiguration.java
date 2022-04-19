package co.cargoai.sqs.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

@Configuration
class TestMessageHandlerConfiguration {

    @Bean
    SqsClient sqsClient() {
        return SqsClient.builder()
                .endpointOverride(URI.create("http://localhost:4576"))
                .region(Region.US_EAST_1)
                .build();
    }

    @Bean
    TestMessageHandler messageHandler(){
        return new TestMessageHandler();
    }

    @Bean
    TestMessageHandlerRegistration testMessageHandlerRegistration(SqsClient sqsClient, ObjectMapper objectMapper, TestMessageHandler messageHandler) {
        return new TestMessageHandlerRegistration(sqsClient, objectMapper, messageHandler);
    }

    @Bean
    TestMessagePublisher testMessagePublisher(SqsClient sqsClient, ObjectMapper objectMapper) {
        return new TestMessagePublisher(sqsClient, objectMapper);
    }

    @Bean
    ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

}
