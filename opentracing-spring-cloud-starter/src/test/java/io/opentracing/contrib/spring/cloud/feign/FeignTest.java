package io.opentracing.contrib.spring.cloud.feign;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.opentracing.Tracer;

/**
 * @author Pavol Loffay
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {FeignTest.FeignWithoutRibbonConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class FeignTest {

    @Configuration
    @EnableFeignClients
    @EnableAutoConfiguration
    static class FeignWithoutRibbonConfiguration {
        @Bean
        public Tracer tracer() {
            return mock(Tracer.class);
        }
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    public void testTracingFeignBeanCreated() {
        assertNotNull(applicationContext.getBean(TraceFeignContext.class));
    }
}
