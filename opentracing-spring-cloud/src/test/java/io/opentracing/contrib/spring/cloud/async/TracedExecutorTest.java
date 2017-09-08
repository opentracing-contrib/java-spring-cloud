package io.opentracing.contrib.spring.cloud.async;

import static org.jgroups.util.Util.assertEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(classes = {MockTracingConfiguration.class, TracedExecutorTest.Configuration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TracedExecutorTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration {
        @Bean
        public Executor threadPoolTaskExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.initialize();
            return executor;
        }

        @Bean
        public Executor simpleAsyncTaskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }
    }

    @Autowired
    private MockTracer mockTracer;
    @Qualifier("threadPoolTaskExecutor")
    @Autowired
    private Executor threadPoolExecutor;
    @Qualifier("simpleAsyncTaskExecutor")
    @Autowired
    private Executor simpleAsyncExecutor;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testThreadPoolTracedExecutor() throws ExecutionException, InterruptedException {
        try(ActiveSpan activeSpan = mockTracer.buildSpan("foo").startActive()) {
            CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                mockTracer.buildSpan("child").start().finish();
                return "ok";
            }, threadPoolExecutor);
            completableFuture.get();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(2, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }

    @Test
    public void testSimpleTracedExecutor() throws ExecutionException, InterruptedException {
        try(ActiveSpan activeSpan = mockTracer.buildSpan("foo").startActive()) {
            CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
                mockTracer.buildSpan("child").start().finish();
                return "ok";
            }, simpleAsyncExecutor);
            completableFuture.get();
        }

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(2, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }
}
