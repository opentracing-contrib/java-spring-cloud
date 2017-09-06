package io.opentracing.contrib.spring.cloud.async;

import feign.Feign;
import io.opentracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;

@Configuration
@EnableAsync
@ComponentScan("io.opentracing.contrib.spring.cloud.async")
public class AsyncTestConfiguration {

    @Bean
    public Tracer tracer() {
        return mock(Tracer.class);
    }


    @Bean(name = "threadPoolTaskExecutor")
    public Executor threadPoolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }

    @Bean
    HttpBinServiceClient httpBinClient() {
        return Feign.builder()
                .target(HttpBinServiceClient.class, "http://httpbin.org");
    }

}
