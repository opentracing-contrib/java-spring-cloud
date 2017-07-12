package io.opentracing.contrib.spring.cloud;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.opentracing.contrib.spring.cloud.TestSpringWebTracing.TestController;
import io.opentracing.mock.MockTracer;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

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
  }

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
    assertEquals(200, responseEntity.getStatusCode().value());
    assertEquals(1, mockTracer.finishedSpans().size());
  }

  @Test
  public void testRestTemplateTracing() {
    restTemplate.getForEntity("http://www.example.com", String.class);
    assertEquals(1, mockTracer.finishedSpans().size());
  }

  @Test
  public void testAsyncRestTemplateTracing() throws ExecutionException, InterruptedException {
    asyncRestTemplate.getForEntity("http://www.example.com", String.class).get();

    await().until(() -> mockTracer.finishedSpans().size() == 1);
    assertEquals(1, mockTracer.finishedSpans().size());
  }
}
