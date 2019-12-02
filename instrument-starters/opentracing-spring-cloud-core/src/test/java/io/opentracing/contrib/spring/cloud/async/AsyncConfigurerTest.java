/**
 * Copyright 2017-2019 The OpenTracing Authors
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
package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentracing.Tracer;
import io.opentracing.contrib.concurrent.TracedExecutor;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.async.instrument.TracedAsyncConfigurer;
import java.util.concurrent.Executor;
import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author wuyupeng
 * @author Tadaya Tsuyukubo
 **/
@SpringBootTest(classes = {AsyncConfigurerTest.AsyncConfigurerConfig.class, MockTracingConfiguration.class, CustomAsyncConfigurerAutoConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncConfigurerTest {

  @Autowired
  private AsyncConfigurer asyncConfigurer;


  @Configuration
  @EnableAsync
  static class AsyncConfigurerConfig extends AsyncConfigurerSupport {
    @Override
    public Executor getAsyncExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.initialize();
      return executor;
    }
  }

  /**
   * Test if the AsyncConfigurer configured by developer can be replaced with TracedAsyncConfigurer by CustomAsyncConfigurerAutoConfiguration
   */
  @Test
  public void testIsTracedAsyncConfigurer() {
    BDDAssertions.then(asyncConfigurer).isInstanceOf(TracedAsyncConfigurer.class);

    assertThat(this.asyncConfigurer.getAsyncExecutor()).isInstanceOfSatisfying(TracedExecutor.class, tracedExecutor -> {
      Tracer tracer = (Tracer) ReflectionTestUtils.getField(tracedExecutor, "tracer");
      assertThat(tracer).isNotNull();
    });

  }
}
