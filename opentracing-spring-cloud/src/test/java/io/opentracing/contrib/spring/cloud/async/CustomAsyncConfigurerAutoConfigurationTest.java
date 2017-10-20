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
package io.opentracing.contrib.spring.cloud.async;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

import io.opentracing.contrib.spring.cloud.async.instrument.TracedAsyncConfigurer;
import org.junit.Test;
import org.springframework.scheduling.annotation.AsyncConfigurer;

/**
 * @author kameshs
 */
public class CustomAsyncConfigurerAutoConfigurationTest {

  @Test
  public void should_return_bean_when_its_not_async_configurer() {
    CustomAsyncConfigurerAutoConfiguration configuration = new CustomAsyncConfigurerAutoConfiguration();
    Object bean = configuration.postProcessAfterInitialization(new Object(), "someBean");
    then(bean).isNotInstanceOf(TracedAsyncConfigurer.class);
  }

  @Test
  public void should_return_async_configurer_when_bean_instance_of_it() {
    CustomAsyncConfigurerAutoConfiguration configuration = new CustomAsyncConfigurerAutoConfiguration();
    Object bean = configuration
        .postProcessAfterInitialization(mock(AsyncConfigurer.class), "myAsync");
    then(bean).isInstanceOf(TracedAsyncConfigurer.class);
  }
}
