package co.cargoai.sqs.internal;

import co.cargoai.sqs.api.DefaultExceptionHandler;
import co.cargoai.sqs.api.SqsMessageHandlerRegistration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
class SqsAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(SqsAutoConfiguration.class);

  @Bean
  SqsMessageHandlerRegistry sqsMessageHandlerRegistry(List<SqsMessageHandlerRegistration<?>> registrations) {
    logger.info("starting configuration sqsMessageHandlerRegistry process");
    return new SqsMessageHandlerRegistry(registrations);
  }

  @Bean
  SqsAutoConfigurationLifecycle sqsLifecycle(SqsMessageHandlerRegistry registry) {
    logger.info("starting configuration sqsLifecycle process");
    return new SqsAutoConfigurationLifecycle(registry);
  }

}
