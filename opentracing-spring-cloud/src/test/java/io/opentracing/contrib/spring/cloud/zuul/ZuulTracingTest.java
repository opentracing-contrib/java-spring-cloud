package io.opentracing.contrib.spring.cloud.zuul;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestController;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {MockTracingConfiguration.class, ZuulTracingTest.ZuulConfiguration.class,
                TestController.class},
        properties = {"zuul.routes.test.url=http://localhost:8085/hello",
                "server.port=8085"})
@RunWith(SpringJUnit4ClassRunner.class)
public class ZuulTracingTest {

    @Autowired
    protected MockTracer mockTracer;

    @Autowired
    private RestTemplate restTemplate;

    @LocalServerPort
    int port;

    @Configuration
    @EnableZuulProxy
    static class ZuulConfiguration {
    }

    @Before
    public void before() {
        mockTracer.reset();
    }

    @Test
    public void testZuulTracing() {
        restTemplate.getForEntity(getUrl("test"), String.class);

        await().until(() -> mockTracer.finishedSpans().size() == 4);
        List<MockSpan> mockSpans = mockTracer.finishedSpans();
        assertEquals(4, mockSpans.size());
    }

    public String getUrl(String path) {
        return "http://localhost:" + port + path;
    }
}
