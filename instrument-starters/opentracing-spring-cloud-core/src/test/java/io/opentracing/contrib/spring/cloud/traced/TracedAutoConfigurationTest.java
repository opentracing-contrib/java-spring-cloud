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
package io.opentracing.contrib.spring.cloud.traced;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.aop.Traced;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import org.assertj.core.api.WithAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
public class TracedAutoConfigurationTest implements WithAssertions {

  @Autowired
  private TracedClass tracedClass;
  @Autowired
  private IgnoredTracedClass ignoredTracedClass;
  @Autowired
  private MockTracer tracer;

  @Before()
  public void setup() {
    tracer.buildSpan("test").start();
  }

  @After()
  public void tearDown() {
    tracer.reset();
  }

  @Test
  public void givenIgnoredClass_whenMethodInvocked_thenNoSpanIsGenerated() {
    ignoredTracedClass.ignoredTraced();
    assertThat(tracer.finishedSpans()).isEmpty();
  }

  @Test
  public void givenActiveTracer_whenITrace_thenTraceIsGenerated() {
    tracedClass.traced();
    assertThat(tracer.finishedSpans()).hasSize(1);
    assertThat(tracer.finishedSpans().get(0).operationName()).isEqualTo("traced");
    assertThat(tracer.finishedSpans().get(0).tags().get(Tags.COMPONENT.getKey()))
        .isEqualTo("traced");
  }

  @Test
  public void givenActiveTracer_whenITraceWithCustomComponent_thenTraceIsGenerated() {
    tracedClass.tracedComponent();
    assertThat(tracer.finishedSpans()).hasSize(1);
    assertThat(tracer.finishedSpans().get(0).operationName()).isEqualTo("tracedComponent");
    assertThat(tracer.finishedSpans().get(0).tags().get(Tags.COMPONENT.getKey()))
        .isEqualTo("custom");
  }

  @Test
  public void givenActiveTracer_whenITraceWithCustomOperation_thenTraceIsGenerated() {
    tracedClass.tracedOperation();
    assertThat(tracer.finishedSpans()).hasSize(1);
    assertThat(tracer.finishedSpans().get(0).operationName()).isEqualTo("customOps");
    assertThat(tracer.finishedSpans().get(0).tags().get(Tags.COMPONENT.getKey()))
        .isEqualTo("traced");
  }

  @Test
  public void givenActiveTracer_whenITraceWhitChild_thenTraceIsGenerated() {
    tracedClass.child("someChildSpan");
    assertThat(tracer.finishedSpans()).hasSize(2);
    assertThat(tracer.finishedSpans().get(1).operationName()).isEqualTo("child");
    assertThat(tracer.finishedSpans().get(1).tags().get(Tags.COMPONENT.getKey()))
        .isEqualTo("traced");
    assertThat(tracer.finishedSpans().get(0).operationName()).isEqualTo("someChildSpan");
    assertThat(tracer.finishedSpans().get(0).tags()).isEmpty();
  }

  @SpringBootApplication
  @Import(MockTracingConfiguration.class)
  public static class TestConfig {

    @Bean
    public TracedClass tracedClass() {
      return new TracedClass();
    }

    @Bean
    public IgnoredTracedClass ignoredTracedClass() {
      return new IgnoredTracedClass();
    }
  }

  public static class TracedClass {
    @Autowired
    private Tracer tracer;

    @Traced
    public void traced() {

    }

    @Traced
    public void child(String spanName) {
      tracer.buildSpan(spanName).start().finish();
    }

    @Traced(operationName = "customOps")
    public void tracedOperation() {

    }

    @Traced(component = "custom")
    public void tracedComponent() {

    }
  }
  public static class IgnoredTracedClass {
    @Traced
    public void ignoredTraced() {

    }

  }
}
