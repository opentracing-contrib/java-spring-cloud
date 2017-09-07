package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


/**
 * @author kameshs
 */
@SpringBootTest(classes = {AsyncTestConfiguration.class, MockTracingConfiguration.class, HelloWorldController.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncTest {

    @Autowired
    protected MockTracer mockTracer;

    @Autowired
    DelayAsyncService delayAsyncService;

    @Autowired
    TestRestTemplate restTemplate;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testAsyncTraceAndSpans() throws Exception {
        ActiveSpan span = mockTracer
                .buildSpan("testAsyncTraceAndSpans")
                .startActive();

        assertThat(delayAsyncService).isNotNull();
        Future<String> fut = delayAsyncService.delayer(3);
        await().atMost(10, SECONDS).until(() -> fut.isDone());
        String response = fut.get();
        assertThat(response).isNotNull();

        span.close();

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 2);
        TestUtils.assertSameTraceId(mockTracer.finishedSpans());

    }

    @Test
    public void testWebAsyncTaskTraceAndSpans() throws Exception {

        String response = restTemplate.getForObject("/hello",String.class);

        assertThat(response).isNotNull();

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 2);

        TestUtils.assertSameTraceId(mockTracer.finishedSpans());
    }

    @Test
    public void testCallableTraceAndSpans() throws Exception {

        String response = restTemplate.getForObject("/hola",String.class);

        assertThat(response).isNotNull();

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 2);

        TestUtils.assertSameTraceId(mockTracer.finishedSpans());
    }


}
