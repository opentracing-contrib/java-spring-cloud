package io.opentracing.contrib.spring.cloud.jms;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.Callable;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {MockTracingConfiguration.class, JmsTestConfiguration.class, JmsTracingTest.MsgController.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsTracingTest {
  @LocalServerPort
  int port;

  @Autowired
  MockTracer tracer;

  @Autowired
  JmsTestConfiguration.MsgListener msgListener;

  @Autowired
  private RestTemplate restTemplate;

  @Before
  public void before() {
    tracer.reset();
  }

  @Test
  public void testListenerSpans() {
    ResponseEntity<String> responseEntity = restTemplate.getForEntity(MockTracingConfiguration.getUrl(port, "/hello"), String.class);

    await().until(() -> {
      List<MockSpan> mockSpans = tracer.finishedSpans();
      return (mockSpans.size() == 4);
    });

    Assert.assertNotNull(msgListener.getMessage());

    assertEquals(200, responseEntity.getStatusCode().value());
    List<MockSpan> spans = tracer.finishedSpans();
    assertEquals(4, spans.size());  // it propagated over to @JmsListener
  }

  @RestController
  public static class MsgController {
    @Autowired
    JmsTemplate jmsTemplate;

    @RequestMapping("/hello")
    public String hello() {
      String message = "Hello!";
      jmsTemplate.convertAndSend("fooQueue", message);
      return message;
    }
  }

}
