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

package io.opentracing.contrib.spring.cloud.starter.jaeger.customizer;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.starter.jaeger.AbstractTracerSpringTest;
import io.opentracing.contrib.spring.cloud.starter.jaeger.TracerBuilderCustomizer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(
    properties = {
        "spring.main.banner-mode=off",
        "opentracing.jaeger.enable-b3-propagation=true"
    }
)
@Import(JaegerTracerB3CustomerizerEnabledSpringTest.TestConfiguration.class)
public class JaegerTracerB3CustomerizerEnabledSpringTest extends AbstractTracerSpringTest {

  @Autowired
  private Tracer tracer;

  public static class TestConfiguration {
    @Bean
    public TracerBuilderCustomizer myCustomizer() {
      // Noop
      return builder -> {
      };
    }
  }

  @Test
  public void testCustomizersShouldContainB3Customizer() {
    Map<String, String> carrier = new HashMap<>();
    carrier.put("X-B3-TraceId", "abc");
    carrier.put("X-B3-SpanId", "def");

    com.uber.jaeger.SpanContext context =
        (com.uber.jaeger.SpanContext) tracer.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(carrier));

    assertThat(context).isNotNull();
    assertThat(context.getTraceId()).isEqualTo(0xabc);
    assertThat(context.getSpanId()).isEqualTo(0xdef);
  }
}
