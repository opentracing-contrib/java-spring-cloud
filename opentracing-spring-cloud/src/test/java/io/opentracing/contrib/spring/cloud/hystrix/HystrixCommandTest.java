package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import io.opentracing.Span;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@SpringBootTest(classes = {MockTracingConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RunWith(SpringJUnit4ClassRunner.class)
public class HystrixCommandTest {

    @RestController
    static class HelloWorldController2 {
        @RequestMapping("/hello2")
        public String hystrixCall() {
            return "Hello2";
        }

    }

    @RestController
    static class HelloWorldController {
        @Autowired
        MockTracer mockTracer;

        @Autowired
        private TestRestTemplate restTemplate;

        @RequestMapping("/hello")
        @HystrixCommand
        public void hystrixCall() {
            Span span = mockTracer.buildSpan("hystrixCall")
                    .startManual();
            try {
                restTemplate.getForObject("/hello2", String.class);
            } finally {
                span.finish();
            }

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
    public void tesHystrixCommandSpans() throws Exception {
        String response = restTemplate.getForObject("/hello", String.class);
        assertThat(response).isNotNull();
        await().until(() -> mockTracer.finishedSpans().size() == 2);

        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(2, mockSpans.size());
        TestUtils.assertSameTraceId(mockSpans);
    }

}
