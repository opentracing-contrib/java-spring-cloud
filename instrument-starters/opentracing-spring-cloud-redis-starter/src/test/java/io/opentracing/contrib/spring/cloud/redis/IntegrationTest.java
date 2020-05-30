/**
 * Copyright 2017-2020 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.redis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import ai.grakn.redismock.RedisServer;
import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracerTestUtil;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Daniel del Castillo
 */
@SpringBootTest(classes = {IntegrationTest.IntegrationTestConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class IntegrationTest {

  @Configuration
  @EnableAutoConfiguration
  public static class IntegrationTestConfiguration {

    public @Bean MockTracer mockTracer() {
      GlobalTracerTestUtil.resetGlobalTracer();
      return new MockTracer();
    }

    public @Bean RedisConnectionFactory redisConnectionFactory() {
      RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
      config.setHostName(redis.getHost());
      config.setPort(redis.getBindPort());
      return new JedisConnectionFactory(config);
    }

  }

  private static RedisServer redis;

  @Autowired
  MockTracer tracer;

  @Autowired
  RedisTemplate redisTemplate;

  @BeforeClass
  public static void setup() throws Exception {
    redis = RedisServer.newRedisServer();
    redis.start();
  }

  @AfterClass
  public static void teardown() {
    redis.stop();
  }

  @Before
  public void init() {
    tracer.reset();
  }

  @Test
  public void commandCreatesNewSpan() {
    redisTemplate.opsForValue().set(100L, "Some value here");
    assertEquals(1, tracer.finishedSpans().size());
    assertEquals("SET", tracer.finishedSpans().get(0).operationName());
  }

  @Test
  public void spanJoinsActiveSpan() {
    MockSpan span = tracer.buildSpan("parent").start();
    try (Scope ignored = tracer.activateSpan(span)) {
      redisTemplate.opsForList().leftPushAll("test-list", 1, 2, 3);
      assertEquals(1, tracer.finishedSpans().size());
      assertEquals("LPUSH", tracer.finishedSpans().get(0).operationName());
    } finally {
      span.finish();
    }

    assertEquals(2, tracer.finishedSpans().size());
    Optional<MockSpan> redisSpan = tracer.finishedSpans().stream()
        .filter((s) -> "java-redis".equals(s.tags().get(Tags.COMPONENT.getKey()))).findFirst();

    Optional<MockSpan> parentSpan =
        tracer.finishedSpans().stream().filter((s) -> "parent".equals(s.operationName())).findFirst();

    assertTrue(redisSpan.isPresent());
    assertTrue(parentSpan.isPresent());

    assertEquals(redisSpan.get().context().traceId(), parentSpan.get().context().traceId());
    assertEquals(redisSpan.get().parentId(), parentSpan.get().context().spanId());
  }

  // Cluster operations can be tested once https://github.com/kstyrc/embedded-redis/issues/79 is fixed

}
