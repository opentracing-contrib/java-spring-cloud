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

import static io.opentracing.contrib.spring.cloud.feign.TestUtils.verifyWithSpanDecorators;

import io.opentracing.contrib.spring.cloud.feign.FeignManualTest.ManualFeignConfiguration;
import io.opentracing.contrib.spring.cloud.feign.FeignTest.FeignRibbonLocalConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Emerson Oliveira
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT,
    classes = {MockTracingConfiguration.class, TestController.class,
        ManualFeignConfiguration.class, FeignRibbonLocalConfiguration.class,
        FeignSpanDecoratorConfiguration.class},
    properties = {"opentracing.spring.web.skipPattern=/notTraced"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignManualWithSpanDecoratorsTest extends FeignManualTest {

  @Test
  public void testTracedRequestDefinedUrl() throws InterruptedException {
    feignInterface.hello();
    verifyWithSpanDecorators(mockTracer);
  }
}