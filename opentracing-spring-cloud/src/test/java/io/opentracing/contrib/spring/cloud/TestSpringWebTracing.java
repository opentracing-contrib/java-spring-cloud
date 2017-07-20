package io.opentracing.contrib.spring.cloud;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import io.opentracing.contrib.spring.cloud.TestSpringWebTracing.TestController;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {MockTracingConfiguration.class, TestController.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TestSpringWebTracing {

  @RestController
  public static class TestController {
    @RequestMapping("/hello")
    public String hello() {
      return "Hello";
    }

    @RequestMapping("/notTraced")
    public String notTraced() {
      return "Not traced";
    }
  }

  @LocalServerPort
  int port;

  @Autowired
  protected MockTracer mockTracer;

  @Autowired
  private TestRestTemplate testRestTemplate;

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private AsyncRestTemplate asyncRestTemplate;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testControllerTracing() {
    ResponseEntity<String> responseEntity = testRestTemplate.getForEntity("/hello", String.class);
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    assertEquals(200, responseEntity.getStatusCode().value());
    assertEquals(1, mockTracer.finishedSpans().size());
  }

  @Test
  public void testRestTemplateTracing() {
    restTemplate.getForEntity(getUrl("/notTraced"), String.class);
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    //only http client is traced
    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
  }

  @Test
  public void testAsyncRestTemplateTracing() throws ExecutionException, InterruptedException {
    asyncRestTemplate.getForEntity(getUrl("/notTraced"), String.class).get();
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
  }

  public String getUrl(String path) {
    return "http://localhost:" + port + path;
  }
}
