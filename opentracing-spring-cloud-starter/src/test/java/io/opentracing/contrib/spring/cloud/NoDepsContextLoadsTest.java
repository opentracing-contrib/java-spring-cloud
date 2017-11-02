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
package io.opentracing.contrib.spring.cloud;

import static org.mockito.Mockito.mock;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.cloud.NoDepsContextLoadsTest.ContextConfiguration;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(classes = ContextConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class NoDepsContextLoadsTest {

  @EnableAutoConfiguration
  @Configuration
  static class ContextConfiguration {
    @Bean
    public Tracer tracer() {
      return mock(Tracer.class);
    }
  }

  @Test
  public void testContextLoads() {
  }
}
