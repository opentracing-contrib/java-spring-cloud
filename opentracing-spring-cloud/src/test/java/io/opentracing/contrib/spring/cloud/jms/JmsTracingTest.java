package io.opentracing.contrib.spring.cloud.jms;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import java.util.List;

import io.opentracing.ActiveSpan;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@SpringBootTest(
  classes = {MockTracingConfiguration.class, JmsTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsTracingTest {
  @Autowired
  MockTracer tracer;

  @Autowired
  JmsTemplate jmsTemplate;

  @Autowired
  JmsTestConfiguration.MsgListener msgListener;

  @Test
  public void testListenerSpans() {
    try (ActiveSpan span = tracer.buildSpan("test").startActive()) {
      jmsTemplate.convertAndSend("fooQueue", "Hello!");
      await().until(() -> tracer.finishedSpans().size() == 1);

      Assert.assertNotNull(msgListener.getMessage());

      List<MockSpan> mockSpans = tracer.finishedSpans();
      assertEquals(1, mockSpans.size()); // it propagated over to @JmsListener
    }
  }
}
