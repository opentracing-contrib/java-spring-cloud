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
package io.opentracing.contrib.spring.cloud.feign;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;

import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.Server;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestController;
import io.opentracing.contrib.spring.cloud.feign.FeignTest.FeignRibbonLocalConfiguration;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.tag.Tags;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class,
        FeignRibbonLocalConfiguration.class},
    properties = {"opentracing.spring.web.skipPattern=/notTraced"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignTest {

  @Configuration
  @EnableFeignClients
  @RibbonClients(@RibbonClient(name = "localService", configuration = RibbonConfiguration.class))
  static class FeignRibbonLocalConfiguration {

  }

  @Configuration
  static class RibbonConfiguration {

    @LocalServerPort
    int port;

    @Bean
    public ILoadBalancer loadBalancer() {
      BaseLoadBalancer loadBalancer = new BaseLoadBalancer();
      loadBalancer.setServersList(Collections.singletonList(new Server("localhost", port)));
      return loadBalancer;
    }
  }

  @FeignClient(value = "localService")
  interface FeignInterface {

    @RequestMapping(method = RequestMethod.GET, value = "/notTraced")
    String hello();
  }

  @Autowired
  protected MockTracer mockTracer;

  @Autowired
  protected FeignInterface feignInterface;

  @Before
  public void before() {
    mockTracer.reset();
  }

  @Test
  public void testTracedRequest() throws InterruptedException {
    feignInterface.hello();
    verify(mockTracer);
  }

  static void verify(MockTracer mockTracer) {
    await().until(() -> mockTracer.finishedSpans().size() == 1);
    List<MockSpan> mockSpans = mockTracer.finishedSpans();
    assertEquals(1, mockSpans.size());
    assertEquals(Tags.SPAN_KIND_CLIENT, mockSpans.get(0).tags().get(Tags.SPAN_KIND.getKey()));
  }
}
