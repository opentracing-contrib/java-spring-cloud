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
package io.opentracing.contrib.spring.cloud.hystrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.strategy.HystrixPlugins;

import io.opentracing.Scope;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(classes = {MockTracingConfiguration.class,
    TracedHystrixCommandTest.TestConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracedHystrixCommandTest {

  @EnableHystrix
  @Configuration
  static class TestConfig {

    @Bean
    public GreetingService greetingService() {
      return new GreetingService();
    }
  }

  @Service
  static class GreetingService {

    @Autowired
    private MockTracer tracer;

    @HystrixCommand
    public String sayHello() {
      tracer.buildSpan("sayHello").start().finish();
      return "Hello!!!";
    }

    @HystrixCommand(fallbackMethod = "defaultGreeting")
    public String alwaysFail() {
      tracer.buildSpan("alwaysFail").start().finish();
      throw new IllegalStateException("Thrown Purposefully");

    }

    public String defaultGreeting() {
      tracer.buildSpan("defaultGreeting").withTag("fallback", "yes").start().finish();
      return "Hi(fallback)";
    }
  }

  @Autowired
  private MockTracer mockTracer;

  @Autowired
  private GreetingService greetingService;

  @BeforeClass
  public static void beforeClass() {
    //reset because concurrent strategy is registered with different tracer
    HystrixPlugins.reset();
  }

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testWithoutCircuitBreaker() throws Exception {

    try (Scope scope = mockTracer.buildSpan("test_without_circuit_breaker")
        .startActive(true)) {
      String response = greetingService.sayHello();
      assertThat(response).isNotNull();
    }

    /**
     * 2 spans totally
     * <ul>
     *     <li>one that's started in test</li>
     *     <li>one that's added in sayHello method of Greeting Service</li>
     * </ul>
     */

    await().atMost(3, TimeUnit.SECONDS).until(() -> mockTracer.finishedSpans().size() == 2);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
    MockSpan hystrixSpan = mockSpans.get(1);
    assertThat(hystrixSpan.tags()).isEmpty();
  }

  @Test
  public void testWithCircuitBreaker() {
    try (Scope scope = mockTracer.buildSpan("test_with_circuit_breaker")
        .startActive(true)) {
      String response = greetingService.alwaysFail();
      assertThat(response).isNotNull();
    }

    /**
     * 3 spans totally
     * <ul>
     *     <li>one thats started in test</li>
     *     <li>one thats added in alwaysFail method of Greeting Service</li>
     *     <li>one thats added in the defaultGreeting method which is a fallback</li>
     * </ul>
     */
    await().atMost(3, TimeUnit.SECONDS).until(() -> mockTracer.finishedSpans().size() == 3);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(3, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);
    MockSpan hystrixSpan = mockSpans.get(1);
    assertThat(hystrixSpan.tags()).isNotEmpty();
    //one thats added in the defaultGreeting method which is a fallback should have the custom tag added
    assertThat(hystrixSpan.tags().get("fallback")).isEqualTo("yes");
  }

  @Test
  public void testHystrixTracedCommand() throws Exception {
    String groupKey = "test_hystrix";
    String commandKey = "hystrix_trace_command";

    try (Scope scope = mockTracer.buildSpan("test_with_circuit_breaker")
        .startActive(true)) {
      com.netflix.hystrix.HystrixCommand.Setter setter = com.netflix.hystrix.HystrixCommand.Setter
          .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
          .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
      new TracedHystrixCommand<Void>(mockTracer, setter) {
        @Override
        public Void doRun() throws Exception {
          mockTracer.buildSpan("doRun").start().finish();
          return null;
        }
      }.execute();
    }

    /**
     * 3 spans totally
     * <ul>
     *     <li>one that's started in test</li>
     *     <li>one that's added in doRun method of HystrixTraceCommand</li>
     *     <li>one that's is instrumented by HystrixTraceCommand</li>
     * </ul>
     */
    await().atMost(3, TimeUnit.SECONDS).until(() -> mockTracer.finishedSpans().size() == 3);

    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(3, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);

    Map tags = mockSpans.get(1).tags();
    assertThat(tags).isNotEmpty();

    //The instrumented trace should have the tags
    assertThat(String.valueOf(tags.get((Tags.COMPONENT.getKey())))).isEqualTo("hystrix");
    assertThat(String.valueOf(tags.get(TracedHystrixCommand.TAG_COMMAND_GROUP)))
        .isEqualTo(groupKey);
    assertThat(String.valueOf(tags.get((TracedHystrixCommand.TAG_COMMAND_KEY))))
        .isEqualTo(commandKey);
    assertThat(String.valueOf(tags.get(TracedHystrixCommand.TAG_THREAD_POOL_KEY)))
        .isEqualTo(groupKey);
  }
}
