package io.opentracing.contrib.spring.cloud.feign;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = {FeignTest.FeignWithoutRibbonConfiguration.class})
@TestPropertySource(properties = {"server.port=13598", "endpoints.health.enabled=true"})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignTest {

    @FeignClient(value = "localService", url = "localhost:13598")
    interface FeignInterface {
        @RequestMapping(method = RequestMethod.GET, value = "/health")
        String hello();
    }

    @RestController
    @Configuration
    @EnableFeignClients
    @EnableAutoConfiguration
    static class FeignWithoutRibbonConfiguration {
        @Bean
        public Tracer tracer() {
            return Mockito.spy(new MockTracer(new ThreadLocalActiveSpanSource(), MockTracer.Propagator.TEXT_MAP));
        }

        @RequestMapping("/health")
        public String hello() {
            return "Hello";
        }
    }

    @Autowired
    private FeignInterface feignInterface;

    @Autowired
    private Tracer tracer;

    @Test
    public void testTraced() {
        feignInterface.hello();
        verify(tracer, times(1)).buildSpan(Mockito.anyString());
    }
}
