/**
 * Copyright 2017-2018 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.log;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockSpan.LogEntry;
import io.opentracing.mock.MockTracer;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, LoggingAutoConfigurationTest.Controller.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class LoggingAutoConfigurationTest {
  private static final String LOG = "log message";
  private static String threadName;
  private static long timestampMicros;

  @RestController
  @SpringBootApplication
  public static class Controller {
    private static final Log commonsLog = LogFactory.getLog(Controller.class);
    private static final Logger slf4jLog = LoggerFactory.getLogger(Controller.class);
    private static final java.util.logging.Logger julLog = java.util.logging.Logger.getLogger(Controller.class.getName());
    private static final org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger(Controller.class);

    @RequestMapping("/jcl")
    public void jcl() {
      threadName = Thread.currentThread().getName();
      timestampMicros = System.currentTimeMillis() * 1000;
      commonsLog.info(LOG);
    }
    @RequestMapping("/slf4j")
    public void slf4j() {
      threadName = Thread.currentThread().getName();
      timestampMicros = System.currentTimeMillis() * 1000;
      slf4jLog.info(LOG);
    }
    @RequestMapping("/jul")
    public void jul() {
      threadName = Thread.currentThread().getName();
      timestampMicros = System.currentTimeMillis() * 1000;
      julLog.info(LOG);
    }
    @RequestMapping("/log4j")
    public void log4j() {
      threadName = Thread.currentThread().getName();
      timestampMicros = System.currentTimeMillis() * 1000;
      log4j.info(LOG);
    }
  }

  @Autowired
  private TestRestTemplate restTemplate;
  @Autowired
  private MockTracer mockTracer;

  @After
  public void after() {
    mockTracer.reset();
  }

  @Test
  public void testJcl() {
    restTemplate.getForEntity("/jcl", Void.class);
    assertLogging();
  }

  @Test
  public void testSlf4j() {
    restTemplate.getForEntity("/slf4j", Void.class);
    assertLogging();
  }

  @Test
  public void testJul() {
    restTemplate.getForEntity("/jul", Void.class);
    assertLogging();
  }

  @Test
  public void testLog4j() {
    restTemplate.getForEntity("/log4j", Void.class);
    assertLogging();
  }

  private void assertLogging() {
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    MockSpan mockSpan = mockSpans.get(0);
    // preHandle log
    // standard log
    // afterCompletion log
    assertEquals(3, mockSpan.logEntries().size());
    LogEntry logEntry = mockSpan.logEntries().get(1);
    assertEquals(4, logEntry.fields().size());
    assertEquals(Controller.class.getName(), logEntry.fields().get("logger"));
    assertEquals(LOG, logEntry.fields().get("message"));
    assertEquals(threadName, logEntry.fields().get("thread"));
    assertEquals(Level.INFO.toString(), logEntry.fields().get("level"));
    // now <= timestamp < now + 10ms
    assertTrue(logEntry.timestampMicros() >= timestampMicros && logEntry.timestampMicros() < timestampMicros + 10 * 1000);
  }
}
