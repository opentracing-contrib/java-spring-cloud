package io.opentracing.contrib.spring.cloud.async;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.util.ThreadLocalActiveSpanSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@ComponentScan("io.opentracing.contrib.spring.cloud.async")
public class AsyncTestConfiguration {

    @Bean
    public Tracer tracer() {
        return new MockTracer(new ThreadLocalActiveSpanSource(), MockTracer.Propagator.TEXT_MAP);
    }

    @Bean
    HttpBinService httpBinService() {
        return new HttpBinService();
    }

    @Bean
    ThreadPoolTaskExecutor poolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
