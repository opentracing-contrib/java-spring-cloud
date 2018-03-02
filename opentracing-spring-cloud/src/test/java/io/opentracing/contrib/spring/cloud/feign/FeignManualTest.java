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

import static io.opentracing.contrib.spring.cloud.feign.FeignTest.verify;

import feign.Client;
import feign.Feign;
import feign.RequestLine;
import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestController;
import io.opentracing.contrib.spring.cloud.feign.FeignManualTest.ManualFeignConfiguration;
import io.opentracing.contrib.spring.cloud.feign.FeignTest.FeignRibbonLocalConfiguration;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class,
        ManualFeignConfiguration.class, FeignRibbonLocalConfiguration.class},
    properties = {"opentracing.spring.web.skipPattern=/notTraced"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignManualTest {

  @Configuration
  static class ManualFeignConfiguration {

    @Autowired
    public ManualFeignConfiguration(Client client) {
      feignInterface = Feign.builder().client(client)
          .target(FeignInterface.class, "http://localService");
    }
  }

  @FeignClient(value = "localService")
  interface FeignInterface {

    @RequestLine("GET /notTraced")
    // TODO this has to be added when using spring Boot 1.4.1
    @RequestMapping(method = RequestMethod.GET, value = "/notTraced")
    String hello();
  }

  private static FeignInterface feignInterface;

  @Autowired
  private MockTracer mockTracer;

  @Test
  public void testTracedRequestDefinedUrl() throws InterruptedException {
    feignInterface.hello();
    verify(mockTracer);
  }
}
