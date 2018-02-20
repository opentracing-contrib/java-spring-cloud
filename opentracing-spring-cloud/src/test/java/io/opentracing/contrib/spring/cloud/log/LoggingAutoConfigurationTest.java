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
import io.opentracing.tag.Tags;
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
  private static final Exception EXCEPTION = new RuntimeException("exception message");

  @RestController
  @SpringBootApplication
  public static class Controller {
    private static final Log commonsLog = LogFactory.getLog(Controller.class);
    private static final Logger slf4jLog = LoggerFactory.getLogger(Controller.class);
    private static final java.util.logging.Logger julLog = java.util.logging.Logger.getLogger(Controller.class.getName());
    private static final org.apache.log4j.Logger log4j = org.apache.log4j.Logger.getLogger(Controller.class);

    private ContextData contextData(Throwable throwable, boolean errorLog, String level) {
      ContextData contextData = new ContextData();
      contextData.setTimestamp(System.currentTimeMillis() * 1000);
      contextData.setThread(Thread.currentThread().getName());
      contextData.setThrowable(throwable);
      contextData.setError(errorLog);
      contextData.setLevel(level);
      return contextData;
    }

    @RequestMapping("/jcl")
    public ContextData jcl() {
      commonsLog.info(LOG, EXCEPTION);
      return contextData(EXCEPTION, false, Level.INFO.toString());
    }
    @RequestMapping("/slf4j")
    public ContextData slf4j() {
      slf4jLog.info(LOG, EXCEPTION);
      return contextData(EXCEPTION, false, Level.INFO.toString());
    }
    @RequestMapping("/jul")
    public ContextData jul() {
      julLog.log(java.util.logging.Level.INFO, LOG, EXCEPTION);
      return contextData(EXCEPTION, false, Level.INFO.toString());
    }
    @RequestMapping("/log4j")
    public ContextData log4j() {
      log4j.info(LOG, EXCEPTION);
      return contextData(EXCEPTION, false, Level.INFO.toString());
    }
    @RequestMapping("/no-exception")
    public ContextData noException() {
      log4j.info(LOG);
      return contextData(null, false, Level.INFO.toString());
    }

    @RequestMapping("/log-error")
    public ContextData logError() {
      log4j.error(LOG);
      return contextData(null, true, Level.ERROR.toString());
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

  @Test
  public void testNoException() {
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/no-exception", ContextData.class);
    assertLogging(response.getBody());
  }

  @Test
  public void testError() {
    ResponseEntity<ContextData> response = restTemplate.getForEntity("/log-error", ContextData.class);
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

    if (contextData.getThrowable() == null && !contextData.isError()) {
      assertEquals(4, logEntry.fields().size());
    } else if (contextData.getThrowable() != null && contextData.isError()) {
      assertEquals(6, logEntry.fields().size());
    } else {
      assertEquals(5, logEntry.fields().size());
    }


    assertEquals(Controller.class.getName(), logEntry.fields().get("logger"));
    assertEquals(LOG, logEntry.fields().get("message"));
    assertEquals(contextData.getThread(), logEntry.fields().get("thread"));
    assertEquals(contextData.getLevel(), logEntry.fields().get("level"));
    if (contextData.getThrowable() != null) {
      assertEquals(contextData.getThrowable().getMessage(), ((Throwable)logEntry.fields().get("error.object")).getMessage());
    }
    if (contextData.isError()) {
      assertEquals(Tags.ERROR, logEntry.fields().get("event"));
    }
    // now >= timestamp +Nms > now
    assertTrue(contextData.getTimestamp() >= logEntry.timestampMicros()  && logEntry.timestampMicros() + 100 * 1000 > contextData.getTimestamp());
  }

  private static class ContextData {
    private long timestamp;
    private String level;
    private String thread;
    private Throwable throwable;
    private boolean error;

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

    public Throwable getThrowable() {
      return throwable;
    }

    public void setThrowable(Throwable throwable) {
      this.throwable = throwable;
    }

    public boolean isError() {
      return error;
    }

    public void setError(boolean error) {
      this.error = error;
    }

    public String getLevel() {
      return level;
    }

    public void setLevel(String level) {
      this.level = level;
    }
  }
}
