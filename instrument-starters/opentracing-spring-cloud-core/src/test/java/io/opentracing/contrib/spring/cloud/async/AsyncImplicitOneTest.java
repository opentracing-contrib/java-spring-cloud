/**
 * Copyright 2017-2021 The OpenTracing Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Prevent overriding a implicit AsyncTaskExecutor {@linkplain DefaultAsyncAutoConfiguration}
 *
 * @author Jerry Zhong
 */
@SpringBootTest(classes = {AsyncImplicitOneTest.Configuration.class, MockTracingConfiguration.class, DefaultAsyncAutoConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncImplicitOneTest {

  public static final String IMPLICIT_THREAD_GROUP = "implicit-thread-group";

  @Autowired(required = false)
  private AsyncConfigurer asyncConfigurer;
  @Autowired
  private AsyncService asyncService;

  @org.springframework.context.annotation.Configuration
  static class Configuration {

    @Bean
    public Executor threadPoolTaskExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setThreadGroupName(IMPLICIT_THREAD_GROUP);
      executor.initialize();
      return executor;
    }

    @Bean
    public AsyncService asyncService() {
      return new AsyncService();
    }
  }

  static class AsyncService {

    @Async
    public Future<String> asyncThreadGroupName() {
      return new AsyncResult<>(Thread.currentThread().getThreadGroup().getName());
    }
  }

  @Test
  public void testNoOverrideImplicitOne() throws ExecutionException, InterruptedException {
    assertNull(asyncConfigurer);
    Future<String> asyncFuture = asyncService.asyncThreadGroupName();
    assertEquals(IMPLICIT_THREAD_GROUP, asyncFuture.get());
  }
}
