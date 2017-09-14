package io.opentracing.contrib.spring.cloud.hystrix;

import com.netflix.hystrix.HystrixCommand;
import feign.opentracing.hystrix.TracingConcurrencyStrategy;
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
 * that registers a tracer with OpenTracing {@link TracingConcurrencyStrategy}.
 *
 * @author kameshsampath
 */
@Configuration
@AutoConfigureAfter({TracerAutoConfiguration.class})
@ConditionalOnClass(HystrixCommand.class)
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(value = "opentracing.spring.cloud.hystrix.strategy.enabled", matchIfMissing = true)
public class HystrixTracingAutoConfiguration {

    @Bean
    TracingConcurrencyStrategy hystrixTracingConcurrencyStrategy(Tracer tracer) {
        return TracingConcurrencyStrategy.register(tracer);
    }


}
