package io.opentracing.contrib.spring.cloud.hystrix;

import io.opentracing.Tracer;
import io.opentracing.contrib.spring.web.autoconfig.ServerTracingAutoConfiguration;
import io.opentracing.contrib.spring.web.autoconfig.TracerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.netflix.hystrix.HystrixCommand;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration Auto-configuration}
 * that registers a custom Sleuth {@link com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy}.
 *
 * @author Marcin Grzejszczak
 * @author kameshsampath - Modifications from original to suit OpenTracing
 * @see SleuthHystrixConcurrencyStrategy
 *
 *
 */
@Configuration
@AutoConfigureAfter({ServerTracingAutoConfiguration.class, TracerAutoConfiguration.class})
@ConditionalOnClass(HystrixCommand.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(value = "opentracing.spring.cloud.hystrix.strategy.enabled", matchIfMissing = true)
public class SleuthHystrixAutoConfiguration {

    @Bean
    SleuthHystrixConcurrencyStrategy sleuthHystrixConcurrencyStrategy(Tracer tracer) {
        return new SleuthHystrixConcurrencyStrategy(tracer);
    }

}
