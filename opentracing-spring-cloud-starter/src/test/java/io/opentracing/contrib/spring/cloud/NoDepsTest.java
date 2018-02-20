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
package io.opentracing.contrib.spring.cloud;

import org.junit.Test;

/**
 * @author Pavol Loffay
 */
public class NoDepsTest {

  @Test(expected = ClassNotFoundException.class)
  public void testNoFeign() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("feign.Client");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoHystrixFeign() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("feign.hystrix.HystrixFeign");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoJMS() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("javax.jms.Message");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoJMSTemplate() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("org.springframework.jms.core.JmsTemplate");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoZuulFilter() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("com.netflix.zuul.ZuulFilter");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoRxJavaHooks() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass("rx.plugins.RxJavaHooks");
  }

  @Test(expected = ClassNotFoundException.class)
  public void testNoWebsocketMessageBroker() throws ClassNotFoundException {
    this.getClass().getClassLoader().loadClass(
        "org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer");
  }
}
