package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(classes = {MockTracingConfiguration.class, WebAsyncTaskTest.HelloWorldController.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
public class WebAsyncTaskTest {

    @RestController
    static class HelloWorldController {
        @Autowired
        MockTracer mockTracer;

        @RequestMapping("/webAsyncTask")
        public WebAsyncTask<String> webAsyncTask() {
            return new WebAsyncTask<>(() -> {
                mockTracer.buildSpan("foo").startManual().finish();
                return "Hello!";
            });
        }

        @RequestMapping("/callable")
        public Callable<String> callable() {
            return () -> {
                mockTracer.buildSpan("foo").startManual().finish();
                return "hola!";
            };
        }
    }

    @Autowired
    private MockTracer mockTracer;
    @Autowired
    private TestRestTemplate restTemplate;

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testWebAsyncTaskTraceAndSpans() throws Exception {
        String response = restTemplate.getForObject("/webAsyncTask",String.class);
        assertThat(response).isNotNull();
        await().until(() -> mockTracer.finishedSpans().size() == 2);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(2, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }

    @Test
    public void testCallableTraceAndSpans() throws Exception {
        String response = restTemplate.getForObject("/callable", String.class);
        assertThat(response).isNotNull();
        await().until(() -> mockTracer.finishedSpans().size() == 2);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(2, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }
}
