package io.opentracing.contrib.spring.cloud.async;

import feign.Feign;
import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Callable;
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
    HttpBinService httpBinService;

    @LocalServerPort
    int localServerPort;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    AsyncRestTemplate asyncRestTemplate;


    @Test
    public void testAsyncTraceAndSpans() throws Exception {
        ActiveSpan span = mockTracer
                .buildSpan("testAsyncTraceAndSpans")
                .startActive();

        assertThat(httpBinService).isNotNull();
        Future<String> fut = httpBinService.delayer(3);
        await().atMost(10, SECONDS).until(() -> fut.isDone());
        String response = fut.get();
        assertThat(response).isNotNull();

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 1);
        List<MockSpan> finishedSpans = mockTracer.finishedSpans();
        assertThat(finishedSpans.size()).isEqualTo(1);
        MockSpan mockSpan = mockTracer.finishedSpans().get(0);
        assertThat(mockSpan).isNotNull();
        assertThat(mockSpan.tags()).isNotEmpty();
        assertThat(mockSpan.tags().get("myTag")).isEqualTo("hello");

        span.close();
    }

    @Test
    public void testWebAsyncTaskTraceAndSpans() throws Exception {

        ActiveSpan span = mockTracer
                .buildSpan("testWebAsyncTaskTraceAndSpans")
                .startActive();

        String response = restTemplate.getForObject("http://localhost:" + localServerPort+"/myip",String.class);

        assertThat(response).isNotNull();

        await().atMost(10, SECONDS).until(() -> mockTracer.finishedSpans().size() == 1);
        List<MockSpan> finishedSpans = mockTracer.finishedSpans();

        finishedSpans.forEach(mockSpan -> {
            mockSpan.tags().forEach((s, o) -> System.out.println(s+o));
        });

        span.close();
    }

    private RequestEntity requestEntity() {

        return null;
    }

    @Before
    public void before() {
        mockTracer.reset();
    }

}
