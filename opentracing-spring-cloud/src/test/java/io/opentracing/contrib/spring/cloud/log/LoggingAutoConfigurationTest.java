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
import org.springframework.http.ResponseEntity;
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

  @RestController
  @SpringBootApplication
  public static class Controller {
    private static final Log commonsLog = LogFactory.getLog(Controller.class);
    private static final Logger slf4jLog = LoggerFactory.getLogger(Controller.class);
    private static final java.util.logging.Logger julLog = java.util.logging.Logger.getLogger(Controller.class.getName());
    private static final org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger(Controller.class);

    private ContextData contextData() {
      ContextData contextData = new ContextData();
      contextData.setTimestamp(System.currentTimeMillis() * 1000);
      contextData.setThread(Thread.currentThread().getName());
      return contextData;
    }

    @RequestMapping("/jcl")
    public ContextData jcl() {
      commonsLog.info(LOG);
      return contextData();
    }
    @RequestMapping("/slf4j")
    public ContextData slf4j() {
      slf4jLog.info(LOG);
      return contextData();
    }
    @RequestMapping("/jul")
    public ContextData jul() {
      julLog.info(LOG);
      return contextData();
    }
    @RequestMapping("/log4j")
    public ContextData log4j() {
      log4j.info(LOG);
      return contextData();
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
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/jcl", ContextData.class);
    assertLogging(response.getBody());
  }

  @Test
  public void testSlf4j() {
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/slf4j", ContextData.class);
    assertLogging(response.getBody());
  }

  @Test
  public void testJul() {
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/jul", ContextData.class);
    assertLogging(response.getBody());
  }

  @Test
  public void testLog4j() {
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/log4j", ContextData.class);
    assertLogging(response.getBody());
  }

  private void assertLogging(ContextData contextData) {
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
    assertEquals(contextData.getThread(), logEntry.fields().get("thread"));
    assertEquals(Level.INFO.toString(), logEntry.fields().get("level"));
    // now <= timestamp < now + 10ms
    assertTrue(logEntry.timestampMicros() >= contextData.getTimestamp() && logEntry.timestampMicros() < contextData.getTimestamp() + 10 * 1000);
  }

  private static class ContextData {
    long timestamp;
    String thread;

    public long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }

    public String getThread() {
      return thread;
    }

    public void setThread(String thread) {
      this.thread = thread;
    }
  }
}
