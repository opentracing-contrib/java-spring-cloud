package io.opentracing.contrib.spring.cloud.async;

import static java.util.concurrent.TimeUnit.SECONDS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

/**
 * @author kameshs
 */
@SpringBootTest(classes = {MockTracingConfiguration.class,  AsyncAnnotationTest.Configuration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncAnnotationTest {

    @org.springframework.context.annotation.Configuration
    static class Configuration {
        @Bean
        public DelayAsyncService delayAsyncService() {
            return new DelayAsyncService();
        }
    }

    static class DelayAsyncService {
        @Autowired
        private MockTracer tracer;

        @Async
        public Future<String> fooFuture() {
            tracer.buildSpan("foo").start().finish();
            return new AsyncResult<>("whatever");
        }
    }

    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private DelayAsyncService delayAsyncService;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testAsyncTraceAndSpans() throws Exception {
        try (ActiveSpan span = mockTracer.buildSpan("bar")
                .startActive()) {
            Future<String> fut = delayAsyncService.fooFuture();
            await().until(() -> fut.isDone());
            assertThat(fut.get()).isNotNull();
        }
        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 3);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(3, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }
}
