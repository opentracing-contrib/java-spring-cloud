package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;


/**
 * @author kameshs
 */
@ContextConfiguration(classes = {AsyncTestConfiguration.class, MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncTest {

    @Autowired
    protected MockTracer mockTracer;

    @Autowired
    HttpBinService httpBinService;

    @Before
    public void callExtService() throws Exception {
        assertThat(httpBinService).isNotNull();
        Future<String> fut = httpBinService.delayer(3);
        await().atMost(10, SECONDS).until(() -> fut.isDone());
        String response = fut.get();
        assertThat(response).isNotNull();
    }

    @Test
    public void testAsyncTraceAndSpans() throws Exception {

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 1);
        List<MockSpan> finishedSpans = mockTracer.finishedSpans();
        assertThat(finishedSpans.size()).isEqualTo(1);
        MockSpan mockSpan = mockTracer.finishedSpans().get(0);
        assertThat(mockSpan).isNotNull();
        assertThat(mockSpan.tags()).isNotEmpty();
        assertThat(mockSpan.tags().get("myTag")).isEqualTo("hello");
    }

}
