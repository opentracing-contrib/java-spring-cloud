package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.strategy.HystrixPlugins;
import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {HystrixTraceCommandTest.TestConfig.class})
@RunWith(SpringRunner.class)
@DirtiesContext
public class HystrixTraceCommandTest {

    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableHystrix
    @Configuration
    //This is required for circuit breaker tests to work correctly
    @EnableAutoConfiguration(exclude = {LoadBalancerAutoConfiguration.class, JmxAutoConfiguration.class})
    static class TestConfig {
        @Bean
        public MockTracer mockTracer() {
            return new MockTracer(new ThreadLocalActiveSpanSource(), MockTracer.Propagator.TEXT_MAP);
        }

        @Bean
        public GreetingService greetingService() {
            return new GreetingService();
        }
    }

    @Service
    static class GreetingService {

        @Autowired
        MockTracer mockTracer;

        @HystrixCommand
        public String sayHello() {
            Span span = mockTracer.buildSpan("sayHello")
                    .startManual();

            span.finish();
            return "Hello!!!";
        }

        @HystrixCommand(fallbackMethod = "defaultGreeting")
        public String sayHelloToUser(String user) {
            Span span = mockTracer.buildSpan("sayHelloToUser")
                    .startManual();
            try {
                throw new IllegalStateException("Thrown Purposefully");
            } finally {
                span.finish();
            }
        }

        public String defaultGreeting(String user) {
            Span span = mockTracer.buildSpan("sayHelloToUser")
                    .withTag("myTag", "fallback")
                    .startManual();
            try {
                return "Hi(fallback)";
            } finally {
                span.finish();
            }
        }
    }

    @Autowired
    private MockTracer mockTracer;

    @Autowired
    private GreetingService greetingService;

    @BeforeClass
    @AfterClass
    public static void reset() {
        HystrixPlugins.reset();
    }

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void test_without_circuit_breaker() throws Exception {

        try (ActiveSpan activeSpan = mockTracer.buildSpan("test_without_circuit_breaker")
                .startActive()) {
            String response = greetingService.sayHello();
            assertThat(response).isNotNull();
            await().atMost(5, TimeUnit.SECONDS).until(() -> mockTracer.finishedSpans().size() == 1);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            TestUtils.assertSameTraceId(mockSpans);
        }
    }

    @Test
    public void test_with_circuit_breaker() {
        try (ActiveSpan activeSpan = mockTracer.buildSpan("test_with_circuit_breaker")
                .startActive()) {
            String response = greetingService.sayHelloToUser("tomandjerry");
            assertThat(response).isNotNull();
            await().until(() -> mockTracer.finishedSpans().size() == 2);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(2, mockSpans.size());
            TestUtils.assertSameTraceId(mockSpans);

            assertThat(mockSpans.get(1).tags()).containsValues("fallback");
        }
    }

    @Test
    public void test_hystrix_trace_command() throws Exception {
        try (ActiveSpan activeSpan = mockTracer.buildSpan("test_with_circuit_breaker")
                .startActive()) {
            String groupKey = "test_hystrix";
            String commandKey = "hystrix_trace_command";
            com.netflix.hystrix.HystrixCommand.Setter setter = com.netflix.hystrix.HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
            new HystrixTraceCommand<Void>(mockTracer, setter) {
                @Override
                public Void doRun() throws Exception {
                    System.out.println("Hello World!");
                    return null;
                }
            }.execute();

            await().until(() -> mockTracer.finishedSpans().size() == 1);

            List<MockSpan> mockSpans = mockTracer.finishedSpans();
            assertEquals(1, mockSpans.size());
            TestUtils.assertSameTraceId(mockSpans);

            Map tags = mockSpans.get(0).tags();
            assertThat(tags).isNotEmpty();

            assertThat(String.valueOf(tags.get((Tags.COMPONENT.getKey())))).isEqualTo("hystrix");
            assertThat(String.valueOf(tags.get("commandGroup"))).isEqualTo(groupKey);
            assertThat(String.valueOf(tags.get("commandKey"))).isEqualTo(commandKey);
            assertThat(String.valueOf(tags.get("threadPoolKey"))).isEqualTo(groupKey);

        }
    }

}
