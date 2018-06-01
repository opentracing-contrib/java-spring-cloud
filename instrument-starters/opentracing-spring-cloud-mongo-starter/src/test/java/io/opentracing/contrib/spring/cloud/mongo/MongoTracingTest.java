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
package io.opentracing.contrib.spring.cloud.mongo;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.mongodb.MongoClient;
import io.opentracing.Scope;
import io.opentracing.Tracer;
import io.opentracing.contrib.mongo.TracingMongoClient;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Vivien Maleze
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.MOCK,
    classes = {MongoTracingTest.TestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class MongoTracingTest {

  @EnableAutoConfiguration
  public static class TestConfiguration {
    @Bean
    public Tracer tracer() {
      return new MockTracer();
    }
  }

  @Autowired
  MongoClient mongoClient;

  @Autowired
  MongoTemplate mongoTemplate;

  @Autowired
  MockTracer tracer;

  @Before
  public void before() {
    tracer.reset();
  }

  /**
   * Make sure we get a TracingMongoClient instead of a normal MongoClient
   */
  @Test
  public void mongoTracerIsInstrumented() {
    assertTrue(mongoClient instanceof TracingMongoClient);
  }

  /**
   * Make sure we get one span once we execute a database call.
   */
  @Test
  public void spanIsCreatedForCommandExecution() {
    this.mongoTemplate.executeCommand("{ buildInfo: 1 }");
    assertEquals(1, tracer.finishedSpans().size());
  }

  /**
   * Make sure that a span is created when an active span exists joins the active
   */
  @Test
  public void spanJoinsActiveSpan() {
    try (Scope ignored = tracer.buildSpan("parent").startActive(true)) {
      assertTrue(this.mongoTemplate.executeCommand("{ buildInfo: 1 }").ok());
      assertEquals(1, tracer.finishedSpans().size());
    }

    assertEquals(2, tracer.finishedSpans().size());
    Optional<MockSpan> mongoSpan = tracer
        .finishedSpans()
        .stream()
        .filter(s -> "java-mongo".equals(s.tags().get(Tags.COMPONENT.getKey())))
        .findFirst();

    Optional<MockSpan> parentSpan = tracer
        .finishedSpans()
        .stream()
        .filter(s -> "parent".equals(s.operationName()))
        .findFirst();

    assertTrue(mongoSpan.isPresent());
    assertTrue(parentSpan.isPresent());

    assertEquals(mongoSpan.get().context().traceId(), parentSpan.get().context().traceId());
    assertEquals(mongoSpan.get().parentId(), parentSpan.get().context().spanId());
  }


  /**
   * Sanity test for multiple requests, on their own threads, each executing a mongo command
   */
  @Test
  public void concurrentParents() {
    ExecutorService service = Executors.newFixedThreadPool(10);
    IntStream.rangeClosed(1, 150).parallel().forEach(i -> service.submit(() -> {
      // each iteration is like a request
      try (Scope parent = tracer.buildSpan("parent_" + i).startActive(true)) {
        parent.span().setTag("iteration", i);
        this.mongoTemplate.executeCommand("{ buildInfo : " + i + " }");
      }
    }));

    // for each request, we have one extra span, the jdbc one
    await().until(() -> tracer.finishedSpans().size() == 300);

    tracer.finishedSpans()
        .stream()
        .filter(s -> s.operationName().startsWith("parent_"))
        .forEach(parent -> {
          List<MockSpan> child = tracer.finishedSpans()
              .stream()
              .filter(c -> c.parentId() == parent.context().spanId())
              .collect(Collectors.toList());
          assertEquals(1, child.size());
          assertEquals("{ \"buildInfo\" : " + parent.tags().get("iteration") + " }",
              child.get(0).tags().get("db.statement"));
        });
  }
}
