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
import org.springframework.stereotype.Service;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {HystrixTraceCommandTest.TestConfig.class})
@RunWith(SpringRunner.class)
public class HystrixTraceCommandTest {

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
        MockTracer tracer;

        @HystrixCommand
        public String sayHello() {
            tracer.buildSpan("sayHello").start().finish();
            return "Hello!!!";
        }

        @HystrixCommand(fallbackMethod = "defaultGreeting")
        public String alwaysFail(String user) {
            tracer.buildSpan("alwaysFail").start().finish();
            throw new IllegalStateException("Thrown Purposefully");

        }

        public String defaultGreeting(String user) {
            tracer.buildSpan("defaultGreeting").withTag("fallback", "yes").start().finish();
            return "Hi(fallback)";
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

        try (ActiveSpan span = mockTracer.buildSpan("test_without_circuit_breaker")
                .startActive()) {
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
    public void test_with_circuit_breaker() {
        try (ActiveSpan span = mockTracer.buildSpan("test_with_circuit_breaker")
                .startActive()) {
            String response = greetingService.alwaysFail("foo");
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
        assertThat(hystrixSpan.tags()).containsValues("yes");
    }

    @Test
    public void test_hystrix_trace_command() throws Exception {
        String groupKey = "test_hystrix";
        String commandKey = "hystrix_trace_command";

        try (ActiveSpan activeSpan = mockTracer.buildSpan("test_with_circuit_breaker")
                .startActive()) {
            com.netflix.hystrix.HystrixCommand.Setter setter = com.netflix.hystrix.HystrixCommand.Setter
                    .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
            new HystrixTraceCommand<Void>(mockTracer, setter) {
                @Override
                public Void doRun() throws Exception {
                    mockTracer.buildSpan("doRun").start().finish();
                    System.out.println("Hello World!");
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
        assertThat(String.valueOf(tags.get("commandGroup"))).isEqualTo(groupKey);
        assertThat(String.valueOf(tags.get("commandKey"))).isEqualTo(commandKey);
        assertThat(String.valueOf(tags.get("threadPoolKey"))).isEqualTo(groupKey);
    }

}
