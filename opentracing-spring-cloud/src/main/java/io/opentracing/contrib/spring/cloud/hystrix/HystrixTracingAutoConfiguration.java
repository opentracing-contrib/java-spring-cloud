package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.HystrixCommand;
import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * that registers a custom OpenTracing {@link com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy}.
 *
 * @author Marcin Grzejszczak
 * @author kameshsampath - Modifications from original to suit OpenTracing
 * @see HystrixTracingConcurrencyStrategy
 */
@Configuration
@AutoConfigureAfter({TracerAutoConfiguration.class})
@ConditionalOnClass(HystrixCommand.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(value = "opentracing.spring.cloud.hystrix.strategy.enabled", matchIfMissing = true)
public class HystrixTracingAutoConfiguration {

    @Bean
    HystrixTracingConcurrencyStrategy hystrixTracingConcurrencyStrategy(Tracer tracer) {
        return new HystrixTracingConcurrencyStrategy(tracer);
    }


}
