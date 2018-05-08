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

package io.opentracing.contrib.spring.cloud.starter.zipkin.basic;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.starter.zipkin.AbstractZipkinTracerSpringTest;
import io.opentracing.contrib.spring.cloud.starter.zipkin.ZipkinAutoConfiguration;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

/**
 * The order of the classes matters here In application code, auto-configuration classes are loaded
 * last so the order of the classes simulates a real scenario
 */
@SpringBootTest(classes = {
    ZipkinTracerBackoffSpringTest.MockTracerConfiguration.class,
    ZipkinAutoConfiguration.class
})
@TestPropertySource(
    properties = {
        "spring.main.banner-mode=off",
        "opentracing.zipkin.enabled=true"
    }
)
public class ZipkinTracerBackoffSpringTest extends AbstractZipkinTracerSpringTest {

  @Test
  public void testIfTracerIsMockTracer() {
    assertThat(tracer).isNotNull();
    assertThat(tracer).isInstanceOf(MockTracer.class);
  }

  @Configuration
  public static class MockTracerConfiguration {

    @Bean
    public Tracer mockTracer() {
      return new MockTracer();
    }
  }
}
