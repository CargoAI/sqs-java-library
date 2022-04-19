package co.cargoai.sqs.internal;

import co.cargoai.sqs.SqsTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;

@SqsTest(queueNames = "testMessages")
@SpringBootTest
@Tag("IgnoreInCi")
class SendAndReceiveIntegrationTest {

    @Autowired
    private SqsClient sqsClient;

    @Autowired
    private TestMessageHandler messageHandler;

    @Autowired
    private TestMessagePublisher messagePublisher;

    @Test
    void sendAndReceive() {
        messagePublisher.publish(new TestMessage("message 1"));

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(messageHandler.getCount()).isEqualTo(1));
    }

}
