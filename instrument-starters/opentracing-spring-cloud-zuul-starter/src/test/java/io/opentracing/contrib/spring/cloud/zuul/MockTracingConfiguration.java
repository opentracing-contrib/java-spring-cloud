/**
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.zuul;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Pavol Loffay
 */
@Configuration
@EnableAutoConfiguration
public class MockTracingConfiguration {

  @Bean
  public MockTracer mockTracer() {
    GlobalTracerTestUtil.resetGlobalTracer();
    return new MockTracer();
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
    return restTemplateBuilder.build();
  }

  @Bean
  public AsyncRestTemplate asyncRestTemplate() {
    return new AsyncRestTemplate();
  }

  public static TestRestTemplate createNotTracedRestTemplate(int port) {
    return new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:" + port));
  }
}
