package io.opentracing.contrib.spring.cloud.zuul;

import com.netflix.zuul.ZuulFilter;
import io.opentracing.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@ConditionalOnBean(Tracer.class)
@ConditionalOnClass(ZuulFilter.class)
@ConditionalOnProperty(name = "opentracing.spring.cloud.zuul.enabled", havingValue = "true", matchIfMissing = true)
public class ZuulAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TracePreZuulFilter tracePreZuulFilter(Tracer tracer) {
        return new TracePreZuulFilter(tracer);
    }

    @Bean
    @ConditionalOnMissingBean
    public TracePostZuulFilter tracePostZuulFilter() {
        return new TracePostZuulFilter();
    }

}
