package io.opentracing.contrib.spring.cloud;

import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import io.opentracing.contrib.spring.web.autoconfig.WebTracingConfiguration;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.util.ThreadLocalActiveSpanSource;

/**
 * @author Pavol Loffay
 */
@Configuration
@EnableAutoConfiguration
public class MockTracingConfiguration {

  @Bean
  public MockTracer mockTracer() {
    return new MockTracer(new ThreadLocalActiveSpanSource(), Propagator.TEXT_MAP);
  }

  @Bean
  public WebTracingConfiguration webTracingConfiguration() {
    return WebTracingConfiguration.builder()
            .withSkipPattern(Pattern.compile("/notTraced"))
            .build();
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.build();
  }

  @Bean
  public AsyncRestTemplate asyncRestTemplate() {
    return new AsyncRestTemplate();
  }
}
