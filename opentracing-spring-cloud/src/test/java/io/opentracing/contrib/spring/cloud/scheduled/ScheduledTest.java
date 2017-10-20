/**
 * Copyright 2017 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.scheduled;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.ExtensionTags;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestUtils;
import io.opentracing.contrib.spring.cloud.scheduled.ScheduledTest.Configuration;
import io.opentracing.contrib.spring.cloud.scheduled.ScheduledTest.ScheduledComponent;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {
        MockTracingConfiguration.class,
        Configuration.class,
        ScheduledComponent.class
    })
@RunWith(SpringJUnit4ClassRunner.class)
public class ScheduledTest {

  @EnableScheduling
  static class Configuration {

  }

  @Component
  @EnableScheduling
  static class ScheduledComponent {

    @Autowired
    private Tracer tracer;
    @Autowired
    private ScheduledAnnotationBeanPostProcessor scheduledAnnotationBeanPostProcessor;
    @Autowired
    private BeanFactory beanFactory;

    @Scheduled(fixedDelay = 1)
    public void scheduledFoo() {
      // disable upcoming scheduling
      scheduledAnnotationBeanPostProcessor
          .postProcessBeforeDestruction(beanFactory.getBean(ScheduledComponent.class), null);
      tracer.buildSpan("child").startManual().finish();
    }
  }

  @Autowired
  private MockTracer tracer;

  @After
  public void after() {
    tracer.reset();
  }

  @Test
  public void testScheduled() throws InterruptedException {
    await().until(() -> tracer.finishedSpans().size() == 2);
    // 1. span created for @Scheduled
    // 2. span created inside scheduled method
    List<MockSpan> mockSpans = tracer.finishedSpans();
    assertEquals(2, mockSpans.size());
    TestUtils.assertSameTraceId(mockSpans);

    MockSpan localSpan = mockSpans.get(0);
    assertEquals("child", localSpan.operationName());

    MockSpan scheduledSpan = mockSpans.get(1);
    assertEquals(scheduledSpan.context().spanId(), localSpan.parentId());
    assertEquals("scheduledFoo", scheduledSpan.operationName());
    assertEquals(3, scheduledSpan.tags().size());
    assertEquals(0, scheduledSpan.logEntries().size());
    assertEquals(ScheduledAspect.COMPONENT_NAME, scheduledSpan.tags().get(Tags.COMPONENT.getKey()));
    assertEquals(ScheduledComponent.class.getSimpleName(),
        scheduledSpan.tags().get(ExtensionTags.CLASS_TAG.getKey()));
    assertEquals("scheduledFoo", scheduledSpan.tags().get(ExtensionTags.METHOD_TAG.getKey()));
  }
}
