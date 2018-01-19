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
package io.opentracing.contrib.spring.cloud.jdbc;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.opentracing.Scope;
import io.opentracing.contrib.jdbc.TracingConnection;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Juraci Paixão Kröhling
 */
@SpringBootTest(classes = {MockTracingConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcTracingTest {

  @Autowired
  MockTracer tracer;

  @Autowired
  DataSource dataSource;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Before
  public void before() {
    tracer.reset();
  }

  /**
   * Make sure we get a data source that returns a tracing connection. Without our Aspect, this
   * would return a regular pooled connection for an embedded database (as we don't configure a
   * database anywhere).
   */
  @Test
  public void dataSourceIsInstrumented() throws SQLException {
    assertTrue(dataSource.getConnection() instanceof TracingConnection);
  }

  /**
   * Make sure we get one span once we execute a database call.
   */
  @Test
  public void spanIsCreatedForPreparedStatement() throws SQLException {
    PreparedStatement pstmt = dataSource.getConnection().prepareStatement("select 1");
    assertTrue(pstmt.execute());
    assertEquals(1, tracer.finishedSpans().size());
  }

  /**
   * Make sure that a span is created when an active span exists joins the active
   */
  @Test
  public void spanJoinsActiveSpan() throws SQLException {
    try (Scope ignored = tracer.buildSpan("parent").startActive(true)) {
      assertTrue(dataSource.getConnection().prepareStatement("select 1").execute());
      assertEquals(1, tracer.finishedSpans().size());
    }

    assertEquals(2, tracer.finishedSpans().size());
    Optional<MockSpan> jdbcSpan = tracer
        .finishedSpans()
        .stream()
        .filter((s) -> "java-jdbc".equals(s.tags().get(Tags.COMPONENT.getKey())))
        .findFirst();

    Optional<MockSpan> parentSpan = tracer
        .finishedSpans()
        .stream()
        .filter((s) -> "parent".equals(s.operationName()))
        .findFirst();

    assertTrue(jdbcSpan.isPresent());
    assertTrue(parentSpan.isPresent());

    assertEquals(jdbcSpan.get().context().traceId(), parentSpan.get().context().traceId());
    assertEquals(jdbcSpan.get().parentId(), parentSpan.get().context().spanId());
  }

  /**
   * Make sure that a span is created when executing statements via a JDBC template
   */
  @Test
  public void spanIsCreatedWhenUsingJdbcTemplate() {
    jdbcTemplate.execute("select 1");
    assertEquals(1, tracer.finishedSpans().size());
  }

  /**
   * Sanity test for multiple requests, on their own threads, each executing a jdbc connection
   */
  @Test
  public void concurrentParents() {
    ExecutorService service = Executors.newFixedThreadPool(10);
    IntStream.rangeClosed(1, 150).parallel().forEach(i -> {
      service.submit(() -> {
        // each iteration is like a request
        try (Scope parent = tracer.buildSpan("parent_" + i).startActive(true)) {
          parent.span().setTag("iteration", i);
          jdbcTemplate.execute("select " + i);
        }
      });
    });

    // for each request, we have one extra span, the jdbc one
    await().until(() -> tracer.finishedSpans().size() == 300);

    tracer.finishedSpans()
        .stream()
        .filter((s) -> s.operationName().startsWith("parent_"))
        .forEach(parent -> {
          List<MockSpan> child = tracer.finishedSpans()
              .stream()
              .filter(c -> c.parentId() == parent.context().spanId())
              .collect(Collectors.toList());
          assertEquals(1, child.size());
          assertEquals("select " + parent.tags().get("iteration"),
              child.get(0).tags().get("db.statement"));
        });
  }
}
