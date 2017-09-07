package io.opentracing.contrib.spring.cloud.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncTestConfiguration {
    @Bean
    DelayAsyncService delayAsyncService() {
        return new DelayAsyncService();
    }

    @Bean
    ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        return new ThreadPoolTaskExecutor();
    }
}
