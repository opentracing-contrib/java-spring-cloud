package io.opentracing.contrib.spring.cloud.feign;

import static io.opentracing.contrib.spring.cloud.feign.FeignTest.verify;

import io.opentracing.contrib.spring.cloud.MockTracingConfiguration;
import io.opentracing.contrib.spring.cloud.TestSpringWebTracing.TestController;
import io.opentracing.contrib.spring.cloud.feign.FeignDefinedUrlTest.FeignWithoutRibbonConfiguration;
import io.opentracing.mock.MockTracer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
    webEnvironment = WebEnvironment.DEFINED_PORT,
    classes = {MockTracingConfiguration.class, TestController.class,
        FeignWithoutRibbonConfiguration.class})
@TestPropertySource(properties = {"server.port=13598"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignDefinedUrlTest {

  @Configuration
  @EnableFeignClients
  static class FeignWithoutRibbonConfiguration {
  }

  @FeignClient(value = "localService", url = "localhost:13598")
  interface FeignInterface {
    @RequestMapping(method = RequestMethod.GET, value = "/hello")
    String hello();
  }

  @Autowired
  MockTracer mockTracer;

  @Autowired
  protected FeignInterface feignInterface;

  @Test
  public void testTracedRequestDefinedUrl() throws InterruptedException {
    feignInterface.hello();
    verify(mockTracer);
  }
}
